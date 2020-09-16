param ($url, $targetDir)
$archiveFile="C:\TEMP\corretto-11-jdk.zip" 
Write-Host ('Downloading {0} ...' -f $url)		
(New-Object System.Net.WebClient).DownloadFile($url, $archiveFile)
Write-Host 'Installing ...'
tar -xf $archiveFile
# rename an unpacked directory like 'jdk11.0.10' to 'jdk11'
$jdkDir=Get-ChildItem -Filter "jdk11.*"|Select-Object -First 1
Rename-Item -Path $jdkDir.FullName -NewName $targetDir
Remove-Item $archiveFile -Force 		
Write-Host 'Installation is complete.'
