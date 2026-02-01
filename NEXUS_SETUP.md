# Nexus Repository Configuration

This project supports integration with Nexus Repository Manager for corporate environments. This allows you to proxy dependencies through your corporate Nexus instance instead of using Maven Central directly.

## Quick Start

### Option 1: Environment Variable (Recommended)

Set the `NEXUS_REPOSITORY_URL` environment variable and the profile will automatically activate:

```bash
# Linux/macOS
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn clean package

# Windows (PowerShell)
$env:NEXUS_REPOSITORY_URL = "http://nexus.example.com:8081/repository/maven-public"
mvn clean package

# Windows (Command Prompt)
set NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn clean package
```

**Verify it's working:**
```bash
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn help:active-profiles
# Should show "nexus" profile as active
```

### Option 2: Maven Command Line Property

```bash
mvn clean package -Dnexus.url=http://nexus.example.com:8081/repository/maven-public
```

### Option 3: Persistent Shell Configuration

Add to `~/.bash_profile`, `~/.bashrc`, or `~/.zshrc`:

```bash
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
```

Then reload and build:
```bash
source ~/.bash_profile
mvn clean package
```

## Finding Your Nexus Repository URL

### In Nexus 3.x

1. Log in to your Nexus instance
2. Go to **Repositories** (left sidebar)
3. Find your repository (usually named `maven-public`, `maven-releases`, or similar)
4. Copy the URL from the **Copy** button in the HTTP or HTTPS column

Common repository URLs:
- `http://nexus.example.com:8081/repository/maven-public` (public group)
- `http://nexus.example.com:8081/repository/maven-releases` (releases only)
- `http://nexus.example.com:8081/repository/maven-snapshots` (snapshots only)

### In Nexus 2.x

Repository URLs follow the pattern:
- `http://nexus.example.com:8081/nexus/content/groups/public`
- `http://nexus.example.com:8081/nexus/content/repositories/releases`

## Authentication (if required)

If your Nexus repository requires authentication, you need to configure Maven settings.

### Create ~/.m2/settings.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <servers>
        <server>
            <id>nexus-releases</id>
            <username>your-username</username>
            <password>your-password</password>
        </server>
        <server>
            <id>nexus-plugin-releases</id>
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>

</settings>
```

**Security Note:** For production environments, use Maven password encryption:

```bash
# Generate encrypted password
mvn --encrypt-password your-password

# Add encrypted password to settings.xml
<password>{Encryption...}</password>
```

## Persistent Configuration

### Using .env file (for development)

Create a `.env` file in the project root:

```bash
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
```

Then load it before building:

```bash
source .env
mvn clean package
```

### Using .bash_profile or .bashrc

Add to `~/.bash_profile` or `~/.bashrc`:

```bash
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
```

Then reload:

```bash
source ~/.bash_profile
mvn clean package
```

### Using Maven Wrapper (if available)

Edit `.mvn/maven.config`:

```
-Dnexus.repository.url=http://nexus.example.com:8081/repository/maven-public
```

## Configuration in Release CI/CD

### GitHub Actions

Add to your workflow file:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    env:
      NEXUS_REPOSITORY_URL: ${{ secrets.NEXUS_REPOSITORY_URL }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven
      - run: mvn clean package
```

Then add the secret in GitHub:
1. Settings → Secrets and variables → Actions
2. New repository secret
3. Name: `NEXUS_REPOSITORY_URL`
4. Value: `http://nexus.example.com:8081/repository/maven-public`

### Other CI/CD Systems

For Jenkins, GitLab CI, Azure Pipelines, etc., set the `NEXUS_REPOSITORY_URL` environment variable during the build.

## Build Profiles

The project includes:

- **`nexus` profile**: Automatically activates when `nexus.repository.url` property is set
- **`native-windows` profile**: For native Windows executables
- **`native-linux` profile**: For native Linux executables

Combine profiles as needed:

```bash
# Build native Windows executable using Nexus (environment variable)
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn clean package -Pnative-windows

# Build native Linux executable using Nexus (command line)
mvn clean package -Pnative-linux -Dnexus.url=http://nexus.example.com:8081/repository/maven-public
```

## Troubleshooting

### "Cannot download repository metadata"

**Cause:** Nexus URL is incorrect or repository doesn't exist

**Solution:**
1. Verify the URL in Nexus UI
2. Test connectivity: `curl http://nexus.example.com:8081/repository/maven-public`
3. Check Nexus logs for errors

### "Authentication failed"

**Cause:** Invalid username/password in settings.xml

**Solution:**
1. Verify credentials in Nexus UI
2. Check server ID matches in settings.xml (should be `nexus-releases` or `nexus-plugin-releases`)
3. Re-encrypt password: `mvn --encrypt-password your-password`

### "Connection refused"

**Cause:** Nexus is unreachable or offline

**Solution:**
1. Check Nexus is running: `curl http://nexus.example.com:8081`
2. Check firewall/network connectivity
3. Temporarily disable Nexus and use Maven Central: `unset NEXUS_REPOSITORY_URL`

### Slow downloads

**Cause:** Network latency or Nexus performance issues

**Solution:**
1. Verify Nexus is healthy
2. Check network connectivity
3. Consider using mirror if available
4. Rebuild with `mvn clean package -o` (offline mode if you've already cached dependencies)

## Snapshots vs Releases

The Nexus profile is configured to handle both:

- **Releases** (`nexus.repository.releases.enabled=true`): Downloaded by default
- **Snapshots** (`nexus.repository.snapshots.enabled=true`): Downloaded by default

To disable snapshots:

```bash
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn clean package -Dnexus.repository.snapshots.enabled=false
```

Or with command line property:

```bash
mvn clean package \
  -Dnexus.url=http://nexus.example.com:8081/repository/maven-public \
  -Dnexus.repository.snapshots.enabled=false
```

## Repository Types

### Group Repository (Recommended)

Contains multiple repositories (releases, snapshots, 3rd-party, etc.). This is the best option for most use cases.

Example: `http://nexus.example.com:8081/repository/maven-public`

### Releases Repository

For released versions only. Use if you want to exclude snapshots.

Example: `http://nexus.example.com:8081/repository/maven-releases`

### Snapshots Repository

For snapshot versions only. Use for development.

Example: `http://nexus.example.com:8081/repository/maven-snapshots`

## Disabling Nexus

To revert to Maven Central:

```bash
# Unset environment variable
unset NEXUS_REPOSITORY_URL

# Then build normally
mvn clean package
```

Or don't use the `-Pnexus` profile when specifying properties.

## Best Practices

1. ✅ Use environment variables for development
2. ✅ Use GitHub secrets for CI/CD
3. ✅ Use Maven settings.xml for authentication
4. ✅ Use encrypted passwords in settings.xml
5. ✅ Use a group repository URL when possible
6. ✅ Document your Nexus URL in team wiki/documentation
7. ❌ Don't commit passwords to version control
8. ❌ Don't use plaintext passwords in CI/CD
9. ❌ Don't hardcode Nexus URLs in pom.xml (use properties instead)

## Fallback to Maven Central

The project is configured with Maven Central as the default repository. If Nexus is not available or not configured, Maven will automatically fall back to Maven Central for all dependencies.

This ensures builds never fail due to Nexus misconfiguration.
