#!/bin/bash

# Inspired by https://deno.land/x/install@v0.1.4/install.ps1
# Bootstrap script for Levain 2
#
# This script attempts to download and install Levain 2 in one of three ways:
# 1. Native executable (recommended, no dependencies)
# 2. JAR version (requires Java)
# 3. Build from source (requires Maven and Java)
#
# Examples:
# curl https://github.com/jmoalves/levain-2/releases/latest/download/install.sh | bash
# levainVersion="2.0.0" curl https://github.com/jmoalves/levain-2/releases/latest/download/install.sh | bash
# levainHome="$HOME/.local/levain" curl https://github.com/jmoalves/levain-2/releases/latest/download/install.sh | bash
# levainUseJar=true curl https://github.com/jmoalves/levain-2/releases/latest/download/install.sh | bash

set -e

# Parameters
: "${levainHome:=$HOME/levain}"
: "${levainVersion:=}"
: "${levainUrlBase:=https://github.com/jmoalves/levain-2}"
: "${levainRepo:=https://github.com/jmoalves/levain-pkgs.git}"
: "${levainUseJar:=false}"

# Detect OS and Architecture
detect_platform() {
  local os=$(uname -s)
  local arch=$(uname -m)
  
  case "$os" in
    Linux*)
      os="linux"
      ;;
    Darwin*)
      os="macos"
      ;;
    MINGW*|MSYS*|CYGWIN*)
      os="windows"
      ;;
    *)
      echo "ERROR: Unsupported OS: $os"
      return 1
      ;;
  esac
  
  case "$arch" in
    x86_64|amd64)
      arch="x86_64"
      ;;
    aarch64|arm64)
      arch="aarch64"
      ;;
    *)
      echo "ERROR: Unsupported architecture: $arch"
      return 1
      ;;
  esac
  
  echo "${os}-${arch}"
}

# Check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Temp directory for installation
tempLevain="/tmp/levain-install-$$"
mkdir -p "$tempLevain"

cleanup() {
  if [ -d "$tempLevain" ]; then
    rm -rf "$tempLevain"
  fi
}

trap cleanup EXIT

echo ""
echo "=== Installing Levain 2 at $levainHome"
echo ""

# Determine which format to download
platform=$(detect_platform)
echo "Detected platform: $platform"

if [ "$levainUseJar" != "true" ]; then
  # Try native executable first
  echo ""
  echo "Attempting to download native executable..."
  
  if [ -z "$levainVersion" ]; then
    nativeUri="${levainUrlBase}/releases/latest/download/levain-${platform}"
  else
    nativeUri="${levainUrlBase}/releases/download/v${levainVersion}/levain-${platform}"
  fi
  
  if curl -fsSL --output /dev/null --head "$nativeUri" 2>/dev/null; then
    echo "Downloading native Levain from $nativeUri"
    curl -fsSL -o "$tempLevain/levain" "$nativeUri"
    chmod +x "$tempLevain/levain"
    
    echo "Installing Levain..."
    "$tempLevain/levain" \
      --levainHome "$levainHome" \
      --addRepo "$levainRepo" \
      install levain
    
    echo ""
    echo "=== Levain installed successfully at $levainHome"
    echo ""
    echo "Add to your PATH:"
    echo "  export PATH=\"$levainHome/levain:\$PATH\""
    echo ""
    exit 0
  fi
  
  echo "Native executable not available for $platform"
  echo "Falling back to JAR version..."
fi

# Use JAR version
echo ""
echo "Checking for Java..."

if ! command_exists java; then
  echo "ERROR: Java is not installed"
  echo ""
  echo "Levain 2 JAR version requires Java 25 or later."
  echo ""
  echo "Options:"
  echo "1. Install Java:"
  echo "   - Ubuntu/Debian: sudo apt-get install default-jre"
  echo "   - macOS: brew install openjdk"
  echo "   - Windows: https://adoptium.net/"
  echo ""
  echo "2. Wait for native executable release (no Java required)"
  echo ""
  echo "3. Build from source:"
  echo "   - git clone https://github.com/jmoalves/levain-2.git"
  echo "   - cd levain-2"
  echo "   - mvn clean package"
  echo "   - java -jar target/levain.jar install levain"
  exit 1
fi

javaVersion=$(java -version 2>&1 | head -1)
echo "Found: $javaVersion"

# Download JAR
if [ -z "$levainVersion" ]; then
  jarUri="${levainUrlBase}/releases/latest/download/levain.jar"
else
  jarUri="${levainUrlBase}/releases/download/v${levainVersion}/levain.jar"
fi

echo ""
echo "Downloading Levain JAR from $jarUri"
curl -fsSL -o "$tempLevain/levain.jar" "$jarUri"

# Install using the downloaded JAR
echo ""
echo "Installing Levain..."
java -jar "$tempLevain/levain.jar" \
  --levainHome "$levainHome" \
  --addRepo "$levainRepo" \
  install levain

echo ""
echo "=== Levain installed successfully at $levainHome"
echo ""
echo "Add to your PATH:"
echo "  export PATH=\"$levainHome/levain:\$PATH\""
echo ""
