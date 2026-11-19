# (c) https://github.com/MontiCore/monticore
#
# Alias for "git diff --no-index" without the LF/CRLF line ending warnings.
# Runs inside <repo>/build/generate-compare, so the folders written by generateBranches.ps1
# can be given by name (with tab completion).
#
# Usage: diffNoIndex.ps1 [-n] [<git diff options>] <dirA> <dirB> [[--] <pathspec>...]
#   -n   only list the names of changed files (--name-only)
# Everything else is passed on to git diff, e.g.
#   diffNoIndex.ps1 -n dev_1a2b3c4 feature_5d6e7f8 -- '*DAO.java'
#   diffNoIndex.ps1 --stat --ignore-cr-at-eol dev_1a2b3c4 feature_5d6e7f8
# PowerShell only lets "--long" options through. Of the short ones -w, -b and -R are known,
# all others have to be quoted ('-U5') or written in their long form (--unified=5).

param(
  [Parameter(ValueFromRemainingArguments)]
  [ArgumentCompleter({
    param($commandName, $parameterName, $wordToComplete)
    $root = git -C $PSScriptRoot rev-parse --show-toplevel
    Get-ChildItem -Directory (Join-Path $root 'build/generate-compare') -Filter "$wordToComplete*" |
      ForEach-Object { $_.Name }
  })]
  [string[]] $Rest,

  [switch] $n,
  [switch] $w,
  [switch] $b,
  [switch] $R
)

$root = git -C $PSScriptRoot rev-parse --show-toplevel
$compareDir = Join-Path $root 'build/generate-compare'

$options = @()
if ($n) { $options += '--name-only' }
if ($w) { $options += '-w' }
if ($b) { $options += '-b' }
if ($R) { $options += '-R' }
$dirs = @()
$pathspecs = @()
foreach ($arg in $Rest) {
  if ($arg.StartsWith('-')) { $options += $arg }
  elseif ($dirs.Count -lt 2) {
    # Folders that are not in the compare dir may still be given relative to the current directory
    if (-not (Test-Path (Join-Path $compareDir $arg)) -and (Test-Path $arg)) { $arg = (Resolve-Path $arg).Path }
    $dirs += $arg
  }
  else { $pathspecs += $arg }
}

if ($dirs.Count -lt 2) {
  Write-Host 'Usage: diffNoIndex.ps1 [-n] [<git diff options>] <dirA> <dirB> [[--] <pathspec>...]'
  if (Test-Path $compareDir) {
    Write-Host "Available in ${compareDir}:"
    Get-ChildItem -Directory $compareDir | ForEach-Object { Write-Host "  $($_.Name)" }
  }
  exit 2
}
if (-not (Test-Path $compareDir)) {
  Write-Host "$compareDir does not exist. Run generateBranches.ps1 first."
  exit 2
}

# PowerShell swallows a literal "--", so it is re-added in front of the pathspecs
$gitArgs = $options + $dirs
if ($pathspecs.Count -gt 0) { $gitArgs += @('--') + $pathspecs }

Push-Location $compareDir
try {
  # longpaths: the generated files easily exceed the 260 characters of Windows
  git -c core.longpaths=true -c core.safecrlf=false diff --no-index @gitArgs
  $exitCode = $LASTEXITCODE
}
finally {
  Pop-Location
}
exit $exitCode
