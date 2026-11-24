@echo off
setlocal
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0space-invaders-launcher.jar" %*
) else (
    java -jar "%~dp0space-invaders-launcher.jar" %*
)
if errorlevel 1 (
    echo The launcher needs Java 17 or later. See README.txt.
    pause
)
