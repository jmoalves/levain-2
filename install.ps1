#!/usr/bin/env pwsh
# Inspired by https://deno.land/x/install@v0.1.4/install.ps1
# Bootstrap script for Levain 2
#
# This script attempts to download and install Levain 2 in one of three ways:
# 1. Native executable (recommended, no dependencies)
# 2. JAR version (requires Java)
# 3. Build from source (requires Maven and Java)
#
# TODO(everyone): Keep this script simple and easily auditable.
#
# Examples:
# iwr https://github.com/jmoalves/levain-2/releases/latest/download/install.ps1 | iex
# $levainVersion="2.0.0";iwr https://github.com/jmoalves/levain-2/releases/latest/download/install.ps1 | iex
# $levainHome="C:\dev-env";iwr https://github.com/jmoalves/levain-2/releases/latest/download/install.ps1 | iex
# $levainUseJar=$true;iwr https://github.com/jmoalves/levain-2/releases/latest/download/install.ps1 | iex

$ErrorActionPreference = 'Stop'

### Parameters
# $levainHome - Optional
# $levainVersion - Optional
# $levainHeadless - Optional
# $levainUrlBase - Optional
# $levainRepo - Optional
# $levainUseJar - Optional (force JAR version)
#

if (! $levainHome) {
  $levainHome = "$HOME\levain"
}

if (! $levainUrlBase) {
  $levainUrlBase = "https://github.com/jmoalves/levain-2"
}

if (! $levainRepo) {
  $levainRepo = "https://github.com/jmoalves/levain-pkgs.git"
}

#
###

# GitHub requires TLS 1.2
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$TempLevain = Join-Path $Env:Temp "levain-install-$([guid]::NewGuid())"

#######################################################
# Preparation
#

if (! $levainHeadless) {
  Clear-Host
}

if ($levainVersion) {
  Write-Output "=== Installing Levain 2 $levainVersion at $levainHome"
} else {
  Write-Output "=== Installing latest Levain 2 release at $levainHome"
}

# Detect platform
$arch = if ([Environment]::Is64BitProcess) { "x86_64" } else { "i686" }
$platform = "windows-$arch"

Write-Output ""
Write-Output "Detected platform: $platform"

if (! $levainUseJar) {
  # Try native executable first
  Write-Output ""
  Write-Output "Attempting to download native executable..."
  
  $NativeUri = if ($levainVersion) {
    "${levainUrlBase}/releases/download/v${levainVersion}/levain-${platform}.exe"
  } else {
    "${levainUrlBase}/releases/latest/download/levain-${platform}.exe"
  }
  
  try {
    $headRequest = [System.Net.HttpWebRequest]::Create($NativeUri)
    $headRequest.Method = "HEAD"
    $headResponse = $headRequest.GetResponse()
    $headResponse.Close()
    
    Write-Output "Downloading native Levain from $NativeUri"
    
    if (!(Test-Path $TempLevain)) {
      New-Item $TempLevain -ItemType Directory | Out-Null
    }
    
    $NativeExe = "$TempLevain\levain.exe"
    Invoke-WebRequest $NativeUri -OutFile $NativeExe -UseBasicParsing
    
    Write-Output ""
    Write-Output "Installing Levain..."
    & $NativeExe --levainHome "$levainHome" --addRepo "$levainRepo" install levain
    
    Write-Output ""
    Write-Output "=== Levain installed successfully at $levainHome"
    Write-Output ""
    Write-Output "Add to your PATH or restart your terminal to use levain"
    
    exit 0
  } catch {
    Write-Output "Native executable not available for $platform"
    Write-Output "Falling back to JAR version..."
  }
}

# Use JAR version
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
  Write-Output "Levain 2 JAR version requires Java 25 or later."
  Write-Output ""
  Write-Output "Options:"
  Write-Output "1. Install Java:"
  Write-Output "   - Download from https://adoptium.net/"
  Write-Output "   - Or: choco install openjdk"
  Write-Output ""
  Write-Output "2. Wait for native executable release (no Java required)"
  Write-Output ""
  Write-Output "3. Build from source:"
  Write-Output "   - git clone https://github.com/jmoalves/levain-2.git"
  Write-Output "   - cd levain-2"
  Write-Output "   - mvn clean package"
  Write-Output "   - java -jar target\levain.jar install levain"
  exit 1
}

#######################################################
# Download JAR
#

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

#######################################################
# Levain install
#

Write-Output ""
Write-Output "Installing Levain..."
& $javaExe -jar $TempLevainJar --levainHome "$levainHome" --addRepo "$levainRepo" install levain

Write-Output ""
Write-Output "=== Levain installed successfully at $levainHome"
Write-Output ""
Write-Output "Add to your PATH or restart your terminal to use levain"
Write-Output ""

#######################################################
# Cleanup
#

if (Test-Path $TempLevain) {
  Write-Output ""
  Write-Output "Removing $TempLevain"
  Remove-Item $TempLevain -Recurse -Force
}
