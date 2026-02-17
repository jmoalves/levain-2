# Original Levain Features Analysis

**Analysis Date:** February 17, 2026  
**Source:** https://github.com/jmoalves/levain (TypeScript/Deno)

---

## Feature 1: Backup on Package Update/Reinstall

### Overview
Before updating or reinstalling a package, Levain saves the old `baseDir` to enable manual rollback.

### Implementation Details

**Location:** `src/cmd/install.ts` (lines 215-265) and `src/cmd/update.ts` (lines 186-236)

**Key Method:** `savePreviousInstall(bkpTag: string, pkg: Package): boolean`

**Behavior:**
1. **Creates timestamped backup directory:**
   ```
   <levainHome>/.levain/backup/bkp-YYYYMMDD-HHMMSS/<packageName>/
   ```

2. **Backup process:**
   - Checks if `baseDir` exists (if not, nothing to backup)
   - Creates backup directory: `levainBackupDir/bkp-YYYYMMDD-HHMMSS/`
   - Copies entire `baseDir` to backup location
   - Checks recipe flag `levain.preserveBaseDirOnUpdate`:
     - If `true`: Keeps old baseDir untouched (for packages with user data)
     - If `false` (default): Moves old baseDir to temp, then deletes it
   - On any error: Aborts installation

3. **Example backup structure:**
   ```
   ~/.levain/backup/
     bkp-20260217-143052/
       maven-3.9/          # Old Maven 3.9 installation
       node-18/            # Old Node 18 installation
     bkp-20260220-091523/
       maven-3.9/          # Another backup from different update
   ```

### Recipe Flag
```yaml
levain.preserveBaseDirOnUpdate: true  # Keep old baseDir after backup
```

### Rollback
- **Not automated** - manual process
- User must manually restore from backup directory
- Backup retention managed by `clean` command

---

## Feature 2: Package Update Check

### Overview
Automatically detects when installed packages have newer recipe versions and prompts user to update.

### Implementation Details

**Location:** `src/cmd/install.ts` (lines 82-126)

**Update Detection:**
- Compares installed recipe (in registry) with current recipe (in repository)
- Uses JSON serialization comparison: `JSON.stringify(currentRecipe) !== JSON.stringify(installedRecipe)`
- Property: `Package.updateAvailable: boolean`

**Behavior:**
1. **During `install` command:**
   - Checks if each package is installed
   - If installed, checks if `updateAvailable == true`
   - Separates packages into `willInstall[]` and `willUpdate[]`
   
2. **User prompt (if updates available):**
   ```
   Package updates available:
   ["maven-3.9", "node-18"]
   
   Proceed with update? [Y/n]:
   ```
   - Default: **Yes** (Y)
   - If user declines: Reloads installed packages without updating
   
3. **Override flags:**
   - `--force`: Always update (no prompt)
   - `--noUpdate`: Skip update check entirely

**Shell Command Integration:**
- `src/cmd/shell.ts` (lines 44-48)
- Controlled by `config.shellCheckForUpdate` flag
- If `true`: Runs `install <packages>` (with update check)
- If `false`: Runs `install --noUpdate <packages>` (skip check)

### Code Example
```typescript
// install.ts lines 91-126
if (!myArgs.force && !myArgs.noUpdate) {
    let willUpdate = []
    let willInstall = []

    for (let pkg of pkgs) {
        if (!pkg.installed) {
            willInstall.push(pkg.name)
        } else if (pkg.updateAvailable) {
            willUpdate.push(pkg.name)
        }
    }

    if (willUpdate.length > 0) {
        log.info("Package updates available:")
        log.info(JSON.stringify(willUpdate, null, 3))
        
        let answer = prompt("Proceed with update? [Y/n]", "Y")
        if (!answer || answer.toUpperCase() !== "Y") {
            shouldUpdate = false
            // Reload installed packages
            pkgs = this.config.packageManager.resolvePackages(pkgNames, true);
        }
    }
}
```

---

## Feature 3: Levain Self-Update (Autoupdate)

