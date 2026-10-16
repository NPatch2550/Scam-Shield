@echo off
title Scam Shield Backend Server
echo ========================================================
echo   Starting Scam Shield Backend Server on port 3000...
echo ========================================================
cd /d "%~dp0"
node src/server.js
pause
