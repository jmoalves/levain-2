# Backup Strategy Analysis for Levain 2

**Analysis Date:** February 17, 2026  
**Topic:** Package Update Backup Implementation Strategy

---

## Original Levain Approach Analysis

### Current Implementation
```
~/.levain/backup/
  bkp-20260217-143052/
    maven-3.9/          # Full copy of old installation
    node-18/            # Full copy of old installation
```

**Process:**
1. Create timestamped backup directory
2. **Copy** entire `baseDir` to backup location
3. Delete old `baseDir` (unless `preserveBaseDirOnUpdate`)
4. Proceed with new installation
5. Manual cleanup via `clean` command

### Strengths ✅
- **Simple**: Easy to understand and implement
- **Complete**: Full backup ensures nothing is lost
- **Safe**: Original data preserved even if installation fails
- **Timestamped**: Clear history of when backups occurred
- **No dependencies**: Works on any filesystem

### Weaknesses ❌
- **Disk space intensive**: Full copy doubles space requirements temporarily
- **Performance**: Large packages (JDK, IDEs) take time to copy
- **Not atomic**: Failure during backup or install leaves partial state
- **Manual rollback**: User must manually restore from backup
- **No retention policy**: Backups accumulate until manually cleaned
- **Redundant work**: Copy then delete instead of move
- **Race conditions**: If process crashes mid-backup, state is unclear

### Risk Assessment
**Failure Scenarios:**
1. ❌ Backup copy fails → Installation aborted (good)
2. ❌ Backup succeeds, old `baseDir` deletion fails → Installation aborted (disk space leaked)
3. ❌ Installation fails after backup → User has backup but broken install
4. ❌ Process crashes mid-backup → Partial backup, unclear state

---

## Alternative Approaches

### Option 1: Atomic Swap (Blue/Green Deployment) ⭐ **RECOMMENDED**

**Concept:** Install new version to temporary location, then atomically swap with old version.

```
~/.levain/packages/
  maven/                    # Symlink or marker pointing to current version
  maven.20260217-143052/    # Old version (renamed, not copied)
  maven.20260217-150322/    # New version (being installed)
```

**Process:**
1. Install new package to **temporary versioned directory**: `maven.{timestamp}/`
2. Verify installation succeeded
3. **Atomically rename** current `maven/` → `maven.backup.{timestamp}/`
4. **Atomically rename** `maven.{timestamp}/` → `maven/`
5. Keep old backup for N days (or N versions)
6. Delete old backups during cleanup

**Code Flow:**
```java
// 1. Install to temp location
Path tempDir = Paths.get(levainHome, pkgName + ".new-" + timestamp);
installService.installTo(recipe, tempDir);

// 2. Verify installation
if (!verifyInstallation(tempDir)) {
    throw new InstallationException("Verification failed");
}

// 3. Atomic swap (all or nothing)
Path currentDir = Paths.get(levainHome, pkgName);
Path backupDir = Paths.get(levainHome, pkgName + ".backup-" + timestamp);

if (Files.exists(currentDir)) {
    Files.move(currentDir, backupDir, StandardCopyOption.ATOMIC_MOVE);
}
Files.move(tempDir, currentDir, StandardCopyOption.ATOMIC_MOVE);

// 4. Cleanup old backups (keep last 3)
cleanupOldBackups(pkgName, keepCount = 3);
```

**Strengths ✅:**
- **Atomic operations**: Either succeeds completely or fails safely
- **Efficient**: Move operations are instant (same filesystem)
- **Easy rollback**: Just swap the directories back
- **No disk space overhead**: Only during installation
- **Automatic retention**: Keep last N versions
- **Clear state**: Always know what's current vs backup
- **Production-proven**: Used by Kubernetes, Docker, etc.

**Weaknesses ❌:**
- **Slightly more complex**: Need to manage versioned directories
- **Same filesystem requirement**: Moves must be on same partition
- **Directory name conventions**: Need consistent naming

**Risk Mitigation:**
- ✅ Installation fails → Temp directory deleted, current untouched
- ✅ Swap fails → Both directories exist, can retry or rollback
- ✅ Process crashes → Temp directory orphaned but current intact

---

### Option 2: Symlink-Based Versioning

**Concept:** Multiple versions coexist, symlink points to active version.

```
~/.levain/packages/
  maven -> maven-3.9.9/     # Symlink to current version
  maven-3.9.9/              # Current installation
  maven-3.9.8/              # Previous version (backup)
  maven-3.8.6/              # Older version
```

