# (c) https://github.com/MontiCore/monticore
#
# Tab completion for diffNoIndex.sh: suggests the folders in <repo>/build/generate-compare.
# Enable with (e.g. in ~/.bashrc):
#   source <repo>/devScripts/generatorcompare/diffNoIndex.completion.bash

_diffNoIndex_complete() {
  local cur="${COMP_WORDS[COMP_CWORD]}"
  COMPREPLY=()
  if [[ "$cur" == -* ]]; then
    COMPREPLY=($(compgen -W '-n --stat --ignore-cr-at-eol --ignore-all-space --' -- "$cur"))
    return
  fi
  local root
  root="$(git rev-parse --show-toplevel 2>/dev/null)" || return
  [ -d "$root/build/generate-compare" ] || return
  local IFS=$'\n'
  COMPREPLY=($(cd "$root/build/generate-compare" && compgen -d -- "$cur"))
}

complete -F _diffNoIndex_complete diffNoIndex.sh
