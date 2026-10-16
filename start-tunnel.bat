@echo off
title Scam Shield Public Cloudflare Tunnel
echo ========================================================
echo   Starting Cloudflare Public Tunnel for Scam Shield...
echo ========================================================
cd /d "%~dp0"
bin\cloudflared.exe tunnel --url http://localhost:3000
pause
