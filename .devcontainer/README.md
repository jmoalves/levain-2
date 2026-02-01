# Levain 2 Dev Container

This directory contains the configuration for the VS Code Dev Container for Levain 2 development.

## Features

- **Java 25**: Latest Java LTS version with experimental features support
- **Maven**: Pre-configured with ByteBuddy experimental mode
- **VS Code Extensions**: 
  - Java Extension Pack
  - Maven for Java
  - Test Runner for Java
  - Debugger for Java
  - XML support
  - GitHub Copilot (if available)
  - Coverage Gutters
  - SonarLint

## Getting Started

1. Install [Docker](https://www.docker.com/products/docker-desktop)
2. Install [VS Code](https://code.visualstudio.com/)
3. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
4. Open this project in VS Code
5. When prompted, click "Reopen in Container" or run the command "Dev Containers: Reopen in Container"

## Docker Context

This devcontainer is configured to automatically use the **default/local Docker context** for portability.

### Using Remote Docker

If you need to use a remote Docker daemon, run the setup script:

```bash
# With default host (astronauta.biruta.net)
.devcontainer/setup-docker-contexts.sh

# With custom host
.devcontainer/setup-docker-contexts.sh myserver.com
.devcontainer/setup-docker-contexts.sh 192.168.1.100
```

This creates the `levain` Docker context using your current username. Then, before opening the devcontainer, switch to it:

```bash
docker context use levain
```

The devcontainer will automatically detect and use the currently active Docker context.

## What's Configured

- **Java 25** with experimental ByteBuddy support
- **Maven** with local `.m2` repository mounted for faster builds
- **Environment variables**: `MAVEN_OPTS=-Dnet.bytebuddy.experimental=true`
- **Post-create command**: Runs `mvn clean install -DskipTests` to download dependencies
- **Port forwarding**: Port 8080 for potential web services

## Building and Testing

Once inside the container, you can run:

```bash
# Build the project
mvn clean package

# Run tests
mvn test

# Run with coverage
mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

## Troubleshooting

If you encounter issues:

1. **Rebuild the container**: Command Palette → "Dev Containers: Rebuild Container"
2. **Check Java version**: Run `java -version` in the container terminal
3. **Verify Maven**: Run `mvn -version` in the container terminal
