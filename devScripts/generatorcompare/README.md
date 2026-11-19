# Diffing of different MontiGem Generator versions

> **Note:** The scripts in this folder were fully AI generated.

When refactoring a code generator like MontiGem it is sometimes useful to perform 
diffing between the generated output of different version. 
These scripts should help with that. 

Use `generateBranches` to generate the code of a project from two different branches.
You can find the output in `./build/generate-compare/<branchname>_<commit_id>/<project dir>`. 
By default, the script assumes you want to compare `dev` with the branch you currently
checked out. 

By default, the backend and frontend of `examples/vis-lib` are generated. Use `-p <project dir>` 
to choose another folder. The generators run in the Gradle project of that folder and in all 
projects below it:

```ps1
.\devScripts\generatorcompare\generateBranches.ps1 -p examples/dashboard
.\devScripts\generatorcompare\generateBranches.ps1 -p umlp-rte/frontend              # all libs
.\devScripts\generatorcompare\generateBranches.ps1 -p umlp-rte/frontend/networkgraph # one lib
```

The script runs the Gradle task `prepareGeneratorDiff` of these projects, which runs the 
generators and writes the folders of the generated code to 
`<project>/build/prepareGeneratorDiff/dirs.txt`. Both branches need to have that task.

Afterward use `diffNoIndex` to perform the actual diff. It expects two directories and
supports you with autocomplete for directories lying in `build/generate-compare`.   
Use the `-n` before the dir args to perform a name-only diff.  
The underlying tool is `git diff` and you can pass glob patterns after the dir args 
with a preceding `--`.

Example workflow: 

```ps1 
cd ~/**/montigem
git checkout gemmap-attribute-qualifiers

.\devScripts\generatorcompare\generateBranches.ps1 
.\devScripts\generatorcompare\diffNoIndex.ps1 dev_f5266a4df gemmap-attribute-qualifiers_329927047 -- **Manager*
```

To read a diff in the browser, pipe it into `gitdiff2html.py`, which renders it as a static HTML page, e.g. 
`./diffNoIndex.sh dev_f5266a4df gemmap-attribute-qualifiers_329927047 | python3 gitdiff2html.py --split -o diff.html`.
The script only needs Python 3 and no further packages; use `--split` for a side-by-side view instead of a unified one.
