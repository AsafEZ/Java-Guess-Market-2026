@echo off
setlocal
cd /d "%~dp0"

node --version >nul 2>&1
if errorlevel 1 (
  echo Node.js was not found in PATH. Install Node.js and try again.
  pause
  exit /b 1
)
call npm --version >nul 2>&1
if errorlevel 1 (
  echo npm was not found in PATH. Install Node.js with npm and try again.
  pause
  exit /b 1
)

if not exist "node_modules\vite\bin\vite.js" (
  echo Installing Web client dependencies...
  call npm ci --no-audit --no-fund
  if errorlevel 1 (
    echo Dependency installation failed.
    pause
    exit /b 1
  )
)

echo Start the Assignment 3 Tomcat server before signing in.
echo Open http://127.0.0.1:5173/ in your browser.
echo Keep this window open while using the Web client.
call npm run dev
if errorlevel 1 (
  echo The Web client did not start. Check whether port 5173 is already in use.
  pause
  exit /b 1
)
