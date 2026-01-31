# Installation Options Guide

This document describes the various installation options available for Levain 2.

## 1. Platform-Independent JAR File

The standalone JAR file can run on any platform with a JVM installed.

### Prerequisites
- Java 17 or later

### Building
```bash
mvn clean package
```

### Running
```bash
# Using the standalone JAR (includes all dependencies)
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar [command] [options]

# Example commands
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar list
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar install git
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar shell jdk-21
```

### Distribution
The standalone JAR is a single file (~4.5MB) that can be distributed and run anywhere Java is available.

## 2. Native Executables (Windows & Linux)

Native executables provide faster startup times and don't require a JVM installation.

### Prerequisites
- GraalVM 17 or later with native-image installed
- For Windows: Microsoft Visual C++ Build Tools
- For Linux: gcc, glibc-devel, zlib-devel

### Building

#### On Windows
```cmd
mvn clean package -Pnative
```
This creates `target\levain.exe`

#### On Linux
```bash
mvn clean package -Pnative
```
This creates `target/levain`

### Running

#### Windows
```cmd
levain.exe list
levain.exe install git maven
levain.exe shell jdk-21
```

#### Linux
```bash
./levain list
./levain install git maven
./levain shell jdk-21
```

### Distribution
Native executables are platform-specific but don't require Java installation:
- Windows executable: `levain.exe` (~50-100MB)
- Linux executable: `levain` (~50-100MB)

## 3. Installing GraalVM (Optional)

### For Windows
1. Download GraalVM from https://www.graalvm.org/downloads/
2. Extract to a directory (e.g., `C:\graalvm-jdk-17`)
3. Set `JAVA_HOME` to the GraalVM directory
4. Install native-image:
   ```cmd
   gu install native-image
   ```

### For Linux
1. Download GraalVM:
   ```bash
   wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.0/graalvm-ce-java17-linux-amd64-22.3.0.tar.gz
   tar -xzf graalvm-ce-java17-linux-amd64-22.3.0.tar.gz
   ```
2. Set `JAVA_HOME`:
   ```bash
   export JAVA_HOME=/path/to/graalvm-ce-java17-22.3.0
   export PATH=$JAVA_HOME/bin:$PATH
   ```
3. Install native-image:
   ```bash
   gu install native-image
   ```

## Comparison of Installation Options

| Feature | Standalone JAR | Native Executable |
|---------|---------------|-------------------|
| Platform | Cross-platform | Platform-specific |
| Size | ~4.5 MB | ~50-100 MB |
| Startup Time | ~1-2 seconds | ~0.1 seconds |
| JVM Required | Yes (Java 17+) | No |
| Memory Usage | Higher | Lower |
| Distribution | Single file | Platform-specific binary |
| Build Time | Fast (~10s) | Slow (~2-5 min) |

## Recommended Approach

1. **Development**: Use the standalone JAR for quick iterations
2. **CI/CD**: Use the standalone JAR for testing
3. **Production/Distribution**: Use native executables for best user experience

## Testing Your Build

After building, test your installation:

```bash
# For JAR
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar --version
java -jar target/levain-standalone-2.0.0-SNAPSHOT.jar list

# For native executable (Windows)
levain.exe --version
levain.exe list

# For native executable (Linux)
./levain --version
./levain list
```

## Troubleshooting

### JAR Issues
- **Error: UnsupportedClassVersionError**: Java version is too old. Requires Java 17+
- **Error: NoClassDefFoundError**: Use the standalone JAR, not the regular JAR

### Native Executable Issues
- **Build fails with "native-image not found"**: GraalVM native-image is not installed
- **Build fails on Windows**: Install Visual C++ Build Tools
- **Build fails on Linux**: Install build-essential, zlib1g-dev
- **Slow build**: Native image compilation is resource-intensive and can take 2-5 minutes

## Future Enhancements

Planned installation options:
- Docker container images
- Package managers (apt, yum, chocolatey, homebrew)
- Windows installer (MSI)
- Linux packages (deb, rpm)
