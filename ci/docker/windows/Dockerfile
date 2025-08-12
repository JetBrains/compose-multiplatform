# escape=`

# Use the latest Windows Server Core 2022 image.
FROM mcr.microsoft.com/windows/servercore:ltsc2022

# Restore the default Windows shell for correct batch processing.
SHELL ["cmd", "/S", "/C"]

# Download BuildTools installer

ADD https://aka.ms/vs/17/release/vs_buildtools.exe C:\Temp\vs_buildtools.exe

# Download channel for fixed install.
ADD https://aka.ms/vs/17/release/channel C:\Temp\VisualStudio.chman

# Install MSVC C++ compiler, CMake, and MSBuild.
RUN `
    # Install Build Tools excluding workloads and components with known issues.
    start /w C:\Temp\vs_buildtools.exe --quiet --wait --norestart --nocache `
    --installPath "C:\BuildTools" `
    --channelUri C:\Temp\VisualStudio.chman `
    --installChannelUri C:\Temp\VisualStudio.chman `
    --add Microsoft.VisualStudio.Workload.VCTools;includeRecommended `
    --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 `
    --add Microsoft.VisualStudio.Component.VC.Tools.ARM64 `
    --add Microsoft.Component.MSBuild `
    --remove Microsoft.VisualStudio.Component.Windows10SDK.10240 `
    --remove Microsoft.VisualStudio.Component.Windows10SDK.10586 `
    --remove Microsoft.VisualStudio.Component.Windows10SDK.14393 `
    --remove Microsoft.VisualStudio.Component.Windows81SDK `
  || IF "%ERRORLEVEL%"=="3010" EXIT 0

RUN setx /M SKIKO_VSBT_PATH "C:\BuildTools"

ADD https://github.com/llvm/llvm-project/releases/download/llvmorg-20.1.7/LLVM-20.1.7-win64.exe C:\Temp\llvm.exe
RUN powershell Start-Process -Wait C:\Temp\llvm.exe -ArgumentList /S
RUN setx /M PATH "C:\Program Files\LLVM\bin;%PATH%"

# Install Java
COPY install_jdk.ps1 C:\TEMP\install_jdk.ps1
RUN powershell C:\TEMP\install_jdk.ps1 -url https://corretto.aws/downloads/latest/amazon-corretto-11-x64-windows-jdk.zip -targetDir C:\jdk11
RUN setx /M PATH "C:\jdk11\bin;%PATH%"
RUN setx /M JAVA_HOME C:\jdk11

ENV PYTHON_VERSION=2.7.18
ENV PYTHON_RELEASE=2.7.18
ADD install_python.ps1 C:\TEMP\install_python.ps1
RUN powershell C:\TEMP\install_python.ps1

COPY install_git.ps1 C:\TEMP\install_git.ps1
RUN powershell C:\TEMP\install_git.ps1
RUN setx /M PATH "C:\Git\cmd;C:\Git\bin;C:\Git\usr\bin;%PATH%"

RUN git.exe clone "https://chromium.googlesource.com/chromium/tools/depot_tools.git" "C:\depot_tools"
RUN setx /M PATH "C:\depot_tools;%PATH%"

# Define the entry point for the docker container.
# This entry point starts the developer command prompt and launches the PowerShell shell.
ENTRYPOINT ["C:\\BuildTools\\Common7\\Tools\\VsDevCmd.bat", "&&", "powershell.exe", "-NoLogo", "-ExecutionPolicy", "Bypass"]
