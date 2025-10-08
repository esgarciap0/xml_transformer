# build-installer.ps1 — compila JAR sombreado y genera .exe con jpackage (PS 5.1)
param(
  [string]$AppName   = "XML Transformer",
  [string]$Version   = "1.0.0",
  [string]$Vendor    = "TriTech Software",
  [string]$MainClass = "xml.json.transformer.Main",
  [string]$IconPath  = "app.ico"
)

$ErrorActionPreference = "Stop"

function Require($cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    throw "$cmd no está en PATH."
  }
}

# usa mvnw.cmd si existe; si no, mvn
$mvnc = "mvn"
if (Test-Path ".\mvnw.cmd") { $mvnc = ".\mvnw.cmd" }
Require $mvnc
Require "jpackage"

Write-Host "» Compilando (shaded JAR)..." -ForegroundColor Cyan
& $mvnc -q -DskipTests clean package

# localizar el shaded jar
$jar = Get-ChildItem ".\target" -Filter "*-shaded.jar" | Select-Object -First 1
if (-not $jar) { throw "No se encontró el shaded JAR en /target." }

# args para jpackage (runtime del JDK completo)
$args = @(
  "--type","exe",
  "--name",$AppName,
  "--app-version",$Version,
  "--vendor",$Vendor,
  "--input","target",
  "--main-jar",$jar.Name,
  "--main-class",$MainClass,
  "--win-dir-chooser","--win-menu","--win-shortcut","--win-per-user-install"
)
if (Test-Path $IconPath) { $args += @("--icon",$IconPath) }

Write-Host "» Empaquetando instalador (.exe)..." -ForegroundColor Cyan
jpackage @args

Write-Host "✅ Instalador generado." -ForegroundColor Green
