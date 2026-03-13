#!/usr/bin/env pwsh
# Bootstrap script for Levain 2 (JAR distribution)

$ErrorActionPreference = 'Stop'

if (! $levainHome) {
  $levainHome = "$HOME\levain"
}

if (! $levainUrlBase) {
  $levainUrlBase = "https://github.com/jmoalves/levain-2"
}

if (! $levainRepo) {
  $levainRepo = "https://github.com/jmoalves/levain-pkgs.git"
}

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$TempLevain = Join-Path $Env:Temp "levain-install-$([guid]::NewGuid())"

if (! $levainHeadless) {
  Clear-Host
}

if ($levainVersion) {
  Write-Output "=== Installing Levain 2 $levainVersion at $levainHome"
} else {
  Write-Output "=== Installing latest Levain 2 release at $levainHome"
}

Write-Output ""
Write-Output "Checking for Java..."

$javaExe = $null
try {
  $javaExe = (Get-Command java -ErrorAction Stop).Source
  $javaVersion = & $javaExe -version 2>&1 | Select-Object -First 1
  Write-Output "Found: $javaVersion"
} catch {
  Write-Output "ERROR: Java is not installed"
  Write-Output ""
  Write-Output "Levain 2 requires Java 25 or later."
  Write-Output ""
  Write-Output "Build from source alternative:"
  Write-Output "  git clone https://github.com/jmoalves/levain-2.git"
  Write-Output "  cd levain-2"
  Write-Output "  mvn clean package"
  Write-Output "  java -jar target\levain.jar install levain"
  exit 1
}

$JarUri = if ($levainVersion) {
  "${levainUrlBase}/releases/download/v${levainVersion}/levain.jar"
} else {
  "${levainUrlBase}/releases/latest/download/levain.jar"
}

$TempLevainJar = "$TempLevain\levain.jar"

if (!(Test-Path $TempLevain)) {
  New-Item $TempLevain -ItemType Directory | Out-Null
}

Write-Output ""
Write-Output "Downloading Levain JAR from $JarUri"
Invoke-WebRequest $JarUri -OutFile $TempLevainJar -UseBasicParsing

Write-Output ""
Write-Output "Installing Levain..."
& $javaExe -jar $TempLevainJar --levainHome "$levainHome" --addRepo "$levainRepo" install levain

Write-Output ""
Write-Output "=== Levain installed successfully at $levainHome"
Write-Output ""
Write-Output "Add to your PATH or restart your terminal to use levain"
Write-Output ""

if (Test-Path $TempLevain) {
  Write-Output ""
  Write-Output "Removing $TempLevain"
  Remove-Item $TempLevain -Recurse -Force
}
