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

## Recommendation for Levain 2

### **Primary Recommendation: Atomic Swap (Option 1)** ⭐

**Why:**
1. ✅ **Production-proven** pattern used by Kubernetes, Docker, and many tools
2. ✅ **Cross-platform** compatible (works on Windows, Linux, macOS)
3. ✅ **Efficient** (move instead of copy)
4. ✅ **Safe** (atomic operations prevent partial states)
5. ✅ **Easy rollback** (just swap directories)
6. ✅ **Space-aware** (only 2x space during install, not permanent)

### Implementation Plan

#### Phase 1: Core Backup Service
```java
public class BackupService {
    private final Config config;
    private final Path backupRoot;
    
    public BackupResult backupAndInstall(Recipe recipe, Path targetDir) {
        // 1. Create temp installation directory
        Path tempDir = createTempInstallDir(recipe);
        
        // 2. Install to temp location (isolated)
        installService.installTo(recipe, tempDir);
        
        // 3. Verify installation
        verifyInstallation(tempDir, recipe);
        
        // 4. Atomic swap
        return atomicSwap(targetDir, tempDir);
    }
    
    private BackupResult atomicSwap(Path current, Path newInstall) {
        Path backup = generateBackupPath(current);
        
        try {
            // Atomic operations (all or nothing)
            if (Files.exists(current)) {
                Files.move(current, backup, ATOMIC_MOVE);
            }
            Files.move(newInstall, current, ATOMIC_MOVE);
            
            return new BackupResult(true, backup);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback: Copy + Delete (not atomic but safe)
            return fallbackSwap(current, newInstall, backup);
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
        Files.move(temp, backup, ATOMIC_MOVE);
        
        log.info("Rolled back {} to {}", packageName, timestamp);
    }
    
    public List<BackupInfo> listBackups(String packageName) {
        // List all backup-{timestamp} directories
    }
}
```

#### Phase 3: Automatic Cleanup
```java
public class BackupCleanupService {
    public void cleanupOldBackups(String packageName, int keepCount) {
        List<Path> backups = findBackups(packageName);
        
        // Sort by timestamp (newest first)
        backups.sort(Comparator.comparing(this::extractTimestamp).reversed());
        
        // Keep only the most recent N backups
        backups.stream()
            .skip(keepCount)
            .forEach(this::deleteBackup);
    }
    
    public void cleanupByAge(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        
        findAllBackups().stream()
            .filter(backup -> isOlderThan(backup, cutoff))
            .forEach(this::deleteBackup);
    }
}
```

### Configuration
```yaml
# config.yaml
backup:
  enabled: true
  strategy: atomic-swap  # or: full-copy, none
  keepCount: 3           # Keep last 3 backups
  maxAge: 30d            # Delete backups older than 30 days
  retentionPolicy: count # or: age, both
  atomicFallback: true   # Fall back to copy if atomic move fails
```

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

### Unit Tests
```java
@Test
void shouldInstallToTempBeforeSwap()
void shouldSwapAtomically()
void shouldPreserveBackupOnFailure()
void shouldRollbackSuccessfully()
void shouldCleanupOldBackups()
void shouldFallbackIfAtomicNotSupported()
void shouldVerifyDiskSpaceBeforeBackup()
```

### Integration Tests
```java
@Test
void shouldHandleProcessCrashDuringInstall()
void shouldRecoverFromPartialSwap()
void shouldWorkAcrossFilesystems()
void shouldHandleWindowsFileLocking()
void shouldPreservePermissions()
```

### Edge Cases
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
