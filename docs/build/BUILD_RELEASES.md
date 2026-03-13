# Building Release Artifacts

This project is built and released as a standalone JAR.

## Build Target

### Standalone JAR

A platform-independent JAR file that runs on any system with Java 25+.

**Build command:**
```bash
mvn clean package -DskipTests
```

**Output:** `target/levain.jar`

**Usage:**
```bash
java -jar target/levain.jar [options]
```

## GitHub Actions Release Workflow

The Release workflow (`.github/workflows/release.yml`) builds and publishes:
- `levain.jar`
- installer scripts (`install-linux.sh`, `install-macos.sh`, `install-windows.ps1`)

### Trigger the Release

**Option 1: Tag-based release**
```bash
git tag v2.0.0
git push origin v2.0.0
```

**Option 2: Manual dispatch**
1. Go to Actions tab in GitHub
2. Select "Release" workflow
3. Click "Run workflow"

## Requirements

### For Local Builds

- JDK 25
- Maven 3.9.0+

## Development

During development:
```bash
mvn clean package
```

This runs tests and generates coverage reports in addition to creating the JAR.

## Version Management

The version is managed in `pom.xml`:
```xml
<version>2.0.0-SNAPSHOT</version>
```

- For development: Use `SNAPSHOT` suffix
- For release: Remove `-SNAPSHOT` and tag with `v<version>`
- For next development: Increment and add `-SNAPSHOT`