**Process:**
1. Install to `maven-{version}/` directory
2. Update symlink: `maven -> maven-{version}/`
3. Keep last N versions
4. Rollback: Just change symlink

**Strengths ✅:**
- **Trivial rollback**: Change symlink instantly
- **Multiple versions**: Can keep entire history
- **Version clarity**: Directory name shows version

**Weaknesses ❌:**
- **Windows complexity**: Symlinks require admin privileges on Windows (or Developer Mode)
- **Version conflicts**: What if user manually edits files in active version?
- **Space usage**: All versions kept until cleanup
- **PATH complexity**: Need to handle symlinked paths in PATH variables

**Verdict:** ❌ **Not recommended** due to Windows symlink limitations

---

### Option 3: Copy-on-Write Filesystem Snapshots

**Concept:** Use filesystem-level snapshots (ZFS, Btrfs, APFS, Windows VSS).

**Process:**
1. Create filesystem snapshot of `baseDir`
2. Proceed with installation
3. Rollback: Restore from snapshot

**Strengths ✅:**
- **Instant snapshots**: No copy time
- **Space efficient**: Only stores changes (CoW)
- **OS-level support**: Native filesystem feature

**Weaknesses ❌:**
- **Platform-dependent**: Different APIs for each OS
- **Not portable**: Requires specific filesystems
- **User control**: User may not have CoW filesystem
- **Complexity**: Need to detect filesystem type, use different APIs

**Verdict:** ❌ **Not recommended** - too many platform assumptions

---

### Option 4: Transaction Journal

**Concept:** Record all changes in a log, replay in reverse to rollback.

**Process:**
1. Start transaction log
2. Record: "DELETE file X", "CREATE file Y", "MODIFY file Z"
3. Perform installation
4. Commit transaction
5. Rollback: Replay log in reverse

**Strengths ✅:**
- **Efficient**: Only log changes, no copying
- **Precise rollback**: Exact reverse operations

**Weaknesses ❌:**
- **Very complex**: Need to track all file operations
- **Fragile**: Log corruption = lost ability to rollback
- **Partial failures**: Hard to handle interruptions
- **Testing**: Need extensive test coverage

**Verdict:** ❌ **Not recommended** - too complex for benefit

---

### Option 5: Hybrid Approach (Original + Improvements)

**Concept:** Keep original approach but optimize performance and safety.

**Process:**
1. Check available disk space before backup
2. Create backup directory with timestamp
3. Use **hard links** instead of copy (when possible)
4. Proceed with installation
5. Delete hard links if successful (saves space)

**Strengths ✅:**
- **Simple**: Similar to original
- **Space efficient**: Hard links use no extra space
- **Backward compatible**: Can fall back to copy

**Weaknesses ❌:**
- **Hard link limitations**: Don't work across filesystems
- **Complexity**: Need to detect filesystem boundaries
- **Partial benefit**: Only saves space, not time

**Verdict:** 🟡 **Acceptable** but not as good as atomic swap

---

## Industry Best Practices

### Package Managers Comparison

| Tool | Approach | Rollback |
|------|----------|----------|
| **APT** | Download to cache, install from cache, keep .deb | Manual reinstall |
| **Homebrew** | Versioned directories + symlinks | `brew switch` |
| **Nix** | Content-addressable store, atomic symlinks | Instant (generation switch) |
| **Docker** | Layered images, atomic container swap | Instant (restart old container) |
| **Kubernetes** | Blue/Green deployments, rolling updates | Instant (switch service) |
| **SDKMAN** | Versioned directories + symlinks | `sdk use` |
| **npm** | No backup (reinstall from registry) | `npm install <pkg>@<version>` |

**Common pattern:** Atomic operations + versioned storage

---

## Critical Design Constraint Discovered ⚠️

### Issue 1: baseDir in Generated Config Files

**Problem:** Recipes use `${baseDir}` in template actions that generate config files **outside** the installation directory:

```yaml
# Maven recipe writes baseDir into user's .m2/settings.xml
- template --replace=/@@localRepo@@/g --with=${baseDir}\repository \
    ${pkgDir}/settings.xml ${home}\.m2\settings.xml

# DBeaver recipe writes baseDir into AppData config
- template --replace=/@@odbcLegacyPath@@/g --with=${baseDir} \
    ${pkgDir}\drivers.xml ${home}/AppData/Roaming/DBeaverData/.../drivers.xml
```