### Overview
Levain checks GitHub releases for new versions and can automatically update itself.

### Implementation Details

**Location:** `src/levain_cli.ts` (lines 66-69) and `src/lib/releases/levain_releases.ts` (lines 165-212)

**Update Check Flow:**

1. **Triggered on every command** (unless `--skip-levain-updates` flag used)
   ```typescript
   // levain_cli.ts line 66-69
   if (!myArgs["skip-levain-updates"]) {
       let levainReleases = new LevainReleases(config)
       await levainReleases.checkLevainUpdate()
   }
   ```

2. **Check GitHub Releases API:**
   - Endpoint: `https://api.github.com/repos/jmoalves/levain/releases/latest`
   - Compares `latestVersion` with `LevainVersion.levainVersion`
   - Validates release tag format: `v[0-9]+.[0-9]+.[0-9]+` (e.g., `v1.2.3`)

3. **Update decision:**
   - Stores `lastKnownVersion` in config
   - If `config.autoUpdate == true`: Updates immediately without prompt
   - If `config.autoUpdate == false`: Prompts user (once per day)
     - Tracks last prompt date with `config.lastUpdateQuestion`
     - Uses `DateUtils.dateTag()` to check if already asked today
   
4. **Prompt example:**
   ```
   New Levain version available: v1.5.0 (current: v1.4.2)
   
   Update now? [Y/n]:
   ```

5. **Update execution:**
   - Runs `install --force levain` command
   - Downloads latest release from GitHub
   - Extracts to Levain installation directory
   - Exits with message: "Levain has been upgraded. Please run your command again."

**Manual upgrade:**
```bash
levain --levain-upgrade
```

**Config properties:**
```typescript
config.autoUpdate: boolean           // Auto-update without prompt
config.lastUpdateQuestion: string    // Date of last prompt (YYYY-MM-DD)
config.lastKnownVersion: string      // Latest version from GitHub
config.shellCheckForUpdate: boolean  // Check for package updates in shell
```

### Code Example
```typescript
// levain_releases.ts lines 165-212
async checkLevainUpdate() {
    if (!this.allowsUpdate()) {
        return // Development versions don't update
    }

    let latestVersion = await this.latestVersion()
    if (!this.needsUpdate(latestVersion)) {
        return // Already up to date
    }

    this.config.lastKnownVersion = latestVersion.versionNumber

    if (!this.config.autoUpdate) {
        // Check if already asked today
        if (this.config.lastUpdateQuestion == DateUtils.dateTag()) {
            log.info("Ignoring update check (already asked today)")
            return
        }

        this.config.lastUpdateQuestion = DateUtils.dateTag()
        let answer = prompt("Update Levain now? [Y/n]", "Y")
        if (!answer || answer.toUpperCase() !== "Y") {
            log.info("Update postponed. Run 'levain --levain-upgrade' later.")
            return
        }
    }

    log.info("Upgrading Levain...")
    
    // Perform upgrade by installing levain package
    await loader.command("install", ["--force", "levain"])
    
    Deno.exit(0) // Exit after upgrade
}
```

---

## Other Notable Features

### 1. **Clean Command with Backup Management**
**Location:** `src/cmd/clean.ts`

