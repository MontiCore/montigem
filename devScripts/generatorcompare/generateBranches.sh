#!/usr/bin/env bash
# (c) https://github.com/MontiCore/monticore
#
# Runs the generators of a project on two branches and stores each result under
#   <repo>/build/generate-compare/<branch>_<short commit id>/<project dir>/
# so the generated code can be compared (see diffNoIndex.sh).
# The Gradle task prepareGeneratorDiff runs the generators and writes the folders of the generated
# code to <project>/build/prepareGeneratorDiff/dirs.txt, so both branches need that task.
# If the output of the project already exists for the commit of a branch (from an earlier run, under
# any branch name), the script asks whether to reuse it and skip the generation for that branch.
#
# Usage: generateBranches.sh [-p <project dir>] [<branchA> [<branchB>]]
#   -p             folder of the Gradle project(s) to generate, default examples/vis-lib.
#                  The task runs in the project of that folder and in all projects below it, e.g.
#                    -p examples/vis-lib                  backend and frontend of vis-lib
#                    -p umlp-rte/frontend                 all libs of the RTE
#                    -p umlp-rte/frontend/networkgraph    one lib
#   no branch:     current branch vs. dev
#   one branch:    <branchA> vs. current branch

set -euo pipefail

DEFAULT_PROJECT_DIR='examples/vis-lib'
TASK_NAME='prepareGeneratorDiff'
DIRS_FILE='build/prepareGeneratorDiff/dirs.txt'
USAGE='Usage: generateBranches.sh [-p <project dir>] [<branchA> [<branchB>]]   (default project: examples/vis-lib, no branch: current branch vs. dev, one branch: <branchA> vs. current branch)'

# Prints the folders of the projects in or below the given folder that have a dirs file.
# Does not descend into build folders, the frontend ones contain node_modules.
projects_with_dirs_file() {
  local build_dir
  while IFS= read -r build_dir; do
    [ -f "${build_dir%/build}/$DIRS_FILE" ] && printf '%s\n' "${build_dir%/build}"
  done < <(find "$1" \( -name node_modules -o -name .git \) -prune -o -type d -name build -prune -print)
  return 0
}

# Prints the folders listed in the dirs file of the project, if there is one
generated_dirs() {
  local dirs_file="$1/$DIRS_FILE" dir
  [ -f "$dirs_file" ] || return 0
  while IFS= read -r dir || [ -n "$dir" ]; do
    dir="${dir%$'\r'}"
    [ -n "$dir" ] && printf '%s\n' "$dir"
  done < "$dirs_file"
}