**Why Atomic Swap Breaks:**
1. Install to temp: `maven.new-20260217/` with `baseDir=maven.new-20260217/`
2. Template writes `C:\Users\John\.levain\packages\maven.new-20260217\repository` into `~/.m2/settings.xml`
3. Atomic rename: `maven.new-20260217/` → `maven/`
4. ❌ Config file now points to **non-existent** path `maven.new-20260217/`

**Impact:** ~25 recipes use `template` with `${baseDir}`, invalidating atomic swap approach.

### Issue 2: Windows File Locking

**Problem:** Files in use cannot be moved/renamed on Windows, but **can** be copied.

**Example:**
- User runs Maven from `~/.levain/packages/maven/bin/mvn.bat`
- `Files.move(maven/, maven.backup/)` → ❌ **FAILS** (file locked)
- Copy succeeds, but delete may fail (acceptable - backup exists)

**Why Original Levain Uses Copy+Delete:**
1. Copy always succeeds even if files are locked
2. Delete might fail, but that's after backup is safe
3. Windows file locking is common (running programs from install dir)

---

## Revised Recommendation

### **Hybrid Approach: Improved Copy-Based Backup with Rollback** ⭐

**Compromise:** Keep original copy-based approach but add improvements from atomic swap strategy.

### Implementation Plan

#### Phase 1: Core Backup Service (Improved Copy-Based)
```java
public class BackupService {
    private final Config config;
    private final Path backupRoot;
    
    public BackupResult backupAndInstall(Recipe recipe, Path targetDir) {
        BackupResult backup = null;
        
        try {
            // 1. Backup existing installation (if exists)
            if (Files.exists(targetDir)) {
                backup = backupCurrentInstallation(targetDir);
            }
            
            // 2. Clear installation directory for fresh install
            if (Files.exists(targetDir)) {
                deleteInstallationDirectory(targetDir); // May fail if files locked
            }
            
            // 3. Install to FINAL location (not temp)
            // baseDir resolves to correct path in generated configs
            installService.installTo(recipe, targetDir);
            
            // 4. Verify installation succeeded
            verifyInstallation(targetDir, recipe);
            
            // 5. Success - mark backup as successful
            if (backup != null) {
                markBackupAsSuccessful(backup);
            }
            
            return backup;
            
        } catch (Exception e) {
            // Installation failed - restore from backup if exists
            if (backup != null && backup.canRestore()) {
                log.warn("Installation failed, restoring from backup...");
                restoreFromBackup(backup, targetDir);
            }
            throw new InstallationException("Installation failed", e);
        }
    
    public void rollback(String packageName, String timestamp) {
        Path current = getPackageDir(packageName);
        Path backup = getBackupDir(packageName, timestamp);
        
        if (!Files.exists(backup)) {
            throw new RollbackException("Backup not found: " + backup);
        }
        
        log.info("Rolling back {} to backup from {}", packageName, timestamp);
        
        // 1. Delete current installation
        if (Files.exists(current)) {
            try {
                deleteRecursively(current);
            } catch (IOException e) {
                // Try rename if delete fails
                Path deletedDir = current.resolveSibling(
                    ".deleted." + current.getFileName() + "." + System.currentTimeMillis());
                Files.move(current, deletedDir);
            }
        }
        
        // 2. Copy backup to current location
        copyDirectory(backup, current);
        
        // 3. Verify rollback succeeded
        if (!Files.exists(current)) {
            throw new RollbackException("Rollback failed - current dir does not exist");
        }
        
        log.info("Successfully rolled back {} to {}", packageName, timestamp);
    }
    
    public List<BackupInfo> listBackups(String packageName) {
        Path packagesDir = getPackagesDir();
        Pattern backupPattern = Pattern.compile(
            Pattern.quote(packageName) + "\\.backup-(\\d{8}-\\d{6})");
        
        return Files.list(packagesDir)
            .filter(Files::isDirectory)
            .map(path -> {
                Matcher m = backupPattern.matcher(path.getFileName().toString());
                if (m.matches()) {
                    String timestamp = m.group(1);
                    long size = calculateDirectorySize(path);
                    return new BackupInfo(packageName, timestamp, path, size);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(BackupInfo::timestamp).reversed())
            .collect(Collectors.toList());
    }
    
    public void restoreFromBackup(BackupResult backup, Path targetDir) {
        if (!Files.exists(backup.getBackupPath())) {
            throw new RollbackException("Backup no longer exists: " + backup);
        }
        
        log.info("Restoring from backup: {}", backup.getBackupPath());
        
        // Delete failed installation
        if (Files.exists(targetDir)) {
            deleteRecursively(targetDir);
        }
        
        // Restore from backup
        copyDirectory(backup.getBackupPath(), targetDir);
        
        log.info("Restored from backup successfully");
        // Verify backup integrity
        verifyBackupIntegrity(currentDir, backupDir);
        
        return new BackupResult(backupDir, currentDir);
    }
    
    private void deleteInstallationDirectory(Path dir) {
        try {
            // Try to delete - may fail if files are locked (Windows)
            deleteRecursively(dir);
        } catch (IOException e) {
            log.warn("Unable to fully delete {}. Some files may be in use.", dir);
            log.debug("Delete error: {}", e.getMessage());
            
            // Rename to .deleted for cleanup later
            Path deletedDir = dir.resolveSibling(
                ".deleted." + dir.getFileName() + "." + System.currentTimeMillis());
            try {
                Files.move(dir, deletedDir);
                log.info("Renamed old installation to {} for later cleanup", deletedDir);
            } catch (IOException renameError) {
                // Can't even rename - files are locked
                // Installation will fail, backup will be restored
                throw new InstallationException(
                    "Cannot delete or rename old installation. " +
                    "Please close any programs using files in: " + dir, e);
            }
        }
    }
}
```

