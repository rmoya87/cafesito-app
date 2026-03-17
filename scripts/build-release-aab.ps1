# Genera el AAB de release para Google Play y lo copia al escritorio.
# Requiere tener el keystore disponible de una de estas formas:
#
# 1) Variables de entorno (como en CI):
#    ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
#    Puedes copiar los valores desde GitHub → Settings → Secrets (solo tú o tu equipo).
#
# 2) Archivo de keystore en la ruta que indica keystore.properties (por defecto ../key-google-play).
#
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path (Join-Path $projectRoot "app\build.gradle.kts"))) {
    $projectRoot = Split-Path $PSScriptRoot -Parent
}
Set-Location $projectRoot

$keystorePath = $null
$restoreKeystoreProps = $false
$tempKeystore = Join-Path $projectRoot "android-release.keystore"
$propsPath = Join-Path $projectRoot "keystore.properties"
$desktop = [Environment]::GetFolderPath("Desktop")
$releaseAab = Join-Path $projectRoot "app\build\outputs\bundle\release\app-release.aab"
$destAab = Join-Path $desktop "Cafesito-app-release.aab"

# Intentar usar keystore desde base64 (secretos de CI)
if ($env:ANDROID_KEYSTORE_BASE64) {
    Write-Host "Usando keystore desde ANDROID_KEYSTORE_BASE64..."
    $b64 = ($env:ANDROID_KEYSTORE_BASE64 -replace "\s", "")
    $pad = (4 - ($b64.Length % 4)) % 4
    if ($pad -gt 0) { $b64 = $b64 + ("=" * $pad) }
    try {
        [IO.File]::WriteAllBytes($tempKeystore, [Convert]::FromBase64String($b64))
    } catch {
        Write-Error "ANDROID_KEYSTORE_BASE64 no es base64 válido: $_"
    }
    if (-not $env:ANDROID_KEYSTORE_PASSWORD -or -not $env:ANDROID_KEY_ALIAS -or -not $env:ANDROID_KEY_PASSWORD) {
        Write-Error "Faltan ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS o ANDROID_KEY_PASSWORD"
    }
    # Respaldar y reemplazar keystore.properties
    if (Test-Path $propsPath) {
        Copy-Item $propsPath "$propsPath.bak" -Force
        $restoreKeystoreProps = $true
    }
    @"
storeFile=android-release.keystore
storePassword=$($env:ANDROID_KEYSTORE_PASSWORD)
keyAlias=$($env:ANDROID_KEY_ALIAS)
keyPassword=$($env:ANDROID_KEY_PASSWORD)
"@ | Set-Content $propsPath -Encoding UTF8
    $keystorePath = $tempKeystore
} else {
    # Usar ruta de keystore.properties existente
    if (Test-Path $propsPath) {
        $line = Get-Content $propsPath | Where-Object { $_ -match "^storeFile=(.+)$" } | Select-Object -First 1
        if ($line -match "^storeFile=(.+)$") {
            $rel = $Matches[1].Trim()
            $abs = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $rel))
            if (Test-Path $abs) {
                $keystorePath = $abs
                Write-Host "Keystore encontrado: $abs"
            }
        }
    }
}

if (-not $keystorePath -or -not (Test-Path $keystorePath)) {
    Write-Host ""
    Write-Host "No se encontró el keystore de release." -ForegroundColor Yellow
    Write-Host "Opciones:"
    Write-Host "  1) Exporta en tu shell las variables ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD (desde GitHub Secrets) y vuelve a ejecutar este script."
    Write-Host "  2) Coloca el archivo del keystore en la ruta que indica keystore.properties (por defecto: carpeta padre del proyecto, archivo key-google-play)."
    Write-Host "     Ruta esperada actual: $(Join-Path (Split-Path $projectRoot -Parent) 'key-google-play')"
    exit 1
}

try {
    & .\gradlew.bat --no-daemon --warning-mode none bundleRelease
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if (Test-Path $releaseAab) {
        Copy-Item $releaseAab $destAab -Force
        Write-Host "AAB de release copiado a: $destAab" -ForegroundColor Green
    } else {
        Write-Warning "No se encontró app-release.aab tras el build."
    }
} finally {
    if ($restoreKeystoreProps -and (Test-Path "$propsPath.bak")) {
        Move-Item "$propsPath.bak" $propsPath -Force
    }
    if (Test-Path $tempKeystore) {
        Remove-Item $tempKeystore -Force
    }
}
