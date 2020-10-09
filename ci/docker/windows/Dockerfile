# escape=`

# Use the latest Windows Server Core image with .NET Framework 4.8.
FROM mcr.microsoft.com/dotnet/framework/sdk:4.8-windowsservercore-ltsc2019

# Restore the default Windows shell for correct batch processing.
SHELL ["cmd", "/S", "/C"]

# Install MSVC C++ compiler, CMake, and MSBuild.
ADD https://aka.ms/vs/16/release/vs_buildtools.exe C:\Temp\vs_buildtools.exe
ADD https://aka.ms/vs/16/release/channel C:\Temp\VisualStudio.chman
RUN C:\Temp\vs_buildtools.exe `
    --quiet --wait --norestart --nocache `
    --installPath C:\BuildTools `
    --channelUri C:\Temp\VisualStudio.chman `
    --installChannelUri C:\Temp\VisualStudio.chman `
    --add Microsoft.VisualStudio.Workload.VCTools;includeRecommended `
    --add Microsoft.Component.MSBuild `
 || IF "%ERRORLEVEL%"=="3010" EXIT 0

RUN setx /M SKIKO_VSBT_PATH "C:\BuildTools"

# Install Java
COPY install_jdk.ps1 C:\TEMP\install_jdk.ps1
RUN powershell C:\TEMP\install_jdk.ps1 -url https://corretto.aws/downloads/latest/amazon-corretto-11-x64-windows-jdk.zip -targetDir C:\jdk11
RUN setx /M PATH "C:\jdk11\bin;%PATH%"
RUN setx /M JAVA_HOME C:\jdk11

ENV PYTHON_VERSION=2.7.18
ENV PYTHON_RELEASE=2.7.18
ADD install_python.ps1 C:\TEMP\install_python.ps1
RUN powershell C:\TEMP\install_python.ps1

ADD https://bintray.com/jetbrains/skija/download_file?file_path=zip.zip C:\TEMP\zip.zip
RUN tar -xf C:\TEMP\zip.zip
RUN setx /M PATH "C:\zip;%PATH%"

COPY install_git.ps1 C:\TEMP\install_git.ps1
RUN powershell C:\TEMP\install_git.ps1
RUN setx /M PATH "C:\Git\cmd;C:\Git\bin;C:\Git\usr\bin;%PATH%"

RUN git.exe clone "https://chromium.googlesource.com/chromium/tools/depot_tools.git" "C:\depot_tools"
RUN setx /M PATH "C:\depot_tools;%PATH%"

# Define the entry point for the docker container.
# This entry point starts the developer command prompt and launches the PowerShell shell.
ENTRYPOINT ["C:\\BuildTools\\Common7\\Tools\\VsDevCmd.bat", "&&", "powershell.exe", "-NoLogo", "-ExecutionPolicy", "Bypass"]
