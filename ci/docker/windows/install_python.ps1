$url = ('https://www.python.org/ftp/python/{0}/python-{1}.amd64.msi' -f $env:PYTHON_RELEASE, $env:PYTHON_VERSION)
$installer='C:\TEMP\python.msi'
Write-Host ('Downloading {0} ...' -f $url)
(New-Object System.Net.WebClient).DownloadFile($url, $installer)
Write-Host 'Installing ...'
Start-Process msiexec -Wait -ArgumentList @('/i', $installer, '/quiet', '/qn', 'TARGETDIR=C:\Python', 'ALLUSERS=1', 'ADDLOCAL=DefaultFeature,Extensions,TclTk,Tools,PrependPath')
Remove-Item $installer -Force
Write-Host 'Python installation is complete.'