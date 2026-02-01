# Nexus Integration Summary

I've successfully configured Nexus Repository support for your Levain project. This allows the Maven build to use a corporate Nexus instance if available, while gracefully falling back to Maven Central if not configured.

## What Was Added

### 1. **pom.xml Updates**

- **Properties** for Nexus configuration:
  - `nexus.repository.url`: Read from `NEXUS_REPOSITORY_URL` environment variable
  - `nexus.repository.releases.enabled`: Control whether to download releases (default: true)
  - `nexus.repository.snapshots.enabled`: Control whether to download snapshots (default: true)

- **Repository declarations**:
  - Default Maven Central Repository (always available as fallback)
  - Separate `<repositories>` and `<pluginRepositories>` sections

- **Nexus Profile** (`id=nexus`):
  - Auto-activates when `nexus.repository.url` property is set
  - Adds Nexus repositories for both artifacts and plugins
  - Uses configurable properties for flexibility

### 2. **Documentation Files**

- **[NEXUS_SETUP.md](NEXUS_SETUP.md)**: Comprehensive guide covering:
  - Quick start options (environment variable, command line, profile)
  - Finding your Nexus repository URL
  - Authentication setup with settings.xml
  - Persistent configuration methods
  - CI/CD integration (GitHub Actions, Jenkins, etc.)
  - Troubleshooting guide
  - Best practices

- **[.mvn/settings.xml.template](.mvn/settings.xml.template)**: Template Maven settings file with:
  - Server credentials configuration
  - Comments explaining each section
  - Optional mirror and profile configurations
  - Instructions for password encryption

## Usage

### Simplest Method: Environment Variable

```bash
# Set the environment variable
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public

# Build normally - Nexus profile auto-activates
mvn clean package

# Verify the profile is active
mvn help:active-profiles
```

### Command Line Method

```bash
mvn clean package -Dnexus.url=http://nexus.example.com:8081/repository/maven-public
```

### Test It Works

```bash
# The profile should show as active
export NEXUS_REPOSITORY_URL=http://nexus.example.com:8081/repository/maven-public
mvn help:active-profiles
# Output should include: "- nexus (source: ...)"
```

## Key Features

✅ **Optional**: Build works fine without Nexus configured  
✅ **Environment Variable Support**: Easy integration with CI/CD  
✅ **Flexible**: Works with any Nexus URL format  
✅ **Profiles**: Can combine with native builds (-Pnexus,native-linux)  
✅ **Fallback**: Automatically uses Maven Central if Nexus isn't available  
✅ **Authentication**: Supports credentials via settings.xml  
✅ **Documentation**: Comprehensive setup guide included  

## Integration with Release Workflow

To use Nexus in the GitHub Actions Release workflow, add a secret:

1. GitHub Settings → Secrets and variables → Actions
2. New secret: `NEXUS_REPOSITORY_URL` = your nexus URL

Then update `.github/workflows/release.yml` to include:

```yaml
env:
  NEXUS_REPOSITORY_URL: ${{ secrets.NEXUS_REPOSITORY_URL }}
```

## Backward Compatibility

✅ Existing builds continue to work without any changes  
✅ Default Maven Central is always available as fallback  
✅ No breaking changes to pom.xml structure  
✅ Build succeeds even if Nexus is unreachable  

## Next Steps

1. Copy `NEXUS_SETUP.md` to your team wiki/documentation
2. Use `.mvn/settings.xml.template` for team members who need authentication
3. Set `NEXUS_REPOSITORY_URL` environment variable in your CI/CD system
4. Document your specific Nexus URL in team guidelines

Your build is ready for both open-source (Maven Central) and corporate (Nexus) environments!
