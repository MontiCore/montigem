# (c) https://github.com/MontiCore/monticore
#
# Runs the generators of a project on two branches and stores each result under
#   <repo>/build/generate-compare/<branch>_<short commit id>/<project dir>/
# so the generated code can be compared (see diffNoIndex.ps1).
# The Gradle task prepareGeneratorDiff runs the generators and writes the folders of the generated
# code to <project>/build/prepareGeneratorDiff/dirs.txt, so both branches need that task.
#
# Usage: generateBranches.ps1 [-p <project dir>] [<branchA> [<branchB>]]
#   -p             folder of the Gradle project(s) to generate, default examples/vis-lib.
#                  The task runs in the project of that folder and in all projects below it, e.g.
#                    -p examples/vis-lib                  backend and frontend of vis-lib
#                    -p umlp-rte/frontend                 all libs of the RTE
#                    -p umlp-rte/frontend/networkgraph    one lib
#   no branch:     current branch vs. dev
#   one branch:    <branchA> vs. current branch

# not positional, so a single argument is a branch and not the project
[CmdletBinding(PositionalBinding = $false)]
param(
  [Alias('p')]
  [string]$Project,
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$Branches
)

$ErrorActionPreference = 'Stop'

$defaultProjectDir = 'examples/vis-lib'
$taskName = 'prepareGeneratorDiff'
$dirsFile = 'build/prepareGeneratorDiff/dirs.txt'

$branchArgs = @($Branches | Where-Object { $_ })
if ($branchArgs.Count -gt 2) {
  Write-Host 'Usage: generateBranches.ps1 [-p <project dir>] [<branchA> [<branchB>]]   (default project: examples/vis-lib, no branch: current branch vs. dev, one branch: <branchA> vs. current branch)'
  exit 2
}

function Invoke-Native {
  $exe = $args[0]
  # Not "$exe, $rest = $args": Windows PowerShell 5.1 splats a single string argument char by char
  $rest = @($args | Select-Object -Skip 1)
  & $exe @rest
  if ($LASTEXITCODE -ne 0) { throw "'$args' failed with exit code $LASTEXITCODE" }
}

# The folders of the projects in or below the given folder that have a dirs file.
# Does not descend into build folders, the frontend ones contain node_modules.
function Get-ProjectsWithDirsFile($dir) {
  if (Test-Path "$dir/$dirsFile") { $dir }
  foreach ($child in Get-ChildItem -LiteralPath $dir -Directory -Force) {
    if ($child.Name -notin 'build', 'node_modules', '.git') {
      Get-ProjectsWithDirsFile "$dir/$($child.Name)"
    }
  }
}

# The folders listed in the dirs file of the project, if there is one
function Get-GeneratedDirs($project) {
  $file = "$project/$dirsFile"
  if (Test-Path $file) {
    @(Get-Content $file | ForEach-Object { $_.Trim() } | Where-Object { $_ })
  }
}

# Removes the output of the previous run so nothing stale from the other branch survives
function Remove-Generated($root, $project) {
  foreach ($dir in Get-GeneratedDirs $project) {
    # the paths come from a file, never delete outside the repository
    if (-not $dir.StartsWith("$root/", [StringComparison]::OrdinalIgnoreCase)) {
      Write-Warning "Not removing $dir, it is outside of $root"
    }
    elseif (Test-Path $dir) { Remove-Item -Recurse -Force $dir }
  }
  if (Test-Path "$project/$dirsFile") { Remove-Item -Force "$project/$dirsFile" }
}

# Copies the generated folders of the project to the target, keeping their path below build/
function Copy-Generated($project, $target) {
  $buildDir = "$project/build/"
  foreach ($dir in Get-GeneratedDirs $project) {
    if (-not (Test-Path $dir -PathType Container)) { continue }
    $rel = if ($dir.StartsWith($buildDir, [StringComparison]::OrdinalIgnoreCase)) {
      $dir.Substring($buildDir.Length)
    } else { Split-Path -Leaf $dir }
    $destination = Join-Path $target $rel
    New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
    Copy-Item -Recurse $dir $destination
  }
}

$root = (git -C $PSScriptRoot rev-parse --show-toplevel)
if ($LASTEXITCODE -ne 0) { throw 'Not inside a git repository' }

# a given folder may be relative to the current directory (tab completion) or to the repository
if ($Project -and (Test-Path $Project -PathType Container)) {
  $projectDir = git -C $Project rev-parse --show-prefix
}
else {
  if (-not $Project) { $Project = $defaultProjectDir }
  if (-not (Test-Path "$root/$Project" -PathType Container)) {
    Write-Host "Project folder $Project does not exist"
    exit 2
  }
  $projectDir = git -C "$root/$Project" rev-parse --show-prefix
}
# empty for the root of the repository
$projectDir = "$projectDir".TrimEnd('/')
$projectRoot = if ($projectDir) { "$root/$projectDir" } else { $root }
$projectLabel = if ($projectDir) { $projectDir } else { '.' }

Push-Location $root
try {
  if (git status --porcelain --untracked-files=no) {
    throw 'Working tree has uncommitted changes. Commit or stash them first.'
  }

  $gradle = if (Test-Path './gradlew.bat') { "$root/gradlew.bat" } else { 'gradle' }
  $original = git symbolic-ref --short -q HEAD
  if (-not $original) { $original = git rev-parse HEAD }

  $branches = switch ($branchArgs.Count) {
    0 { $original, 'dev' }
    1 { $branchArgs[0], $original }
    2 { $branchArgs[0], $branchArgs[1] }
  }
  Write-Host "Project:       $projectLabel"
  Write-Host "First branch:  $($branches[0])"
  Write-Host "Second branch: $($branches[1])"

  $results = @()
  try {
    foreach ($branch in $branches) {
      Write-Host "`n=== $branch ===" -ForegroundColor Cyan
      Invoke-Native git checkout $branch
      $shortId = git rev-parse --short HEAD

      if (-not (Test-Path $projectRoot -PathType Container)) {
        throw "Project folder $projectLabel does not exist on $branch"
      }

      foreach ($p in @(Get-ProjectsWithDirsFile $projectRoot)) { Remove-Generated $root $p }

      # runs the task in the project of the folder and in all projects below it
      Invoke-Native $gradle -p $projectRoot $taskName

      $projects = @(Get-ProjectsWithDirsFile $projectRoot)
      if ($projects.Count -eq 0) { throw "$taskName did not write any $dirsFile below $projectLabel" }

      # only the part of this project is replaced, the results of other projects stay
      $target = "$root/build/generate-compare/$($branch -replace '[\\/]', '-')_$shortId"
      $projectTarget = if ($projectDir) { "$target/$projectDir" } else { $target }
      if (Test-Path $projectTarget) { Remove-Item -Recurse -Force $projectTarget }
      foreach ($p in $projects) {
        Copy-Generated $p "$target/$($p.Substring($root.Length + 1))"
      }
      $results += $target
    }
  }
  finally {
    Write-Host "`nRestoring $original" -ForegroundColor Cyan
    git checkout $original
  }

  Write-Host "`nGenerated output:" -ForegroundColor Green
  $results | ForEach-Object { Write-Host "  $_" }
  Write-Host "`nCompare with:"
  Write-Host "  $PSScriptRoot\diffNoIndex.ps1 -n $(Split-Path -Leaf $results[1]) $(Split-Path -Leaf $results[0])"
}
finally {
  Pop-Location
}