#### Phase 2: Rollback Support
```java
public class RollbackService {
    public void rollback(String packageName, String timestamp) {
        Path current = getPackageDir(packageName);
        Path backup = getBackupDir(packageName, timestamp);
        
        if (!Files.exists(backup)) {
            throw new RollbackException("Backup not found: " + backup);
        }
        
        // Swap current with backup
        Path temp = createTempDir();
        Files.move(current, temp, ATOMIC_MOVE);
        Files.move(backup, current, ATOMIC_MOVE);
        Files.move(temp, backup, ATOMIC_MOVE);always final location)
    maven.backup-20260217-1430/ # Backup from 2:30 PM (full copy)
    maven.backup-20260217-1200/ # Backup from 12:00 PM (full copy)
    .deleted.maven.1708181234/  # Old version being deleted (Windows locked files)
    node/
    node.backup-20260216-0900/
  temp/
    # No installation temp dirs - install directly to final location
```

**Key Changes from Atomic Swap:**
- IWhy Copy-Based is Actually Better for Levain

### Advantages Over Atomic Swap

1. **Handles locked files gracefully** (Windows programs running from install dir)
   - Copy succeeds even if files are in use
   - Delete may fail, but backup is safe
   
2. **Supports ${baseDir} in generated configs**
   - Install directly to final location
   - Template actions write correct paths
   - No post-swap fixup needed

3. **Simpler failure recovery**
   - Backup exists before installation starts
   - Failed install → just restore from backup
   - No partial state issues

4. **No cross-filesystem limitations**
   - Copy works across filesystems
   - Atomic move requires same filesystem

### Trade-offs Accepted

| Aspect | Copy-Based | Atomic Swap | Winner |
|--------|------------|-------------|---------|
| **Speed** | ~30s (Maven) | ~1s | Atomic |
| **Windows compatibility** | ✓ Handles locked files | ✗ Fails on locked files | Copy |
| **Config file paths** | ✓ Correct paths | ✗ Wrong paths | Copy |
| **Disk space** | 2x during install | 2x during install | Tie |
| **Rollback speed** | ~30s (copy back) | ~1s (rename) | Atomic |
| **Complexity** | Simple | Complex | Copy |
| **Cross-filesystem** | ✓ Works | ✗ Fails | Copy |

**Verdict:** Copy-based is better for Levain's use case despite slower performance.
### Directory Structure
```
~/.levain/
  packages/
    maven/                      # Current installation (symlink or directory)
    maven.backup-20260217-1430/ # Previous version (backup 1)
    maven.backup-20260217-1200/ # Older version (backup 2)
    node/
    node.backup-20260216-0900/
  temp/
    maven.installing-XYZ/       # Temp install location (deleted after success)
```

---

## Alternative: Fallback to Original Approach

**If atomic swap proves problematic**, we can implement the original approach with improvements:

