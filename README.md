# Levain 2

_Something to help you make your software grow_

A Java standalone console application for development environment installation. This is a drop-in replacement for the original [levain](https://github.com/jmoalves/levain) and supports recipes from the [levain-pkgs](https://github.com/jmoalves/levain-pkgs) repository.

## Features

- **Cross-Platform**: Runs on Windows and Linux
- **Multiple Distribution Options**:
  - Standalone JAR file (platform-independent)
  - Native executables for Windows and Linux (via GraalVM)
- **Recipe-Based Installation**: Uses YAML recipes for package installation
- **Testing**: Comprehensive testing with JUnit 5, Cucumber, and Mockito

## Installation Options

### Option 1: Using the Standalone JAR

Download the standalone JAR and run it with your favorite JVM:

```bash
java -jar levain-standalone-2.0.0-SNAPSHOT.jar [command] [options]
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

- JDK 17 or later (Java 21+ recommended for long-term support)
- Maven 3.9 or later
- (Optional) GraalVM 17+ for native executable builds

### Build Commands

**Build the project and run tests:**
```bash
mvn clean test
```

**Build the standalone JAR:**
```bash
mvn clean package
```

This creates:
- `target/levain-2.0.0-SNAPSHOT.jar` - Regular JAR
- `target/levain-standalone-2.0.0-SNAPSHOT.jar` - Standalone JAR with all dependencies

**Build native executable (requires GraalVM):**
```bash
mvn clean package -Pnative
```

This creates platform-specific native executables in the `target/` directory.

## Usage

### List Available Packages

```bash
# List all recipes
levain list

# Filter recipes
levain list jdk
```

### Install Packages

```bash
# Install a single package
levain install jdk-21

# Install multiple packages
levain install jdk-21 git maven
```

### Open a Configured Shell

```bash
# Open shell with no packages
levain shell

# Open shell with specific packages
levain shell jdk-21 maven
```

## Testing

The project uses three testing frameworks:

- **JUnit 5**: Unit tests
- **Cucumber**: BDD/acceptance tests
- **Mockito**: Mocking framework for unit tests

### Run All Tests

```bash
mvn test
```

### Run Only Unit Tests

```bash
mvn test -Dtest=*Test
```

### Run Only Cucumber Tests

```bash
mvn test -Dtest=RunCucumberTest
```

## Project Structure

```
levain-2/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/github/jmoalves/levain/
│   │   │       ├── Levain.java              # Main entry point
│   │   │       ├── cli/                     # CLI commands
│   │   │       ├── model/                   # Data models
│   │   │       ├── service/                 # Business logic
│   │   │       └── util/                    # Utilities
│   │   └── resources/
│   │       └── logback.xml                  # Logging configuration
│   └── test/
│       ├── java/
│       │   └── com/github/jmoalves/levain/
│       │       ├── cucumber/                # Cucumber step definitions
│       │       └── service/                 # Unit tests
│       └── resources/
│           └── features/                    # Cucumber feature files
├── pom.xml                                  # Maven configuration
└── README.md                                # This file
```

## Development

### IDE Setup

The project can be imported into any Java IDE that supports Maven:

- **IntelliJ IDEA**: File → Open → Select pom.xml
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Open folder with Java extension pack installed

### Code Style

- Java 21 language features
- Follow standard Java naming conventions
- Use SLF4J for logging

## License

See [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Run the test suite
5. Submit a pull request

## Compatibility

This version aims to be a drop-in replacement for the original levain, supporting:
- The same recipe format (YAML)
- Compatible command-line interface
- Recipe repositories from levain-pkgs

## Roadmap

- [ ] Full recipe YAML parsing
- [ ] Recipe repository management
- [ ] Package download and extraction
- [ ] Environment variable management
- [ ] Windows registry integration
- [ ] Complete levain-pkgs compatibility
- [ ] Shell integration improvements
