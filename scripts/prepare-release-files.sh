#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/prepare-release-files.sh [options]

Options:
  --version <value>       Release version suffix for levain-<version>.jar
  --jar-path <path>       Source JAR path (default: target/levain.jar)
  --release-dir <path>    Output directory (default: release)
  --install-dir <path>    Installer scripts directory (default: install)
  -h, --help              Show this help message

Notes:
  - If --version is omitted, the script extracts it from pom.xml.
  - Installer scripts are copied when present.
EOF
}

version=""
jar_path="target/levain.jar"
release_dir="release"
install_dir="install"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      version="${2:-}"
      shift 2
      ;;
    --jar-path)
      jar_path="${2:-}"
      shift 2
      ;;
    --release-dir)
      release_dir="${2:-}"
      shift 2
      ;;
    --install-dir)
      install_dir="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$version" ]]; then
  version="$(grep '<version>' pom.xml | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')"
fi

mkdir -p "$release_dir"

if [[ -f "$jar_path" ]]; then
  cp "$jar_path" "$release_dir/levain.jar"
  cp "$jar_path" "$release_dir/levain-${version}.jar"
fi

if [[ -f "$install_dir/install-windows.ps1" ]]; then
  cp "$install_dir/install-windows.ps1" "$release_dir/install-windows.ps1"
fi

if [[ -f "$install_dir/install-linux.sh" ]]; then
  cp "$install_dir/install-linux.sh" "$release_dir/install-linux.sh"
  chmod +x "$release_dir/install-linux.sh"
fi

if [[ -f "$install_dir/install-macos.sh" ]]; then
  cp "$install_dir/install-macos.sh" "$release_dir/install-macos.sh"
  chmod +x "$release_dir/install-macos.sh"
fi

ls -la "$release_dir/"
