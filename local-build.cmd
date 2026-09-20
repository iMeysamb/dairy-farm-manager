@echo off
setlocal
set "JAVA_HOME="
if exist "C:\Program Files\Microsoft\jdk-17\bin\java.exe" set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Microsoft\jdk-17.*") do if exist "%%~fJ\bin\java.exe" if not defined JAVA_HOME set "JAVA_HOME=%%~fJ"
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin;%PATH%
cd /d "%~dp0"
if defined JAVA_HOME (
  echo Using Java: %JAVA_HOME%
) else (
  echo Java 17 was not found under C:\Program Files\Microsoft
  echo Install Microsoft OpenJDK 17 first.
  exit /b 1
)
if exist "%ANDROID_HOME%\platforms\android-34" (
  echo Android SDK ready.
) else (
  echo Android SDK not ready. Install Android SDK Platform 34 and Build Tools 34 in Android Studio.
  exit /b 1
)
call gradlew.bat assembleDebug
endlocal
