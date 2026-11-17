#!/usr/bin/env bash
# (c) https://github.com/MontiCore/monticore
#
# Alias for "git diff --no-index" without the LF/CRLF line ending warnings.
# Runs inside <repo>/build/generate-compare, so the folders written by generateBranches.sh
# can be given by name (tab completion: source diffNoIndex.completion.bash).
#
# Usage: diffNoIndex.sh [-n] [<git diff options>] <dirA> <dirB> [[--] <pathspec>...]
#   -n   only list the names of changed files (--name-only)
# Everything else is passed on to git diff, e.g.
#   diffNoIndex.sh -n dev_1a2b3c4 feature_5d6e7f8 -- '*DAO.java'
#   diffNoIndex.sh --stat --ignore-cr-at-eol dev_1a2b3c4 feature_5d6e7f8
# Options that take a value have to be written as one word (--output=file, -U5).

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
compare_dir="$(git -C "$script_dir" rev-parse --show-toplevel)/build/generate-compare"

options=()
dirs=()
pathspecs=()
only_pathspecs=false
for arg in "$@"; do
  if $only_pathspecs; then
    pathspecs+=("$arg")
  elif [ "$arg" = "--" ]; then
    only_pathspecs=true
  elif [ "$arg" = "-n" ]; then
    options+=(--name-only)
  elif [[ "$arg" == -* ]]; then
    options+=("$arg")
  elif [ ${#dirs[@]} -lt 2 ]; then
    # Folders that are not in the compare dir may still be given relative to the current directory
    if [ ! -e "$compare_dir/$arg" ] && [ -e "$arg" ]; then
      arg="$(cd "$arg" && pwd)"
    fi
    dirs+=("$arg")
  else
    pathspecs+=("$arg")
  fi
done

if [ ${#dirs[@]} -lt 2 ]; then
  echo 'Usage: diffNoIndex.sh [-n] [<git diff options>] <dirA> <dirB> [[--] <pathspec>...]' >&2
  if [ -d "$compare_dir" ]; then
    echo "Available in $compare_dir:" >&2
    ls -1 "$compare_dir" | sed 's/^/  /' >&2
  fi
  exit 2
fi
if [ ! -d "$compare_dir" ]; then
  echo "$compare_dir does not exist. Run generateBranches.sh first." >&2
  exit 2
fi

cd "$compare_dir" || exit 2
# longpaths: the generated files easily exceed the 260 characters of Windows
if [ ${#pathspecs[@]} -gt 0 ]; then
  exec git -c core.longpaths=true -c core.safecrlf=false diff --no-index "${options[@]}" "${dirs[@]}" -- "${pathspecs[@]}"
fi
exec git -c core.longpaths=true -c core.safecrlf=false diff --no-index "${options[@]}" "${dirs[@]}"
