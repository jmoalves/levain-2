# Levain 2 Distribution Strategy

## Overview

Levain 2 supports multiple distribution formats to handle different user scenarios:

### 1. **Native Executables** (Recommended)
- **Format**: Standalone compiled binaries per platform
- **Platforms**: `levain-linux-x86_64`, `levain-macos-x86_64`, `levain-macos-aarch64`, `levain-windows-x86_64.exe`
- **Dependencies**: None
- **Size**: Moderate (depends on GraalVM native-image)
- **Installation**: Direct execution, bootstrap recipe copies to home directory

**Build Process**:
```bash
# Requires GraalVM or native-image installation
mvn clean package -Pnative
# Produces: target/levain (native executable)
```

### 2. **JAR Distribution**
- **Format**: `levain.jar` (shaded/fat JAR with all dependencies)
- **Dependencies**: Java 25+ required
- **Size**: ~20MB (shaded JAR)
- **Installation**: `java -jar levain.jar install levain`

**Build Process**:
```bash
# Default Maven build
mvn clean package -DskipTests
# Produces: target/levain.jar
```

### 3. **Source Installation**
- **Format**: Git repository with Maven build
- **Dependencies**: Java 25+, Maven 3.8+
- **For**: Developers, custom builds, contributions

**Build Process**:
```bash
git clone https://github.com/jmoalves/levain-2.git
cd levain-2
mvn clean package
java -jar target/levain.jar install levain
```

## Smart Bootstrap Scripts

The `install.sh` and `install.ps1` scripts handle platform detection and fallback:

```
1. Detect OS/Architecture
2. Try to download native executable
   ├─ If found: Execute natively (no Java needed)
   └─ If not found: Fall back to JAR
3. For JAR version:
   ├─ Check Java is installed
   ├─ If not found: Show helpful instructions
   └─ If found: Run JAR installation
```

## Installation Methods

### Linux/macOS
```bash
# One-liner from latest release
curl https://github.com/jmoalves/levain-2/releases/latest/download/install.sh | bash

# Specific version
levainVersion="2.0.0" curl ... | bash

# Custom home directory
levainHome="/opt/levain" curl ... | bash

# Force JAR version (if Java preferred)
levainUseJar=true curl ... | bash
```

### Windows (PowerShell)
```powershell
# One-liner from latest release
iwr https://github.com/jmoalves/levain-2/releases/latest/download/install.ps1 | iex

# Specific version
$levainVersion="2.0.0";iwr ... | iex

# Custom home directory
$levainHome="C:\dev-env";iwr ... | iex

# Force JAR version (if Java preferred)
$levainUseJar=$true;iwr ... | iex
```

## Release Publishing Checklist

1. **Build native executables** (if included in release):
   ```bash
   mvn clean package -Pnative
   ```

2. **Build JAR**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Create GitHub Release** with assets:
   - `levain-linux-x86_64` (or native-image output)
   - `levain-macos-x86_64` (if available)
   - `levain-macos-aarch64` (if available)
   - `levain-windows-x86_64.exe` (if available)
   - `levain.jar` (always, as fallback)
   - `install.sh`
   - `install.ps1`
   - SHA256 checksums

4. **Update version** in:
   - `pom.xml` (Maven version)
   - `src/main/resources/recipes/levain.levain.yaml` (Levain recipe version)

## Architecture

### Bootstrap Flow

```
User runs: curl ... | bash (or .ps1 | iex on Windows)
        ↓
Script detects platform (Linux/macOS/Windows, x86_64/aarch64)
        ↓
Try download native executable
    ├─ Success: Run native binary directly
    │   └─ CopyAction copies files to home
    │
    └─ Not found: Fall back to JAR
        ├─ Check Java installed
        │   ├─ Not found: Show installation instructions
        │   │
        │   └─ Found: Download and run JAR
        │       └─ CopyAction copies files to home
```

### No Circular Dependency

**Key insight**: The installer runs a pre-compiled version (native or JAR) that's already extracted/downloaded. That running instance then uses the `copy` action to install itself to the home directory.

This avoids the circular dependency problem:
- ✅ Levain v2.0.0 from releases runs the install
- ✅ Running Levain copies v2.0.0 to home directory
- ✅ Next time, installed Levain manages packages
- ✅ Levain can self-upgrade via recipe mechanism

## Future Enhancements

1. **GraalVM Native Image**: Compile to true native executable
2. **Minimal JRE embedding**: Package lightweight Java runtime with JAR
3. **Checksums**: Add SHA256 verification to bootstrap scripts
4. **Multiple mirrors**: Support Nexus/Artifactory mirrors
5. **Auto-updates**: Built-in mechanism to update Levain itself
6. **OS Package managers**: Publish to brew, apt, choco, etc.

