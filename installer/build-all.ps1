# build-all.ps1 — build completo: shaded JAR -> app-image -> installer .exe (PS 5.1)
[CmdletBinding()]
param(
  [string]$AppName   = "XML Transformer",
  [string]$Version   = "1.0.0",
  [string]$Vendor    = "EdwinSoftware",
  [string]$MainClass = "xml.json.transformer.Main",
  [string]$IconPath  = "..\src\main\resources\app.ico"
)

$ErrorActionPreference = "Stop"

function RequireCmd([string]$cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    throw "❌ '$cmd' no está en PATH."
  }
}

Write-Host "Starting build-all.ps1..." -ForegroundColor Cyan

# Project root = carpeta padre de /installer
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

# Maven Wrapper (NO depende de mvn instalado)
$mvnw = Join-Path $projectRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
  throw "❌ No se encontró mvnw.cmd en la raíz del proyecto: $mvnw"
}

RequireCmd "jpackage"

# =====================================================
# 1) Compilar shaded JAR
# =====================================================
Write-Host "Compiling shaded JAR..." -ForegroundColor Cyan
Push-Location $projectRoot
& $mvnw -q -DskipTests clean package
Pop-Location

# Ubicar shaded jar
$jar = Get-ChildItem (Join-Path $projectRoot "target") -Filter "*-shaded.jar" | Select-Object -First 1
if (-not $jar) {
  throw "❌ No se encontró *-shaded.jar en: $projectRoot\target"
}

Write-Host ("Using shaded jar: {0}" -f $jar.Name) -ForegroundColor Yellow

# Recursos
$resourceDir = Join-Path $projectRoot "src\main\resources"
if (-not (Test-Path $resourceDir)) {
  throw "❌ No existe resource-dir: $resourceDir"
}

# Ruta absoluta del icono (mejor para jpackage)
$iconAbs = (Resolve-Path (Join-Path $PSScriptRoot $IconPath) -ErrorAction SilentlyContinue).Path
if (-not $iconAbs -or -not (Test-Path $iconAbs)) {
  Write-Warning "⚠️ No se encontró el icono .ico en: $IconPath (se usará icono por defecto)."
  $iconAbs = $null
} else {
  Write-Host ("Using icon: {0}" -f $iconAbs) -ForegroundColor Yellow
}

# =====================================================
# 2) Limpiar app-image anterior (evita: destination directory already exists)
# =====================================================
$appImageDir = Join-Path $projectRoot $AppName
if (Test-Path $appImageDir) {
  Write-Host "Removing previous app-image: $appImageDir" -ForegroundColor DarkYellow
  Remove-Item -Recurse -Force $appImageDir
}

# =====================================================
# 3) Generar app-image
# =====================================================
Write-Host "Generating app-image..." -ForegroundColor Cyan

$appImageArgs = @(
  "--type","app-image",
  "--name",$AppName,
  "--app-version",$Version,
  "--vendor",$Vendor,
  "--input",(Join-Path $projectRoot "target"),
  "--main-jar",$jar.Name,
  "--main-class",$MainClass,
  "--resource-dir",$resourceDir
)

if ($iconAbs) {
  $appImageArgs += @("--icon",$iconAbs)
}

Push-Location $projectRoot
jpackage @appImageArgs
Pop-Location

if (-not (Test-Path $appImageDir)) {
  throw "❌ No se generó la app-image esperada en: $appImageDir"
}

# =====================================================
# 4) Generar instalador .exe desde app-image
# =====================================================
Write-Host "Generating installer .exe..." -ForegroundColor Cyan

$exeArgs = @(
  "--type","exe",
  "--name",$AppName,
  "--app-version",$Version,
  "--vendor",$Vendor,
  "--app-image",$appImageDir,
  "--win-menu",
  "--win-shortcut",
  "--win-dir-chooser",
  "--win-per-user-install"
)

if ($iconAbs) {
  $exeArgs += @("--icon",$iconAbs)
}

Push-Location $projectRoot
jpackage @exeArgs
Pop-Location

Write-Host "✅ Build COMPLETO finalizado correctamente" -ForegroundColor Green
Write-Host ("Output (project root): {0}" -f $projectRoot) -ForegroundColor Gray
