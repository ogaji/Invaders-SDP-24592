@echo off
setlocal
pushd "%~dp0" || exit /b 1
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar space-invaders-launcher.jar %*
) else (
    java -jar space-invaders-launcher.jar %*
)
set "launcher_exit=%ERRORLEVEL%"
popd
if not "%launcher_exit%"=="0" (
    echo Launcher stopped. Check the error above and README.txt. Java 17 or later is required.
    if "%~1"=="" pause
)
exit /b %launcher_exit%
