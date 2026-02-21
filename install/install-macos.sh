#!/usr/bin/env bash
set -euo pipefail

REPO="jmoalves/levain-2"
TAG=""
MODE="binary"
INSTALL_DIR="$HOME/levain"
ADD_TO_PATH="false"

usage() {
  echo "Usage: $0 [--tag TAG] [--repo OWNER/REPO] [--install-dir DIR] [--jar|--binary] [--add-to-path]"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)
      TAG="$2"
      shift 2
      ;;
    --repo)
      REPO="$2"
      shift 2
      ;;
    --install-dir)
      INSTALL_DIR="$2"
      shift 2
      ;;
    --jar)
      MODE="jar"
      shift
      ;;
    --binary)
      MODE="binary"
      shift
      ;;
    --add-to-path)
      ADD_TO_PATH="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
 done

get_latest_nightly_tag() {
  python - "$REPO" <<'PY'
import json
import sys
import urllib.request

repo = sys.argv[1]
url = f"https://api.github.com/repos/{repo}/releases?per_page=100"
req = urllib.request.Request(url, headers={"User-Agent": "levain-install"})
with urllib.request.urlopen(req) as resp:
    releases = json.load(resp)
nightlies = [r for r in releases if r.get("prerelease") and str(r.get("tag_name", "")).startswith("nightly-")]
if not nightlies:
    raise SystemExit("No nightly releases found")
nightlies.sort(key=lambda r: r.get("published_at") or "")
print(nightlies[-1].get("tag_name"))
PY
}

if [[ -z "$TAG" ]]; then
  TAG=$(get_latest_nightly_tag)
fi

mkdir -p "$INSTALL_DIR"

if [[ "$MODE" == "jar" ]]; then
  ASSET="levain.jar"
  DEST="$INSTALL_DIR/levain.jar"
else
  ASSET="levain-macos-arm64"
  DEST="$INSTALL_DIR/levain"
fi

URL="https://github.com/$REPO/releases/download/$TAG/$ASSET"

echo "Downloading $ASSET from $URL"
curl -fsSL -o "$DEST" "$URL"

if [[ "$MODE" == "jar" ]]; then
  cat > "$INSTALL_DIR/levain" <<'EOF'
#!/usr/bin/env bash
exec java -jar "$(dirname "$0")/levain.jar" "$@"
EOF
  chmod +x "$INSTALL_DIR/levain"
else
  chmod +x "$DEST"
fi

if [[ "$ADD_TO_PATH" == "true" ]]; then
  if [[ -n "${GITHUB_PATH:-}" ]]; then
    echo "$INSTALL_DIR" >> "$GITHUB_PATH"
  fi
  export PATH="$INSTALL_DIR:$PATH"
fi

echo "Installed Levain ($MODE) to $INSTALL_DIR using tag $TAG"
