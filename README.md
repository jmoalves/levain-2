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
- `target/levain-standalone-2.0.0-SNAPSHOT.jar` - Standalone executable JAR

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

## Documentation

Additional documentation is available in the [docs](docs/) directory:

- **[Installation](docs/installation/)** - How to install and set up Levain 2
- **[Build & Releases](docs/build/)** - Building and releasing the application
- **[Configuration](docs/configuration/)** - Configuration implementation details
- **[Registry](docs/registry/)** - Package registry implementation
- **[Setup & Integration](docs/setup/)** - Nexus repository setup and integration
- **[Implementation](docs/implementation/)** - Implementation details and analysis

## Usage

### Available Commands

Levain 2 provides the following commands:

#### List Recipes

```bash
# List all available recipes
levain list

# Filter recipes by name (case-sensitive)
levain list jdk
levain list maven
```

#### Install Packages

```bash
# Install a single package
levain install jdk-21

# Install multiple packages
levain install jdk-21 git maven

# Force reinstall an already-installed package
levain install --force jdk-21
```

#### Open Shell

```bash
# Open a new shell
levain shell

# Open shell with specific packages in environment
levain shell jdk-21 maven
```

#### Manage Configuration

```bash
# Add a recipe repository
levain config add-repo local dir:/path/to/recipes
levain config add-repo github https://github.com/user/recipes

# List configured repositories
levain config list-repo

# Remove a repository by name
levain config remove-repo local
```

### Global Options

```bash
# Show help
levain --help

# Show version
levain --version

# Verbose output
levain -v [command]

# Set custom levain home directory
levain --levainHome /custom/path [command]

# Set custom cache directory
levain --levainCache /custom/cache [command]
```

## Project Structure

```
levain-2/
├── src/
│   ├── main/
│   │   ├── java/com/github/jmoalves/levain/
│   │   │   ├── Levain.java                      # Main entry point
│   │   │   ├── cli/
│   │   │   │   ├── LevainCommand.java           # Root CLI command
│   │   │   │   ├── CdiCommandFactory.java       # CDI bean factory for Picocli
│   │   │   │   └── commands/
│   │   │   │       ├── ConfigCommand.java       # Repository configuration
│   │   │   │       ├── InstallCommand.java      # Package installation
│   │   │   │       ├── ListCommand.java         # Recipe listing
│   │   │   │       └── ShellCommand.java        # Shell integration
│   │   │   ├── config/
│   │   │   │   └── Config.java                  # Configuration management
│   │   │   ├── model/
│   │   │   │   ├── Recipe.java                  # Recipe data model
│   │   │   │   └── RecipeTree.java              # Recipe dependency tree
│   │   │   ├── service/
│   │   │   │   ├── ConfigService.java           # Config persistence
│   │   │   │   ├── RecipeService.java           # Recipe orchestration
│   │   │   │   ├── RecipeLoader.java            # YAML parsing
│   │   │   │   ├── InstallService.java          # Installation logic
│   │   │   │   └── ShellService.java            # Shell management
│   │   │   └── repository/
│   │   │       ├── Repository.java              # Repository interface
│   │   │       ├── RepositoryManager.java       # Multi-source orchestration
│   │   │       ├── RepositoryFactory.java       # Factory for repo creation
│   │   │       ├── Registry.java                # Installed packages registry
│   │   │       ├── ResourceRepository.java      # Built-in recipes from JAR
│   │   │       ├── DirectoryRepository.java     # Local filesystem recipes
│   │   │       ├── GitRepository.java           # Git repository support
│   │   │       ├── ZipRepository.java           # ZIP archive support
│   │   │       └── RemoteRepository.java        # HTTP/HTTPS repositories
│   │   └── resources/
│   │       ├── log4j2.xml                       # Logging configuration
│   │       └── recipes/                         # Built-in recipes
│   └── test/
│       ├── java/com/github/jmoalves/levain/
│       │   ├── cli/                             # CLI command tests
│       │   ├── config/                          # Configuration tests
│       │   ├── service/                         # Service layer tests
│       │   ├── model/                           # Model tests
│       │   ├── repository/                      # Repository tests
│       │   └── cucumber/                        # BDD scenarios
│       └── resources/
│           ├── features/                        # Cucumber feature files
│           └── recipes/                         # Test recipes
├── pom.xml                                      # Maven configuration
├── codecov.yml                                  # Coverage configuration
├── README.md                                    # This file
└── LICENSE                                      # MIT License
```

### Key Components

**CLI Layer (`cli/`)**:
- Picocli-based command-line interface
- Hierarchical command structure
- CDI dependency injection for commands
- Global options (help, version, verbose)

**Service Layer (`service/`)**:
- `RecipeService`: Orchestrates recipe loading and dependency resolution
- `InstallService`: Handles package installation workflow
- `ConfigService`: Manages persistent configuration
- `RecipeLoader`: YAML parsing and recipe creation

**Repository Layer (`repository/`)**:
- `RepositoryManager`: Chains multiple repositories, deduplicates recipes
- `Registry`: Tracks installed packages in local cache
- Multiple source types: Resources, Directory, Git, ZIP, HTTP