- Automatically runs randomly (25% chance) before commands
- Cleans old backup directories (keeps today's backups)
- Controlled by flags: `--backup`, `--all`, `--keep-days`

### 2. **Retry Mechanism for File Operations**
**Location:** `src/lib/utils/utils.ts`

- Retries file operations (remove, move) up to 5 times
- Handles Windows file locking issues
- Used in: `savePreviousInstall()`

### 3. **Package Dependencies**
**Location:** `src/lib/package/file_system_package.ts`

- All packages automatically depend on `levain` package
- Ensures Levain itself is installed first
- Normalized in `normalizeDeps()`

### 4. **Registry Management**
**Location:** `src/lib/repository/registry.ts`

- Stores installed recipe snapshots in `~/.levain/registry/`
- Used for update detection (compare with current recipe)
- Removed before reinstall to force fresh install

### 5. **User Info Management**
**Location:** `src/lib/user_info/userinfo_util.ts`

- Prompts for user info on first install
- Stores: fullname, email, login, password
- Flags: `--ask-fullname`, `--ask-email`, etc.

---

## Key Configuration Properties

### File Locations
```typescript
levainHome              // Base directory (default: ~/.levain)
levainConfigDir         // Config directory (~/.levain)
levainConfigFile        // Main config (~/levain.config.json)
levainBackupDir         // Backups (~/.levain/backup)
levainCacheDir          // Cache (~/.levainCache)
levainRegistryDir       // Installed recipes (~/.levain/registry)
levainSafeTempDir       // Temp files (~/.levain/temp)
```

### Update Settings
```typescript
autoUpdate              // Auto-update Levain without prompt
shellCheckForUpdate     // Check package updates in shell command
lastUpdateQuestion      // Last date prompted for Levain update
lastKnownVersion        // Latest Levain version from GitHub
```

---

## Implementation Recommendations for Levain 2

### Priority 1: Backup Feature ⭐⭐⭐
**Critical for production use**

1. **Add backup directory management:**
   - Create `levainBackupDir` in Config
   - Generate timestamp tags: `bkp-YYYYMMDD-HHMMSS`

2. **Modify InstallService:**
   - Add `backupPreviousInstall()` method before installation
   - Copy old `baseDir` to backup location
   - Support `levain.preserveBaseDirOnUpdate` recipe flag

3. **Add manual cleanup command:**
   - `levain clean --backups --keep-days=7`
   - Delete backups older than N days

### Priority 2: Package Update Check ⭐⭐
**User experience improvement**

1. **Enhance Recipe/Package model:**
   - Add `updateAvailable` property
   - Compare installed recipe hash with current recipe hash
   - Store installed recipe snapshots in registry

2. **Modify InstallService:**
   - Check `updateAvailable` before installation
   - Prompt user if updates exist
   - Support `--force` and `--noUpdate` flags

3. **Add to ShellService:**
   - Optional update check before shell (configurable)

### Priority 3: Levain Self-Update ⭐
**Nice to have**

1. **Create UpdateCheckService:**
   - Check GitHub releases API for latest version
   - Compare with current version
   - Track last prompt date (once per day)

2. **Add config properties:**
   - `autoUpdate: boolean`
   - `lastUpdateQuestion: LocalDate`
   - `skipLevainUpdates: boolean` (command flag)

3. **Integrate with startup:**
   - Check on every command execution
   - Download and replace JAR file
   - Restart with new version

---

## Testing Strategy

### Backup Feature Tests
```java
@Test
void shouldBackupBeforeUpdate()
void shouldPreserveBaseDirWhenFlagSet()
void shouldDeleteOldBaseDirByDefault()
void shouldAbortOnBackupFailure()
void shouldCreateTimestampedBackupDir()
```

### Update Check Tests
```java
@Test
void shouldDetectUpdateAvailable()
void shouldPromptUserForUpdate()
void shouldSkipUpdateWithNoUpdateFlag()
void shouldForceUpdateWithForceFlag()
void shouldCompareRecipeHashes()
```

### Self-Update Tests
```java
@Test
void shouldCheckGitHubForUpdates()
void shouldPromptOncePerDay()
void shouldAutoUpdateWhenConfigured()
void shouldSkipWithSkipUpdatesFlag()
void shouldValidateReleaseVersion()
```

---

## Summary

**All three features are production-ready in original Levain:**

1. ✅ **Backup on update** - Robust, tested, handles edge cases
2. ✅ **Package update check** - User-friendly, configurable
3. ✅ **Levain self-update** - Automatic, GitHub-integrated

**Recommended implementation order for Levain 2:**
1. Backup feature (Priority 1 - essential for safety)
2. Package update check (Priority 2 - UX improvement)  
3. Self-update (Priority 3 - convenience feature)

**Estimated effort:**
- Backup: 2-3 days (with tests)
- Update check: 3-4 days (with registry management)
- Self-update: 2-3 days (with GitHub API integration)
