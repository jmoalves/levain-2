#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 --mode stable|nightly --current-tag TAG --output FILE [--previous-tag TAG] [--release-dir DIR] [--repo OWNER/REPO]"
}

MODE=""
CURRENT_TAG=""
PREVIOUS_TAG=""
OUTPUT=""
RELEASE_DIR=""
REPO=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="$2"
      shift 2
      ;;
    --current-tag)
      CURRENT_TAG="$2"
      shift 2
      ;;
    --previous-tag)
      PREVIOUS_TAG="$2"
      shift 2
      ;;
    --output)
      OUTPUT="$2"
      shift 2
      ;;
    --release-dir)
      RELEASE_DIR="$2"
      shift 2
      ;;
    --repo)
      REPO="$2"
      shift 2
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

if [[ -z "$MODE" || -z "$CURRENT_TAG" || -z "$OUTPUT" ]]; then
  usage
  exit 1
fi

get_previous_official_release() {
  python - "$REPO" "$CURRENT_TAG" <<'PY'
import json
import os
import sys
import urllib.request

repo = sys.argv[1]
current_tag = sys.argv[2]

if not repo:
  sys.exit(0)

url = f"https://api.github.com/repos/{repo}/releases?per_page=100"
headers = {"User-Agent": "levain-release-notes"}
token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
if token:
  headers["Authorization"] = f"Bearer {token}"

req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req) as resp:
  releases = json.load(resp)

official = [
  r for r in releases
  if not r.get("prerelease") and str(r.get("tag_name", "")).startswith("v")
]

official.sort(key=lambda r: r.get("published_at") or "", reverse=True)

current_seen = False
previous = None

for release in official:
  tag = release.get("tag_name", "")
  if tag == current_tag:
    current_seen = True
    continue
  if current_seen:
    previous = tag
    break

if not current_seen and official:
  previous = official[0].get("tag_name", "")

if previous:
  print(previous)
PY
}

if [[ -z "$PREVIOUS_TAG" ]]; then
  if [[ "$MODE" == "nightly" ]]; then
    mapfile -t tags < <(git tag -l 'nightly-*' --sort=version:refname)
  else
    if [[ -n "$REPO" ]]; then
      PREVIOUS_TAG=$(get_previous_official_release || true)
    fi
    if [[ -z "$PREVIOUS_TAG" ]]; then
      mapfile -t tags < <(git tag -l 'v*' --sort=version:refname)
    fi
  fi

  if [[ -z "$PREVIOUS_TAG" && ${#tags[@]} -gt 0 ]]; then
    for ((i=${#tags[@]}-1; i>=0; i--)); do
      if [[ "${tags[$i]}" == "$CURRENT_TAG" ]]; then
        if (( i > 0 )); then
          PREVIOUS_TAG="${tags[$i-1]}"
        fi
        break
      fi
    done
  fi
fi

range=""
if [[ -n "$PREVIOUS_TAG" ]]; then
  range="$PREVIOUS_TAG..$CURRENT_TAG"
fi

{
  echo "# Release Notes"
  echo ""
  echo "- Tag: $CURRENT_TAG"
  if [[ -n "$PREVIOUS_TAG" ]]; then
    echo "- Previous: $PREVIOUS_TAG"
  fi
  echo "- Generated: $(date -u +'%Y-%m-%d %H:%M:%SZ')"
  echo ""
  echo "## Changes"
  echo ""
  if [[ -n "$range" ]]; then
    git log "$range" --pretty=format:"- %h %s (%an)" || true
  else
    git log -n 50 --pretty=format:"- %h %s (%an)" || true
  fi
  echo ""

  if [[ -n "$RELEASE_DIR" && -d "$RELEASE_DIR" ]]; then
    echo "## Assets"
    echo ""
    while IFS= read -r file; do
      echo "- $file"
    done < <(find "$RELEASE_DIR" -maxdepth 1 -type f -printf '%f\n' | sort)
    echo ""
  fi
} > "$OUTPUT"
