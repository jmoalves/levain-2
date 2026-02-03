# Development Guide

This guide is for developers who want to contribute to or understand the internals of Levain 2.

## IDE Setup

The project can be imported into any Java IDE that supports Maven:

- **IntelliJ IDEA**: File → Open → Select pom.xml
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Open folder with Java extension pack installed

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
├── README.md                                    # Project overview
└── LICENSE                                      # MIT License
```

## Core Architecture

### Layered Design

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
- `ShellService`: Shell management and integration

**Repository Layer (`repository/`)**:
- `RepositoryManager`: Chains multiple repositories, deduplicates recipes
- `Registry`: Tracks installed packages in local cache
- Multiple source types: Resources, Directory, Git, ZIP, HTTP

**Model Layer (`model/`)**:
- `Recipe`: YAML recipe structure with commands and dependencies
- `RecipeTree`: Dependency resolution and ordering
- `Config`: Configuration management

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

Current: Coverage tracked via [codecov](https://codecov.io/gh/jmoalves/levain-2)

Target: 95% (Line/Branch)

Focus areas for improvement:
- Entry point coverage (Levain, LevainCommand)
- Complex repository logic (Git/ZIP extraction)
- Error handling and edge cases
- Shell integration

## Code Style

- Java 25 language features
- Follow standard Java naming conventions
- Use SLF4J for logging (via Log4j2)
- Use Picocli annotations for CLI
- Use Jackson annotations for YAML

## Implementation Details

### Configuration System

See [Configuration Implementation](../configuration/CONFIG_IMPLEMENTATION.md) for details on how configuration is managed.

### Registry System

See [Registry Implementation](../registry/REGISTRY_IMPLEMENTATION.md) for details on how installed packages are tracked.

### Repository Architecture

Multiple repository types support different sources:
- `ResourceRepository` - Built-in recipes in the JAR
- `DirectoryRepository` - Local directory of recipes
- `GitRepository` - Git repositories (auto-cloned)
- `ZipRepository` - ZIP archives (auto-extracted)
- `RemoteRepository` - HTTP/HTTPS recipe sources

See [Additional Repositories Analysis](../implementation/ADDITIONAL_REPOSITORIES_ANALYSIS.md) for technical details.

## Contributing

Before submitting changes:

1. Ensure all tests pass: `mvn clean verify`
2. Follow the project's code style (see above)
3. Add tests for new functionality
4. Update documentation as needed

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Write tests for new functionality
4. Make your changes following the code style
5. Ensure all tests pass (`mvn clean test`)
6. Verify code coverage is maintained
7. Submit a pull request

## Debugging

Set system properties for debug output:

```bash
mvn test -Dlevain.debug=true
```

Check logs in `logs/levain.log` when running the application.

