@echo off
cd /d "%~dp0..\.."

set "V_ENV=env\.versions.env"
set "C_ENV=env\.env.config.dev"
set "FILES=-f docker-compose.base.yml -f docker-compose.dev.yml"
set "ENV_FILES=--env-file %V_ENV% --env-file %C_ENV%"

echo [1/3] Stopping and cleaning...
docker compose %ENV_FILES% %FILES% down --remove-orphans

echo [2/3] Building containers...
docker compose %ENV_FILES% %FILES% build

echo [3/3] Starting services...
docker compose %ENV_FILES% %FILES% up -d

echo.
echo All services are up. Following app logs:
docker compose %ENV_FILES% %FILES% logs -f app

pause