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
  local tags_json
  local latest_tag

  tags_json="$(curl -fsSL -H "User-Agent: levain-install" "https://api.github.com/repos/${REPO}/tags?per_page=100")"

  latest_tag="$(printf '%s\n' "$tags_json" \
    | grep -oE '"name"[[:space:]]*:[[:space:]]*"nightly-[^"]+"' \
    | sed -E 's/.*"(nightly-[^"]+)"/\1/' \
    | sort -V \
    | tail -n 1 || true)"

  if [[ -z "$latest_tag" ]]; then
    echo "No nightly releases found for ${REPO}" >&2
    return 1
  fi

  printf '%s\n' "$latest_tag"
}

if [[ -z "$TAG" ]]; then
  TAG=$(get_latest_nightly_tag)
fi

mkdir -p "$INSTALL_DIR"

if [[ "$MODE" == "jar" ]]; then
  ASSET="levain.jar"
  DEST="$INSTALL_DIR/levain.jar"
else
  ASSET="levain-linux-x64"
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
