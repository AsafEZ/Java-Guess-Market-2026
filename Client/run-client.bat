@echo off
setlocal
java -version >nul 2>nul
if errorlevel 1 (
    echo Java 25 is required. Add it to PATH and try again.
    exit /b 1
)
pushd "%~dp0"
java "-Duser.home=%USERPROFILE%" --enable-native-access=ALL-UNNAMED -cp "Client.jar;lib\*" guessmarket.client.ClientLauncher
set "result=%errorlevel%"
popd
exit /b %result%
