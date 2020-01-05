# Script for deploying to gh-pages.
# Requires a git ./deployment directory which tracks a gh-pages branch.

Write-Output "Running tests."
yarn test
if ($LastExitCode -ne 0) { throw "Tests failed." }

Write-Output "Generating production build."
yarn build
if ($LastExitCode -ne 0) { throw "Build failed." }

Write-Output "Deleting ./deployment contents."
Remove-Item -Recurse ./deployment/*

Write-Output "Copying dist to ./deployment."
Copy-Item -Recurse ./dist/* ./deployment
Write-Output "www.phantasmal.world" > deployment/CNAME

Write-Output "Bumping version."
$version = Get-Content -Path version.txt
$version = $version / 1 + 1
Write-Output $version > version.txt

Write-Output "Committing and pushing to gh-pages."
Set-Location ./deployment
git pull
if ($LastExitCode -ne 0) { throw "Git pull failed." }
git add .
if ($LastExitCode -ne 0) { throw "Git add failed." }
git commit -m "Release $version."
if ($LastExitCode -ne 0) { throw "Git commit failed." }
git push
if ($LastExitCode -ne 0) { throw "Git push failed." }

Set-Location ..

Write-Output ""
Write-Output "Deployed release $version successfully."
Write-Output ""