# Removes the output of the previous run so nothing stale from the other branch survives
remove_generated() {
  local root="$1" project="$2" dir
  while IFS= read -r dir; do
    # the paths come from a file, never delete outside the repository
    if [[ "$dir" == "$root"/* ]]; then
      rm -rf "$dir"
    else
      echo "Not removing $dir, it is outside of $root" >&2
    fi
  done < <(generated_dirs "$project")
  rm -f "$project/$DIRS_FILE"
}

# Copies the generated folders of the project to the target, keeping their path below build/
copy_generated() {
  local project="$1" target="$2" dir rel
  while IFS= read -r dir; do
    [ -d "$dir" ] || continue
    rel="${dir#"$project/build/"}"
    [ "$rel" != "$dir" ] || rel="${dir##*/}"
    mkdir -p "$target/$(dirname "$rel")"
    cp -r "$dir" "$target/$rel"
  done < <(generated_dirs "$project")
}

# Prints an earlier result folder for the commit that contains the project, preferring the given target
existing_result() {
  local target="$1" short_id="$2" project_dir="$3" candidate
  for candidate in "$target" "${target%/*}"/*_"$short_id"; do
    if [ -d "$candidate${project_dir:+/$project_dir}" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 0
}

# Asks a yes/no question, an empty answer (or no terminal to ask on) means yes
ask_skip() {
  local answer
  while true; do
    read -r -p "$1 [Y/n] " answer < /dev/tty || return 0
    case "$answer" in
      ''|y|Y|yes|Yes) return 0 ;;
      n|N|no|No) return 1 ;;
    esac
  done
}

# Everything lives in main() so bash has read the whole script before any checkout happens
main() {
  local project_arg="$DEFAULT_PROJECT_DIR" project_given=false branch_args=()
  while [ $# -gt 0 ]; do
    case "$1" in
      -p|--project)
        if [ $# -lt 2 ]; then
          echo "$USAGE" >&2
          exit 2
        fi
        project_arg="$2"
        project_given=true
        shift 2
        ;;
      -h|--help)
        echo "$USAGE"
        exit 0
        ;;
      -*)
        echo "Unknown option $1" >&2
        echo "$USAGE" >&2
        exit 2
        ;;
      *)
        branch_args+=("$1")
        shift
        ;;
    esac
  done
  if [ ${#branch_args[@]} -gt 2 ]; then
    echo "$USAGE" >&2
    exit 2
  fi

  local script_dir root
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  root="$(git -C "$script_dir" rev-parse --show-toplevel)"

  # a given folder may be relative to the current directory (tab completion) or to the repository
  local project_dir
  if $project_given && [ -d "$project_arg" ]; then
    project_dir="$(git -C "$project_arg" rev-parse --show-prefix)"
  elif [ -d "$root/$project_arg" ]; then
    project_dir="$(git -C "$root/$project_arg" rev-parse --show-prefix)"
  else
    echo "Project folder $project_arg does not exist" >&2
    exit 2
  fi
  project_dir="${project_dir%/}"
  local project_root="$root${project_dir:+/$project_dir}"

  cd "$root"

  if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
    echo 'Working tree has uncommitted changes. Commit or stash them first.' >&2
    exit 1
  fi

  local gradle=gradle
  [ -x ./gradlew ] && gradle="$root/gradlew"

  local original
  original="$(git symbolic-ref --short -q HEAD || git rev-parse HEAD)"

  local branches
  case ${#branch_args[@]} in
    0) branches=("$original" dev) ;;
    1) branches=("${branch_args[0]}" "$original") ;;
    2) branches=("${branch_args[0]}" "${branch_args[1]}") ;;
  esac
  echo "Project:       ${project_dir:-.}"
  echo "First branch:  ${branches[0]}"
  echo "Second branch: ${branches[1]}"

  # all questions are asked before the first checkout, so the generation runs unattended afterwards
  local short_ids=() targets=() reuses=() i j branch short_id target existing reuse
  for i in "${!branches[@]}"; do
    branch="${branches[$i]}"
    if ! short_id="$(git rev-parse --verify -q --short "$branch^{commit}")"; then
      echo "Unknown branch $branch" >&2
      exit 1
    fi
    target="$root/build/generate-compare/${branch//[\/\\]/-}_$short_id"
    reuse=''

    local earlier=''
    for j in "${!short_ids[@]}"; do
      [ "${short_ids[$j]}" = "$short_id" ] && { earlier="$j"; break; }
    done
    if [ -n "$earlier" ]; then
      # both branches are at the same commit, the second run would generate the same output
      if ask_skip "$branch is at the same commit $short_id as ${branches[$earlier]}. Skip its generation and reuse that output?"; then
        reuse="${reuses[$earlier]:-${targets[$earlier]}}"
      fi
    else
      existing="$(existing_result "$target" "$short_id" "$project_dir")"
      if [ -n "$existing" ] && ask_skip "Output of ${project_dir:-.} for $branch (commit $short_id) already exists in $existing. Skip its generation?"; then
        reuse="$existing"
      fi
    fi
    short_ids+=("$short_id")
    targets+=("$target")
    reuses+=("$reuse")
  done

  trap "echo; echo 'Restoring $original'; git checkout '$original'" EXIT

  local results=() project projects
  for i in "${!branches[@]}"; do
    branch="${branches[$i]}"
    target="${targets[$i]}"
    printf '\n=== %s ===\n' "$branch"
    if [ -n "${reuses[$i]}" ]; then
      echo "Skipped, reusing ${reuses[$i]}"
      results+=("${reuses[$i]}")
      continue
    fi

    git checkout "$branch"

    if [ ! -d "$project_root" ]; then
      echo "Project folder ${project_dir:-.} does not exist on $branch" >&2
      exit 1
    fi

    while IFS= read -r project; do
      remove_generated "$root" "$project"
    done < <(projects_with_dirs_file "$project_root")

    # runs the task in the project of the folder and in all projects below it
    "$gradle" -p "$project_root" "$TASK_NAME"

    mapfile -t projects < <(projects_with_dirs_file "$project_root")
    if [ ${#projects[@]} -eq 0 ]; then
      echo "$TASK_NAME did not write any $DIRS_FILE below ${project_dir:-.}" >&2
      exit 1
    fi

    # only the part of this project is replaced, the results of other projects stay
    rm -rf "$target${project_dir:+/$project_dir}"
    for project in "${projects[@]}"; do
      copy_generated "$project" "$target/${project#"$root/"}"
    done
    results+=("$target")
  done

  printf '\nGenerated output:\n'
  printf '  %s\n' "${results[@]}"
  printf '\nCompare with:\n  %s/diffNoIndex.sh -n %s %s\n' "$script_dir" "${results[1]##*/}" "${results[0]##*/}"
}

main "$@"
