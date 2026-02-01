#!/bin/bash
# Setup Docker contexts for Levain 2 development
# This script creates the 'levain' Docker context for remote development
#
# Usage:
#   ./setup-docker-contexts.sh [HOSTNAME]
#
# Examples:
#   ./setup-docker-contexts.sh                    # Uses default: astronauta.biruta.net
#   ./setup-docker-contexts.sh myserver.com
#   ./setup-docker-contexts.sh 192.168.1.100

set -e

CONTEXT_NAME="levain"
CONTEXT_HOST="${1:-astronauta.biruta.net}"
CURRENT_USER=$(whoami)

echo "Setting up Docker context: $CONTEXT_NAME"
echo "Using SSH host: $CURRENT_USER@$CONTEXT_HOST"

# Check if context already exists
if docker context list | grep -q "^$CONTEXT_NAME"; then
    echo "Context '$CONTEXT_NAME' already exists. Updating..."
    docker context update "$CONTEXT_NAME" --docker "host=ssh://$CURRENT_USER@$CONTEXT_HOST"
else
    echo "Creating new context '$CONTEXT_NAME'..."
    docker context create "$CONTEXT_NAME" --docker "host=ssh://$CURRENT_USER@$CONTEXT_HOST"
fi

echo "✓ Docker context '$CONTEXT_NAME' configured successfully"
echo ""
echo "To use this context in devcontainer, switch contexts manually with:"
echo "  docker context use $CONTEXT_NAME"


