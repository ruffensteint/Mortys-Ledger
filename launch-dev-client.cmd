@echo off
setlocal
cd /d "%~dp0"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JAVA_HOME is not set to a valid JDK installation.
  echo Current value: %JAVA_HOME%
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
call gradlew.bat run

if errorlevel 1 (
  echo.
  echo RuneLite development client exited with an error.
  pause
)