### Enhanced Copy-Based Backup
```java
public class CopyBackupService {
    public BackupResult backupBeforeInstall(Path currentDir) {
        // 1. Check disk space
        long requiredSpace = calculateDirectorySize(currentDir) * 2;
        if (getAvailableSpace() < requiredSpace) {
            throw new InsufficientSpaceException();
        }
        
        // 2. Create backup with progress reporting
        Path backup = createBackupDir();
        copyWithProgress(currentDir, backup);
        
        // 3. Verify backup integrity
        verifyBackupIntegrity(currentDir, backup);
        
        return new BackupResult(true, backup);
    }
    
    private void copyWithProgress(Path src, Path dst) {
        // Stream copy with progress bar
        AtomicLong copied = new AtomicLong(0);
        long total = calculateDirectorySize(src);
        
        Files.walk(src).forEach(source -> {
            Path dest = dst.resolve(src.relativize(source));
            Files.copy(source, dest, COPY_ATTRIBUTES);
            copied.addAndGet(Files.size(source));
            updateProgress(copied.get(), total);
        });
    }
}
```

**Trade-offs:**
- ✅ Simpler implementation
- ✅ No dependency on atomic operations
- ❌ Slower (full copy)
- ❌ More disk space

---

## Testing Strategy
Enhanced Copy-Based Backup Strategy**

**Reasoning:**
1. ✅ **Handles ${baseDir} in generated configs** - Critical for 779 production recipes
2. ✅ **Windows file locking compatible** - Copy succeeds when move fails
3. ✅ **Simpler implementation** - Fewer edge cases than atomic swap
4. ✅ **Cross-filesystem support** - No same-filesystem requirement
5. ✅ **Safe failure recovery** - Backup exists before installation starts
6. ✅ **Production-tested** - Original Levain uses this successfully

**Why NOT Atomic Swap:**
- ❌ Breaks ${baseDir} resolution in template actions
- ❌ Fails on Windows when files are in use
- ❌ Requires complex post-swap path fixup
- ❌ Needs same filesystem for atomic moves

### Implementation Priority
1. ✅ **Phase 1:** Copy-based backup with progress reporting (2 days)
2. ✅ **Phase 2:** Rollback support (1 day)
3. ✅ **Phase 3:** Automatic cleanup (1 day)
4. ✅ **Phase 4:** Disk space checks and verification (1 day)
5. ✅ **Phase 5:** Handle locked files gracefully (1 day)

**Total effort:** 6-7 days with comprehensive testing

### Key Improvements Over Original Levain
- ✅ Disk space validation before backup
- ✅ Progress reporting for large packages
- ✅ Backup integrity verification
- ✅ Automatic rollback on installation failure
- ✅ Configurable retention policy
- ✅ Graceful handling of locked files (Windows)
- ✅ Programmatic rollback command

### Acceptance Criteria
- ✅ Installation fails safely (old version backed up and restorable)
- ✅ Rollback command works reliably
- ✅ Backups cleaned automatically by retention policy
- ✅ Works on Windows (with locked files), Linux, macOS
- ✅ Disk space validated before operations
- ✅ ${baseDir} resolves correctly in all recipes
- ✅ Progress reporting for operations > 5 second
- Installation to non-existent package (no backup needed)
- Cross-filesystem installs (atomic move fails)
- Insufficient disk space
- Simultaneous installs of same package
- Windows file locking during backup
- Backup directory permissions issues

---

## Performance Comparison

### Scenario: Maven 3.9 (350 MB installed)

| Approach | Backup Time | Disk Space | Rollback Time |
|----------|-------------|------------|---------------|
| **Original (copy)** | ~30s | 700 MB | Manual (~30s) |
| **Atomic swap** | ~1s | 700 MB | ~1s |
| **Hard links** | ~5s | 350 MB | Manual (~5s) |
| **Symlinks** | Instant | 350 MB | Instant |

**Winner: Atomic Swap** (best balance of speed, safety, compatibility)

---

## Conclusion

### Final Recommendation: **Atomic Swap Strategy**

**Reasoning:**
1. Industry-standard approach (Kubernetes, Docker, Homebrew)
2. Cross-platform compatible
3. Safe atomic operations
4. Fast (move vs copy)
5. Easy rollback
6. Reasonable complexity

### Implementation Priority
1. ✅ **Phase 1:** Basic atomic swap (2-3 days)
2. ✅ **Phase 2:** Rollback support (1 day)
3. ✅ **Phase 3:** Automatic cleanup (1 day)
4. ✅ **Phase 4:** Fallback to copy strategy (1 day)

**Total effort:** 5-6 days with comprehensive testing

### Acceptance Criteria
- ✅ Installation fails safely (old version untouched)
- ✅ Rollback works within 5 seconds
- ✅ Backups cleaned automatically
- ✅ Works on Windows, Linux, macOS
- ✅ Handles cross-filesystem scenarios
- ✅ 95%+ test coverage