**Model Layer (`model/`)**:
- `Recipe`: YAML recipe structure with commands and dependencies
- `RecipeTree`: Dependency resolution and ordering

## Testing

### Test Coverage

The project maintains comprehensive test coverage with the following test categories:

- **Unit Tests**: Component-level tests with mocking
- **Integration Tests**: Service and repository integration scenarios
- **BDD Tests**: Cucumber acceptance tests for end-to-end workflows

### Test Frameworks

- **JUnit 5** (v5.11.4): Modern unit testing with assertions and annotations
- **Mockito** (v5.15.2): Mocking, stubbing, and verification
- **Cucumber** (v7.21.0): BDD scenarios and step definitions
- **JaCoCo** (v0.8.13): Code coverage measurement

### Running Tests

```bash
# All tests (unit + integration + BDD)
mvn clean test

# Generate coverage report
mvn clean test jacoco:report
# Report generated at: target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=RecipeServiceTest

# Run with specific pattern
mvn test -Dtest=*RepositoryTest

# Cucumber acceptance tests only
mvn test -Dtest=RunCucumberTest
```

### Test Coverage Areas

**Service Layer** (110+ tests):
- RecipeServiceTest: 11 tests for recipe loading and filtering
- ConfigServiceTest: 18 tests for configuration persistence
- InstallServiceTest: 12 tests for installation scenarios
- RecipeLoaderTest: 10 tests for YAML parsing
- ShellServiceTest: 3 tests for shell integration

**Repository Layer** (100+ tests):
- DirectoryRepositoryTest: 13 tests for filesystem operations
- RepositoryManagerTest: 12 tests for multi-source orchestration
- RegistryTest: 25 tests for installed package tracking
- GitRepositoryTest: 7 tests with local git repository
- ZipRepositoryTest: 7 tests with ZIP extraction
- RemoteRepositoryTest: 6 tests with HTTP mocking
- ResourceRepositoryTest: 6 tests for built-in recipes

**CLI Commands** (30+ tests):
- ConfigCommandTest: 11 tests for repository management
- InstallCommandTest: 7 tests for package installation
- ShellCommandTest: 3 tests for shell launching
- ListCommandTest: 5 tests for recipe listing

**Model & Config** (15+ tests):
- RecipeTest: 2 tests for recipe model
- ConfigTest: 13 tests for configuration management
- AbstractRepositoryTest: 4 tests for base functionality

**BDD Acceptance Tests** (2 feature files):
- install_packages.feature: Installation workflows
- list_recipes.feature: Recipe discovery and filtering

### Coverage Goals

Current: ~78% (Line/Branch)
Target: 95% (Line/Branch)

Focus areas for improvement:
- Entry point coverage (Levain, LevainCommand)
- Complex repository logic (Git/ZIP extraction)
- Error handling and edge cases
- Shell integration

## Development

### IDE Setup

Supported IDEs:
- **IntelliJ IDEA**: File → Open → pom.xml
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Install Extension Pack for Java

### Code Style

- Java 21+ language features
- SLF4J for logging
- Picocli for CLI
- Jackson for YAML parsing
- Jakarta EE/CDI for dependency injection

### Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 5 | 5.11.4 | Unit testing |
| Mockito | 5.15.2 | Mocking |
| Picocli | 4.7.7 | CLI framework |
| Jackson | 2.18.2 | YAML/JSON parsing |
| Weld/CDI | Latest | Dependency injection |
| Log4j2 | Latest | Logging |
| Cucumber | 7.21.0 | BDD tests |
| JaCoCo | 0.8.13 | Code coverage |

## Roadmap

### Completed ✅
- [x] Full recipe YAML parsing and model
- [x] Multi-source repository system
- [x] Recipe dependency resolution and ordering
- [x] Built-in recipes (ResourceRepository)
- [x] Local directory repositories (DirectoryRepository)
- [x] Git repository support with caching
- [x] ZIP archive extraction support
- [x] HTTP/HTTPS remote repositories
- [x] Package registry/installation tracking
- [x] Configuration management system
- [x] CLI command structure (list, install, shell, config)
- [x] Comprehensive unit test coverage
- [x] BDD acceptance tests
- [x] Code coverage analysis (~78%)

### In Progress 🔄
- [ ] Improve code coverage to 95%
- [ ] Entry point integration testing
- [ ] Complex repository scenario coverage

### Planned 📋
- [ ] Windows registry integration
- [ ] Shell environment variable setup
- [ ] Recipe validation and linting
- [ ] Package update/upgrade support
- [ ] Dependency tree visualization
- [ ] Configuration UI/wizard
- [ ] Plugin system for custom repositories
- [ ] Performance optimization and caching
- [ ] Parallel recipe installation
## Development

### IDE Setup

The project can be imported into any Java IDE that supports Maven:

- **IntelliJ IDEA**: File → Open → Select pom.xml
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Open folder with Java extension pack installed

### Code Style

