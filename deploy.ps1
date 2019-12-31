# Script for deploying to gh-pages.
# Requires a git ./deployment directory which tracks a gh-pages branch.

Write-Output "Running tests."
yarn test

Write-Output "Generating production build."
yarn build

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
git add .
git commit -m "Release $version."
git push

Set-Location ..
