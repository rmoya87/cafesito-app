@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM Cafesito - Bootstrap de entorno local en Windows
REM Uso:
REM   scripts\run-dev-env.bat            -> instala deps web, levanta web y hace assembleDebug Android
REM   scripts\run-dev-env.bat --no-web   -> solo Android/shared
REM   scripts\run-dev-env.bat --no-android -> solo web
REM   scripts\run-dev-env.bat --tests    -> ejecuta tests shared + web antes de levantar

set ROOT_DIR=%~dp0..
cd /d "%ROOT_DIR%"

set RUN_WEB=1
set RUN_ANDROID=1
set RUN_TESTS=0

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--no-web" (
  set RUN_WEB=0
) else if /I "%~1"=="--no-android" (
  set RUN_ANDROID=0
) else if /I "%~1"=="--tests" (
  set RUN_TESTS=1
) else (
  echo [WARN] Argumento no reconocido: %~1
)
shift
goto parse_args

:args_done
echo ==============================================
echo   Cafesito - Auto entorno local (Windows)
echo   ROOT: %CD%
echo ==============================================

if "%RUN_TESTS%"=="1" (
  echo [INFO] Ejecutando tests previos...
  if "%RUN_ANDROID%"=="1" (
    call gradlew.bat :shared:allTests
    if errorlevel 1 goto fail
  )
  if "%RUN_WEB%"=="1" (
    pushd webApp
    call npm ci
    if errorlevel 1 (
      popd
      goto fail
    )
    call npm test
    if errorlevel 1 (
      popd
      goto fail
    )
    popd
  )
)

if "%RUN_ANDROID%"=="1" (
  echo [INFO] Compilando Android debug...
  call gradlew.bat :app:assembleDebug
  if errorlevel 1 goto fail
)

if "%RUN_WEB%"=="1" (
  echo [INFO] Instalando dependencias web...
  pushd webApp
  call npm ci
  if errorlevel 1 (
    popd
    goto fail
  )

  echo [INFO] Levantando webApp en http://localhost:4173
  start "Cafesito Web" cmd /k "npm run dev -- --host 0.0.0.0 --port 4173"
  popd
)

echo [OK] Entorno inicializado.
echo [TIP] Si Android Studio no esta abierto, abre el proyecto y ejecuta la app en emulador/dispositivo.
exit /b 0

:fail
echo [ERROR] Fallo la inicializacion del entorno.
exit /b 1
