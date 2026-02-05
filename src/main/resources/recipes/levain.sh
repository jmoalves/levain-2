#!/bin/bash
# Levain 2 wrapper script
# This script invokes the levain.jar from the current directory

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/levain.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: levain.jar not found at $JAR_FILE"
    exit 1
fi

exec java -jar "$JAR_FILE" "$@"
