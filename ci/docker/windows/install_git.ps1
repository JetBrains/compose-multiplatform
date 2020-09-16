# Install 7zip
Write-Host 'Downloading 7z...'
$7zinstaller='C:\TEMP\7zsetup.exe'
(New-Object System.Net.WebClient).DownloadFile('https://www.7-zip.org/a/7z1900-x64.exe', $7zinstaller)
Write-Host 'Installing 7z...'
Start-Process $7zinstaller -ArgumentList '/S' -Wait
Remove-Item $7zinstaller -Force
$env:Path += ';C:\Program Files\7-Zip\'

# Install Git
Write-Host 'Downloading Git...'
$gitarchive='C:\TEMP\portableGit.7z.exe'
(New-Object System.Net.WebClient).DownloadFile('https://github.com/git-for-windows/git/releases/download/v2.28.0.windows.1/PortableGit-2.28.0-64-bit.7z.exe', $gitarchive)
Write-Host 'Installing Git...'
7z.exe x $gitarchive -o'C:\Git'
Remove-Item $gitarchive -Force