- Java 17 language features
- Follow standard Java naming conventions
- Use SLF4J for logging

## Architecture

### Repository System

Levain 2 uses a flexible, multi-source repository system that combines recipes from different sources:

#### Repository Types

1. **ResourceRepository** - Built-in recipes from JAR
   - Packaged inside the JAR file at `src/main/resources/recipes/`
   - Always available, no external dependencies
   - Contains core Levain runtime recipe

2. **DirectoryRepository** - Local filesystem recipes
   - Loads `.levain.yaml` files from a directory
   - Discovered via environment variable `LEVAIN_RECIPES_DIR`
   - Or system property `levain.recipes.dir`
   - Or standard location `~/levain/levain-pkgs/recipes`

3. **GitRepository** - Clone and load from Git
   - Supports HTTP/HTTPS Git URLs
   - Automatically clones to local cache
   - Caches in `~/.levain/cache/git/`
   - Updates on each access with `git pull`
   - Requires Git to be installed and available on PATH

4. **ZipRepository** - Extract and load from ZIP
   - Supports local file paths or HTTP/HTTPS URLs
   - Automatically downloads and extracts
   - Caches extracted content in `~/.levain/cache/`
   - Re-uses cache if already extracted

5. **RemoteRepository** - Direct HTTP/HTTPS access
   - Loads recipes from HTTP endpoints
   - Supports GitHub repository URLs
   - Normalizes GitHub URLs to raw content format
   - No local caching of YAML content (yet)

6. **Registry** - Installed packages tracker
   - Located at `~/.levain/registry/`
   - Stores copies of installed recipe YAML files
   - Enables offline access to installed recipes
   - One `.levain.yaml` file per installed package

#### Recipe Resolution Flow

```
RecipeService.loadRecipe("jdk-21")
    ↓
RecipeTree.getRecipe("jdk-21")
    ↓
RepositoryManager.resolveRecipe("jdk-21")
    ├→ ResourceRepository (search first)
    ├→ Configured repositories (from config.json)
    ├→ DirectoryRepository (if configured)
    └→ Registry (fallback to installed recipes)
    ↓
Recipe found in ResourceRepository
```

#### Repository Configuration

Repositories are configured in `~/.levain/config.json`:

```json
{
  "levainHome": "/home/user/levain",
  "registryDir": "/home/user/.levain/registry",
  "cacheDir": "/home/user/.levain/cache",
  "shellPath": "/bin/bash",
  "defaultPackage": "levain",
  "repositories": [
    {
      "name": "levain-pkgs",
      "uri": "https://github.com/jmoalves/levain-pkgs"
    },
    {
      "name": "local-recipes",
      "uri": "dir:/home/user/my-recipes"
    }
  ]
}
```

#### Adding Repository Sources

Via CLI:
```bash
# Add Git repository
levain config add-repo pkgs https://github.com/jmoalves/levain-pkgs

# Add local directory
levain config add-repo local dir:/home/user/recipes

# Add ZIP archive
levain config add-repo archive file:///path/to/recipes.zip

# Add HTTP endpoint
levain config add-repo remote https://recipes.example.com

# List all repositories
levain config list-repo

# Remove a repository
levain config remove-repo pkgs
```

### Configuration System

#### Configuration File

Located at `~/.levain/config.json`, persisted by `ConfigService`.

Supports:
- Custom levain home directory
- Registry location
- Cache location
- Default shell path
- Default package
- Custom environment variables
- Repository definitions

#### Configuration Scopes (Priority Order)

1. **Command-line options** (highest priority)
   - `--levainHome /path`
   - `--levainCache /path`
   - `--verbose`

2. **System properties**
   - `-Dlevain.recipes.dir=/path`
   - `-Dlevain.cache.dir=/path`

3. **Environment variables**
   - `LEVAIN_RECIPES_DIR`
   - `LEVAIN_CACHE_DIR`

4. **Configuration file** (`~/.levain/config.json`)

5. **Defaults** (lowest priority)
   - levainHome: `~/levain`
   - registryDir: `~/.levain/registry`
   - cacheDir: `~/.levain/cache`

### Recipe Format

Recipes are YAML files with `.levain.yaml` extension:

```yaml
name: jdk-21
version: 21.0.5
description: Java Development Kit 21
commands:
  install:
    - echo "Installing JDK 21"
    - # More install commands
  validate:
    - java -version
dependencies:
  - levain  # Implicit for all recipes
```

### Dependency Resolution

`RecipeTree` handles:
- Implicit `levain` dependency for all recipes
- Explicit dependency chains
- Circular dependency detection
- Depth-first traversal for installation order
- Deduplication (each recipe installed once)

#### Example

```yaml
name: springboot
dependencies:
  - jdk-21
  - maven
```

Installation order: `levain` → `jdk-21` → `maven` → `springboot`

## License

See [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Write tests for new functionality
4. Make your changes following the code style
5. Ensure all tests pass (`mvn clean test`)
6. Verify code coverage is maintained
7. Submit a pull request

## Support & Contact

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/jmoalves/levain-2).
