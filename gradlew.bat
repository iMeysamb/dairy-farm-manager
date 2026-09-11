@echo off
setlocal
set DIR=%~dp0
cd /d "%DIR%"
set GRADLE_HOME=C:\Gradle\gradle-8.7
if exist "%GRADLE_HOME%\bin\gradle.bat" (
  call "%GRADLE_HOME%\bin\gradle.bat" %*
) else (
  echo Gradle was not found at %GRADLE_HOME%\bin\gradle.bat
  echo Please install Gradle or point GRADLE_HOME to the right path.
  exit /b 1
)
endlocal
