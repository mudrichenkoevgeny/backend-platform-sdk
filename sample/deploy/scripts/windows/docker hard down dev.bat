@echo off
set "ROOT=%~dp0..\..\"

set "COMPOSE_FILES=-f "%ROOT%docker-compose.base.yml" -f "%ROOT%docker-compose.dev.yml""
set "ENV_FILES=--env-file "%ROOT%env\.versions.env" --env-file "%ROOT%env\.env.config.dev""

echo [1/1] Removing containers and ALL DATA (volumes)...
docker compose %ENV_FILES% %COMPOSE_FILES% down -v

echo.
echo DB cleanup done. Volumes removed.
pause