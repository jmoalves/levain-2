# Levain 2

_Something to help you make your software grow_

[![Build Status](https://github.com/jmoalves/levain-2/actions/workflows/maven.yml/badge.svg)](https://github.com/jmoalves/levain-2/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/jmoalves/levain-2/branch/main/graph/badge.svg)](https://codecov.io/gh/jmoalves/levain-2)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Java standalone console application for development environment installation and recipe management. This is a drop-in replacement for the original [levain](https://github.com/jmoalves/levain) and supports recipes from the [levain-pkgs](https://github.com/jmoalves/levain-pkgs) repository.

## Features

- **Cross-Platform**: Runs on Windows and Linux
- **Multiple Repository Sources**:
  - Built-in recipes from JAR resources (ResourceRepository)
  - Local directory repositories (DirectoryRepository)
  - Git repositories with automatic cloning (GitRepository)
  - ZIP archives with extraction (ZipRepository)
  - Remote HTTP/HTTPS repositories (RemoteRepository)
- **Recipe-Based Installation**: YAML-based recipe format with dependency resolution
- **Package Registry**: Tracks installed packages with automatic storage
- **Configuration Management**: Persistent configuration with repository management
- **Comprehensive Testing**: JUnit 5, Cucumber BDD, and Mockito with code coverage tracking
- **CLI Commands**: list, install, shell, and config management

## Installation Options

### Option 1: Using the Standalone JAR

Download the standalone JAR and run it with your favorite JVM:

```bash
java -jar levain.jar [command] [options]
```

### Option 2: Using Native Executables

Download the native executable for your platform:

**Windows:**
```cmd
levain.exe [command] [options]
```

**Linux:**
```bash
./levain [command] [options]
```

## Documentation

Start here based on your role:

### For Users

👉 **[User Guide](docs/USER_GUIDE.md)** - Getting started, installation, common tasks, configuration, and troubleshooting

### For Developers

👉 **[Developer Guide](docs/DEVELOPMENT.md)** - Building from source, architecture, implementation details, testing, and debugging

### Additional Resources

- **[Installation](docs/installation/)** - Detailed installation procedures
- **[Build & Releases](docs/build/)** - Building and releasing the application
- **[Configuration](docs/configuration/)** - Configuration implementation details
- **[Registry](docs/registry/)** - Package registry implementation
- **[Setup & Integration](docs/setup/)** - Nexus repository setup and integration
- **[Implementation](docs/implementation/)** - Implementation details and analysis

## Building from Source

### Prerequisites

- JDK 25 (Java 25)
- Maven 3.9 or later
- (Optional) GraalVM for native executable builds

### Build Commands

**Clean build and run all tests:**
```bash
mvn clean test
```

**Build with code coverage analysis:**
```bash
mvn clean verify
```

The coverage report will be generated at `target/site/jacoco/index.html`.

**Package the application (JAR file):**
```bash
mvn clean package
```

This creates:
- `target/levain-2.0.0-SNAPSHOT.jar` - Regular JAR with dependencies
- `target/levain.jar` - Standalone executable JAR

**Build native executable (requires GraalVM):**
```bash
mvn clean package -Pnative
```

This creates platform-specific native executables in the `target/` directory.

### Run Tests

**All tests (unit + integration + Cucumber):**
```bash
mvn test
```

**Only unit tests:**
```bash
mvn test -Dtest=*Test
```

**Only Cucumber acceptance tests:**
```bash
mvn test -Dtest=RunCucumberTest
```

**With coverage report:**
```bash
mvn test jacoco:report
```

## Usage

For detailed usage examples and common workflows, see [User Guide - Getting Started](docs/USER_GUIDE.md#getting-started)

### Available Commands

**List recipes:**
```bash
levain list                  # List all available recipes with installation status
levain list jdk              # Filter recipes by pattern
levain list --installed      # Show only installed recipes
levain list --available      # Show only recipes that are not installed
levain list --source         # Show the source repository for each recipe
```

Note: source information is tracked for new installations. Existing installs
from older versions will show `unknown` until reinstalled.

**Install packages:**
```bash
levain install jdk-21 maven git
```

**Open shell:**
```bash
levain shell
```

**Manage repositories:**
```bash
levain config repo add dir:/path/to/recipes local
levain config repo list
```

### Global Options

```bash
levain --help          # Show help
levain --version       # Show version
levain -v [command]    # Verbose output
```

For full command reference, see [User Guide](docs/USER_GUIDE.md).

## License

See [LICENSE](LICENSE) file for details.

## Roadmap

For planned features and development priorities, see [ROADMAP.md](ROADMAP.md).

## Contributing

For contributing guidelines and developer setup, see [Developer Guide](docs/DEVELOPMENT.md#contributing).

## Support & Contact

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/jmoalves/levain-2).
