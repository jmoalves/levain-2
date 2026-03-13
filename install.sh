#!/bin/bash

# Bootstrap script for Levain 2 (JAR distribution)
#
# This script downloads the Levain JAR and installs it.

set -e

: "${levainHome:=$HOME/levain}"
: "${levainVersion:=}"
: "${levainUrlBase:=https://github.com/jmoalves/levain-2}"
: "${levainRepo:=https://github.com/jmoalves/levain-pkgs.git}"

detect_platform() {
  local os=$(uname -s)
  case "$os" in
    Linux*|Darwin*|MINGW*|MSYS*|CYGWIN*)
      return 0
      ;;
    *)
      echo "ERROR: Unsupported OS: $os"
      return 1
      ;;
  esac
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

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

detect_platform

echo "Checking for Java..."

if ! command_exists java; then
  echo "ERROR: Java is not installed"
  echo ""
  echo "Levain 2 requires Java 25 or later."
  echo ""
  echo "Build from source alternative:"
  echo "  git clone https://github.com/jmoalves/levain-2.git"
  echo "  cd levain-2"
  echo "  mvn clean package"
  echo "  java -jar target/levain.jar install levain"
  exit 1
fi

javaVersion=$(java -version 2>&1 | head -1)
echo "Found: $javaVersion"

if [ -z "$levainVersion" ]; then
  jarUri="${levainUrlBase}/releases/latest/download/levain.jar"
else
  jarUri="${levainUrlBase}/releases/download/v${levainVersion}/levain.jar"
fi

echo ""
echo "Downloading Levain JAR from $jarUri"
curl -fsSL -o "$tempLevain/levain.jar" "$jarUri"

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
