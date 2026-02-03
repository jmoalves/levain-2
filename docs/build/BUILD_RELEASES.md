# Building Release Artifacts

This project can be built in three different formats:

## Build Targets

### 1. Standalone JAR (Multiplatform)
A platform-independent JAR file that runs on any system with Java 25+.

**Build command:**
```bash
mvn clean package -DskipTests
```

**Output:** `target/levain-standalone-<version>.jar`

**Usage:**
```bash
java -jar levain-standalone-<version>.jar [options]
```

### 2. Windows x64 Executable
Native Windows executable built with GraalVM Native Image. Requires Windows and GraalVM.

**Build command (on Windows):**
```bash
mvn clean package -DskipTests -Pnative
```

**Output:** `target/levain.exe`

**Usage:**
```cmd
levain.exe [options]
```

Drop-in replacement for levain command line tool.

### 3. Linux x64 Executable
Native Linux executable built with GraalVM Native Image. Requires Linux and GraalVM.

**Build command (on Linux):**
```bash
mvn clean package -DskipTests -Pnative
```

First, ensure `native-image` is installed:
```bash
gu install native-image
```

**Output:** `target/levain`

**Usage:**
```bash
./levain [options]
```

Drop-in replacement for levain command line tool.

## GitHub Actions Release Workflow

The Release workflow (`.github/workflows/release.yml`) automatically builds all three targets:

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

### What the Release Workflow Does

1. **build-jar job (runs on ubuntu-latest)**
   - Builds the standalone JAR with all dependencies
   - Output: `levain-standalone-<version>.jar`

2. **build-windows job (runs on windows-latest)**
   - Builds the native Windows x64 executable
   - Uses GraalVM native-image
   - Output: `levain-<version>-windows-x64.exe`

3. **build-linux job (runs on ubuntu-latest)**
   - Builds the native Linux x64 executable
   - Installs GraalVM native-image
   - Output: `levain-<version>-linux-x64`

4. **release job**
   - Creates a GitHub Release with all three artifacts
   - Generates release notes automatically
   - Only runs on tag push (e.g., `v2.0.0`)

5. **publish-artifacts job**
   - Used for manual workflow dispatch
   - Displays downloaded artifacts

### Release Artifacts

All three binaries are attached to the GitHub Release:
- `levain-standalone-2.0.0.jar` - Multiplatform JAR
- `levain-2.0.0-windows-x64.exe` - Windows executable
- `levain-2.0.0-linux-x64` - Linux executable

## Requirements

### For Local Builds

- **JDK 25** (Temurin or GraalVM distribution recommended)
- **Maven 3.9.0+**
- **GraalVM native-image** (only for native executables)
  - Install with: `gu install native-image`
  - Requires 10+ GB disk space and 4+ GB RAM for compilation

### For GitHub Actions

Requirements are automatically handled by the workflow:
- Windows runner for Windows executable
- Linux runner for Linux executable
- GraalVM community edition distributions

## Development

During development, build the standalone JAR:
```bash
mvn clean package
```

This runs tests and generates coverage reports in addition to creating the JAR.

## Troubleshooting

### GraalVM Native Image Issues

If you encounter issues building native images:

1. Ensure you have GraalVM installed (not regular JDK):
   ```bash
   java -version
   ```
   Should show `GraalVM` in output.

2. Install native-image tool:
   ```bash
   gu install native-image
   ```

3. Try building with increased memory:
   ```bash
   MAVEN_OPTS="-Xmx8g" mvn clean package -DskipTests -Pnative
   ```

### Windows Executable Build Issues

- Requires Visual C++ Build Tools or MinGW
- If build fails, install Build Tools for Visual Studio
- Windows 10 or later recommended

### Linux Executable Build Issues

- Requires GCC and standard build tools:
  ```bash
  sudo apt-get install build-essential
  ```

## Version Management

The version is managed in `pom.xml`:
```xml
<version>2.0.0-SNAPSHOT</version>
```

- For development: Use `SNAPSHOT` suffix
- For release: Remove `-SNAPSHOT` and tag with `v<version>`
- For next development: Increment and add `-SNAPSHOT`

Example release flow:
```bash
# Update version in pom.xml
mvn versions:set -DnewVersion=2.0.0 -DgenerateBackupPoms=false

# Commit and tag
git add pom.xml
git commit -m "Release 2.0.0"
git tag v2.0.0
git push origin main v2.0.0

# Start next development version
mvn versions:set -DnewVersion=2.1.0-SNAPSHOT -DgenerateBackupPoms=false
git add pom.xml
git commit -m "Start 2.1.0 development"
git push origin main
```
