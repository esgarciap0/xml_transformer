# Build Windows installer: shaded JAR -> app image -> .exe
[CmdletBinding()]
param(
  [string]$AppName = "XML Transformer",
  [string]$Version = "1.0.0",
  [string]$Vendor = "EdwinSoftware",
  [string]$MainClass = "xml.json.transformer.Main",
  [string]$IconPath = "..\src\main\resources\app.ico"
)

$ErrorActionPreference = "Stop"

function Require-Cmd([string]$CommandName) {
  if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
    throw "'$CommandName' is not available in PATH."
  }
}

function Remove-IfExists([string]$Path) {
  if (Test-Path $Path) {
    Remove-Item -LiteralPath $Path -Recurse -Force
  }
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mvnw = Join-Path $projectRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
  throw "Maven wrapper not found: $mvnw"
}

Require-Cmd "jpackage"

$distRoot = Join-Path $projectRoot "dist"
$windowsDist = Join-Path $distRoot "windows"
$appImageDest = Join-Path $windowsDist "app-image"
$installerDest = Join-Path $windowsDist "installer"
$appImageDir = Join-Path $appImageDest $AppName

Write-Host "Building shaded JAR..." -ForegroundColor Cyan
Push-Location $projectRoot
& $mvnw -q -DskipTests clean package
Pop-Location

$jar = Get-ChildItem (Join-Path $projectRoot "target") -Filter "*-shaded.jar" | Select-Object -First 1
if (-not $jar) {
  throw "No *-shaded.jar found in target."
}

$resourceDir = Join-Path $projectRoot "src\main\resources"
$iconAbs = (Resolve-Path (Join-Path $PSScriptRoot $IconPath) -ErrorAction SilentlyContinue).Path
if (-not $iconAbs -or -not (Test-Path $iconAbs)) {
  Write-Warning "Icon not found, jpackage will use the default icon: $IconPath"
  $iconAbs = $null
}

Remove-IfExists $windowsDist
New-Item -ItemType Directory -Force -Path $appImageDest, $installerDest | Out-Null

Write-Host "Generating Windows app image..." -ForegroundColor Cyan
$appImageArgs = @(
  "--type", "app-image",
  "--name", $AppName,
  "--app-version", $Version,
  "--vendor", $Vendor,
  "--input", (Join-Path $projectRoot "target"),
  "--main-jar", $jar.Name,
  "--main-class", $MainClass,
  "--resource-dir", $resourceDir,
  "--dest", $appImageDest
)
if ($iconAbs) {
  $appImageArgs += @("--icon", $iconAbs)
}
jpackage @appImageArgs

if (-not (Test-Path $appImageDir)) {
  throw "Expected app image was not generated: $appImageDir"
}

Write-Host "Generating Windows .exe installer..." -ForegroundColor Cyan
$exeArgs = @(
  "--type", "exe",
  "--name", $AppName,
  "--app-version", $Version,
  "--vendor", $Vendor,
  "--app-image", $appImageDir,
  "--dest", $installerDest,
  "--win-menu",
  "--win-shortcut",
  "--win-dir-chooser",
  "--win-per-user-install"
)
if ($iconAbs) {
  $exeArgs += @("--icon", $iconAbs)
}
jpackage @exeArgs

Write-Host "Windows build completed." -ForegroundColor Green
Write-Host "App image: $appImageDir"
Write-Host "Installer output: $installerDest"
