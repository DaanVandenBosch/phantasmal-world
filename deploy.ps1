# Script for deploying to gh-pages.
# Requires a git ./deployment directory which tracks a gh-pages branch.
Param([Parameter(Mandatory = $true)][Int32]$release)

Write-Output "Deleting ./deployment contents."
Remove-Item -Recurse ./deployment/*

Write-Output "Copying build to ./deployment."
Copy-Item -Recurse ./build/* ./deployment
Write-Output "www.phantasmal.world" > deployment/CNAME

Write-Output "Committing and pushing to gh-pages."
Set-Location ./deployment
git pull
git add .
git commit -m "Release $release."
git push
