# Nexus Setup - Simple Mirror Configuration

The simplest and most reliable way to use Nexus with Maven is through the `~/.m2/settings.xml` file with mirror configuration.

## Quick Setup

### 1. Copy the settings file

```bash
# Create .m2 directory if it doesn't exist
mkdir -p ~/.m2

# Copy the template
cp .m2/settings.xml ~/.m2/settings.xml
```

Or manually create `~/.m2/settings.xml` with this content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <mirrors>
        <mirror>
            <id>nexus</id>
            <name>Nexus Repository Mirror</name>
            <url>http://astronauta.biruta.net:31000/repository/maven-public</url>
            <mirrorOf>*</mirrorOf>
        </mirror>
    </mirrors>

</settings>
```

### 2. Update the Nexus URL

Edit `~/.m2/settings.xml` and replace the URL with your Nexus server:
- `http://astronauta.biruta.net:31000/repository/maven-public`
- `http://your-nexus-server:8081/repository/maven-public`
- `https://nexus.company.com/repository/maven-public`

### 3. Test it works

```bash
# Clean your local Maven cache (optional but recommended for testing)
rm -rf ~/.m2/repository

# Run Maven - it should now use Nexus
mvn clean compile

# Check Maven output - you should see downloads from your Nexus URL
```

## Windows Setup

### Location
`%USERPROFILE%\.m2\settings.xml` (typically `C:\Users\YourName\.m2\settings.xml`)

### PowerShell
```powershell
# Create directory
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.m2"

# Edit the file
notepad "$env:USERPROFILE\.m2\settings.xml"
```

Paste the XML content above and save.

## Adding Authentication (If Required)

If your Nexus requires login, add the `<servers>` section:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <!-- Add credentials here -->
    <servers>
        <server>
            <id>nexus</id>
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>

    <mirrors>
        <mirror>
            <id>nexus</id>
            <name>Nexus Repository Mirror</name>
            <url>http://astronauta.biruta.net:31000/repository/maven-public</url>
            <mirrorOf>*</mirrorOf>
        </mirror>
    </mirrors>

</settings>
```

**Important:** The server `<id>nexus</id>` must match the mirror `<id>nexus</id>`.

## How It Works

- **Mirror**: Redirects all Maven downloads to your Nexus server
- **mirrorOf="*"**: Catches ALL repository requests (Central, plugins, etc.)
- **Global**: Works for all Maven projects on your system
- **No project changes**: The pom.xml doesn't need any modifications

## Advantages Over Environment Variables

✅ **Standard Maven approach** - documented and widely used  
✅ **Works automatically** - no need to remember environment variables  
✅ **Global configuration** - applies to all projects  
✅ **Persistent** - survives terminal restarts  
✅ **More reliable** - Maven natively supports this method  

## Verifying Nexus is Being Used

Check Maven output when building:

```bash
mvn clean compile
```

You should see URLs like:
```
Downloading from nexus: http://astronauta.biruta.net:31000/repository/maven-public/...
```

Instead of:
```
Downloading from central: https://repo.maven.apache.org/maven2/...
```

## Troubleshooting

### Problem: Still downloading from Maven Central

**Solution:** Make sure `~/.m2/settings.xml` exists and is valid XML. Check the file path:
```bash
# Linux/macOS
cat ~/.m2/settings.xml

# Windows
type %USERPROFILE%\.m2\settings.xml
```

### Problem: Authentication errors (401 Unauthorized)

**Solution:** Add credentials in `<servers>` section with matching `<id>nexus</id>`.

### Problem: Connection refused or timeout

**Solution:** 
- Verify Nexus URL in browser: `http://astronauta.biruta.net:31000`
- Check firewall/network
- Try with `http://` instead of `https://` or vice versa

### Problem: SSL certificate errors

**Solution:** If using HTTPS with self-signed certificate:
```bash
mvn clean compile -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```

Or add to settings.xml:
```xml
<profiles>
    <profile>
        <id>disable-ssl</id>
        <properties>
            <maven.wagon.http.ssl.insecure>true</maven.wagon.http.ssl.insecure>
            <maven.wagon.http.ssl.allowall>true</maven.wagon.http.ssl.allowall>
        </properties>
    </profile>
</profiles>

<activeProfiles>
    <activeProfile>disable-ssl</activeProfile>
</activeProfiles>
```

## Disabling Nexus Temporarily

### Option 1: Rename settings.xml
```bash
mv ~/.m2/settings.xml ~/.m2/settings.xml.backup
mvn clean compile
mv ~/.m2/settings.xml.backup ~/.m2/settings.xml
```

### Option 2: Use command line override
```bash
mvn clean compile -s /dev/null  # Linux/macOS
mvn clean compile -s NUL        # Windows
```

## Mirror Patterns

Change `<mirrorOf>` to control what gets mirrored:

| Pattern | Description |
|---------|-------------|
| `*` | Mirror everything |
| `central` | Mirror only Maven Central |
| `*,!repo1` | Mirror everything except repo1 |
| `external:*` | Mirror all external repositories |

Example - mirror everything except specific repository:
```xml
<mirrorOf>*,!my-custom-repo</mirrorOf>
```

## Team Setup

For corporate environments, share this settings.xml with your team:

1. Commit `.m2/settings.xml` template to repository (this project already has it)
2. Team members copy to `~/.m2/settings.xml`
3. Update Nexus URL if needed
4. Add credentials if authentication required

## CI/CD Setup

GitHub Actions example:

```yaml
- name: Create Maven settings.xml
  run: |
    mkdir -p ~/.m2
    cat > ~/.m2/settings.xml << 'EOF'
    <settings>
      <mirrors>
        <mirror>
          <id>nexus</id>
          <url>${{ secrets.NEXUS_URL }}</url>
          <mirrorOf>*</mirrorOf>
        </mirror>
      </mirrors>
    </settings>
    EOF

- name: Build with Maven
  run: mvn clean package
```

Add `NEXUS_URL` secret in GitHub Settings → Secrets.
