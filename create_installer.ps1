param(
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

if (-not $SkipBuild) {
    Write-Host "Construyendo el proyecto con Maven..." -ForegroundColor Cyan
    .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Fallo en la construcción con Maven."
        exit $LASTEXITCODE
    }
}

Write-Host "Preparando archivos para jpackage..." -ForegroundColor Cyan
$inputDir = "target\installer-input"
$outputDir = "target\installer"
$iconPath = "src\main\resources\app.ico"

try {
    if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir -ErrorAction Stop }
    if (Test-Path $outputDir) { Remove-Item -Recurse -Force $outputDir -ErrorAction Stop }
} catch {
    $outputDir = "target\installer_dist"
    if (Test-Path $outputDir) { Remove-Item -Recurse -Force $outputDir -ErrorAction SilentlyContinue }
}
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

Copy-Item "target\PlaylistDownloader.jar" -Destination $inputDir
if (Test-Path "src\main\Libs") {
    Copy-Item -Path "src\main\Libs" -Destination "$inputDir\Libs" -Recurse -Force
}

Write-Host "Generando instalador (.exe) usando jpackage..." -ForegroundColor Cyan
jpackage `
  --type exe `
  --dest $outputDir `
  --input $inputDir `
  --name "PlaylistDownloader" `
  --main-class com.example.interfaz.app.Launcher `
  --main-jar PlaylistDownloader.jar `
  --icon $iconPath `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser `
  --app-version "1.0.0"

if ($LASTEXITCODE -eq 0) {
    Write-Host "¡Instalador generado correctamente en la carpeta $outputDir!" -ForegroundColor Green
} else {
    Write-Error "Ocurrió un error al generar el instalador."
}

# Limpiar directorio temporal
if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
