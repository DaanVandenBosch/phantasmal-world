# Script for deploying to gh-pages.
# Requires a git ./deployment directory which tracks a gh-pages branch.
Param([Parameter(Mandatory = $true)][Int32]$release)

Write-Output "Running tests."
yarn test

Write-Output "Generating production build."
yarn build

Write-Output "Deleting ./deployment contents."
Remove-Item -Recurse ./deployment/*

Write-Output "Copying dist to ./deployment."
Copy-Item -Recurse ./dist/* ./deployment
Write-Output "www.phantasmal.world" > deployment/CNAME

Write-Output "Committing and pushing to gh-pages."
Set-Location ./deployment
git pull
git add .
git commit -m "Release $release."
git push

Set-Location ..
