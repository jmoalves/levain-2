# Development Guide

This guide is for developers who want to contribute to or understand the internals of Levain 2.

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

**Build native executable (requires GraalVM):**
```bash
mvn clean package -Pnative
```

See [Build & Releases](build/BUILD_RELEASES.md) for more details.

## Project Architecture

### Core Components

- **CLI Layer** (`cli/`) - Command-line interface using Picocli
- **Services** (`service/`) - Business logic for recipes, installation, configuration
- **Repositories** (`repository/`) - Recipe sources (local, git, remote, zip, resources)
- **Models** (`model/`) - Data structures (Recipe, Config, etc.)

### Key Concepts

- **Recipe**: YAML file defining a package and its commands
- **Repository**: Source for recipes (can be local directory, git repo, HTTP URL, ZIP file)
- **Registry**: Local storage of installed packages
- **Configuration**: User settings and repository management

## Implementation Details

### Configuration System

See [Configuration Implementation](configuration/CONFIG_IMPLEMENTATION.md) for details on how configuration is managed.

### Registry System

See [Registry Implementation](registry/REGISTRY_IMPLEMENTATION.md) for details on how installed packages are tracked.

### Repository Architecture

Multiple repository types support different sources:
- `ResourceRepository` - Built-in recipes in the JAR
- `DirectoryRepository` - Local directory of recipes
- `GitRepository` - Git repositories (auto-cloned)
- `ZipRepository` - ZIP archives (auto-extracted)
- `RemoteRepository` - HTTP/HTTPS recipe sources

## Testing

### Running Tests

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

### Test Coverage

The project maintains comprehensive test coverage with:
- **Unit Tests**: Component-level tests with mocking
- **Integration Tests**: Service and repository integration scenarios
- **BDD Tests**: Cucumber acceptance tests for end-to-end workflows

See coverage report at `target/site/jacoco/index.html` after running `mvn verify`.

## Code Analysis

### Repository Analysis

See [Additional Repositories Analysis](implementation/ADDITIONAL_REPOSITORIES_ANALYSIS.md) for technical details about different repository implementations.

### Implementation Summary

See [Implementation Summary](implementation/IMPLEMENTATION_SUMMARY.md) for an overview of the system architecture.

## Contributing

Before submitting changes:

1. Ensure all tests pass: `mvn clean verify`
2. Follow the project's code style
3. Add tests for new functionality
4. Update documentation as needed

## Debugging

Set system properties for debug output:

```bash
mvn test -Dlevain.debug=true
```

Check logs in `logs/levain.log` when running the application.

