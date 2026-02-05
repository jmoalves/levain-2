# Implementation Roadmap: Phase 1 Actions

## Overview

This document provides a detailed roadmap for implementing Phase 1 actions that are critical for Maven/JDK installation support.

---

## Action 1: mkdir (START HERE)

### Why Start Here?
- ✅ Simplest implementation (use Java NIO)
- ✅ No platform-specific code needed
- ✅ Fast feedback loop (1 day)
- ✅ Required by most recipes
- ✅ Good template for future actions

### Implementation

#### File: `MkdirAction.java`
```java
package com.github.jmoalves.levain.action;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Create one or more directories recursively.
 * 
 * Usage:
 *   - mkdir ${baseDir}
 *   - mkdir ${home}/.m2 ${home}/.ssh
 */
@ApplicationScoped
public class MkdirAction implements Action {
    
    @Override
    public String name() {
        return "mkdir";
    }
    
    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("mkdir requires at least one directory path");
        }
        
        for (String dirPath : args) {
            Path dir = FileUtils.resolve(context.getBaseDir(), dirPath);
            logger.debug("Creating directory: {}", dir);
            Files.createDirectories(dir);
        }
    }
}
```

#### File: `MkdirActionTest.java` (8 tests)
```java
class MkdirActionTest {
    @TempDir Path tempDir;
    
    // Test 1: Create single directory
    void testCreateSingleDirectory() throws Exception {
        // Create: mkdir ${baseDir}/test
        // Verify: ${baseDir}/test exists
    }
    
    // Test 2: Create multiple directories
    void testCreateMultipleDirectories() throws Exception {
        // Create: mkdir ${baseDir}/dir1 ${baseDir}/dir2
        // Verify: Both exist
    }
    
    // Test 3: Nested directory creation (mkdir -p behavior)
    void testCreateNestedDirectories() throws Exception {
        // Create: mkdir ${baseDir}/a/b/c/d/e
        // Verify: All levels created
    }
    
    // Test 4: Already exists - idempotency
    void testAlreadyExists() throws Exception {
        // Create: Files.createDirectory(dir)
        // Call: mkdir $dir
        // Verify: No exception, succeeds silently
    }
    
    // Test 5: Variable substitution
    void testVariableSubstitution() throws Exception {
        // Create: mkdir ${baseDir}/${name}
        // Where: name = "test123"
        // Verify: ${baseDir}/test123 exists
    }
    
    // Test 6: Relative paths
    void testRelativePaths() throws Exception {
        // Create: mkdir subdir/nested/path
        // Verify: Created relative to baseDir
    }
    
    // Test 7: Permissions
    void testPermissions() throws Exception {
        // Create: mkdir $dir
        // Verify: Directory is readable and writable
    }
    
    // Test 8: Error - no arguments
    void testNoArguments() throws Exception {
        // Call: mkdir
        // Verify: IllegalArgumentException thrown
    }
}
```

**Why This Action First?**
- No platform-specific code (works everywhere)
- No external dependencies
- Good learning opportunity for action lifecycle
- Immediately useful for recipes

---

## Action 2: setVar (NEXT)

### Implementation

#### File: `SetVarAction.java`
```java
/**
 * Set a local recipe variable that can be referenced later.
 * 
 * Usage:
 *   - setVar mavenHome ${baseDir}
 *   - setVar javaHome ${pkg.jdk-21.baseDir}
 */
@ApplicationScoped
public class SetVarAction implements Action {
    
    @Override
    public String name() {
        return "setVar";
    }
    
    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (args.size() != 2) {
            throw new IllegalArgumentException("setVar requires VAR_NAME and VAR_VALUE");
        }
        
        String varName = args.get(0);
        String varValue = args.get(1);
        
        // Resolve variables in the value
        String resolvedValue = context.resolveVariables(varValue);
        
        logger.debug("Setting variable: {} = {}", varName, resolvedValue);
        
        // Store in ActionContext for later use
        context.setRecipeVariable(varName, resolvedValue);
    }
}
```

#### Requires: ActionContext Update
```java
public class ActionContext {
    // NEW: Store recipe-local variables
    private Map<String, String> recipeVariables = new HashMap<>();
    
    // NEW: Get a recipe variable
    public String getRecipeVariable(String name) {
        return recipeVariables.get(name);
    }
    
    // NEW: Set a recipe variable
    public void setRecipeVariable(String name, String value) {
        recipeVariables.put(name, value);
    }
    
    // UPDATED: Resolve variables from multiple sources
    public String resolveVariables(String text) {
        // 1. Recipe variables (this.recipeVariables)
        // 2. Recipe attributes (${version}, ${name}, ${baseDir})
        // 3. Config variables
        // 4. Environment variables
        // Already handled by existing VariableSubstitutor
    }
}
```

#### Test Coverage (10 tests)
- Basic: set and retrieve
- Substitution: resolve ${baseDir} in value
- Package references: ${pkg.jdk-21.version}
- Overwrite: set same variable twice
- Scope: variables persist within recipe
- Empty values
- Numeric values
- Special characters in names
- Access in cmd.env section
- Access in cmd.shell section

---

## Action 3: setEnv (COMPLEX)

### Key Challenge: Cross-Platform Environment Variables

#### Implementation Strategy

**Windows:**
```
Registry path: HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment
Use: Java's WinReg library or ProcessBuilder with reg.exe
Permanent: Write to registry
Temporary: ProcessBuilder with set command
```

**Linux/macOS:**
```
Files: ~/.bashrc, ~/.zshrc, ~/.profile, ~/.bash_profile
Permanent: Append to shell config file
Temporary: Use export in current process (limited)
```

#### File: `SetEnvAction.java` (skeleton)
```java
@ApplicationScoped
public class SetEnvAction implements Action {
    
    @Override
    public String name() {
        return "setEnv";
    }
    
    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        boolean permanent = false;
        String varName;
        String varValue;
        
        // Parse arguments: [--permanent] VAR_NAME VAR_VALUE
        if (args.size() == 3 && "--permanent".equals(args.get(0))) {
            permanent = true;
            varName = args.get(1);
            varValue = args.get(2);
        } else if (args.size() == 2) {
            varName = args.get(0);
            varValue = args.get(1);
        } else {
            throw new IllegalArgumentException("setEnv [--permanent] VAR_NAME VAR_VALUE");
        }
        
        String resolvedValue = context.resolveVariables(varValue);
        
        if (permanent) {
            setPermanentEnvVar(varName, resolvedValue);
        } else {
            setTemporaryEnvVar(varName, resolvedValue);
        }
    }
    
    private void setPermanentEnvVar(String name, String value) {
        if (isWindows()) {
            setWindowsEnvironmentVariable(name, value);
        } else {
            setUnixEnvironmentVariable(name, value);
        }
    }
    
    private void setWindowsEnvironmentVariable(String name, String value) {
        // Use Registry API or ProcessBuilder with reg.exe
        // Write to: HKEY_LOCAL_MACHINE\System\...\Environment
    }
    
    private void setUnixEnvironmentVariable(String name, String value) {
        // Detect shell: bash, zsh, etc.
        // Append to: ~/.bashrc, ~/.zshrc, ~/.profile
        // Format: export VAR_NAME=value
    }
    
    private void setTemporaryEnvVar(String name, String value) {
        // Set in current process only
        System.setProperty(name, value);
        ProcessBuilder.environment().put(name, value);
    }
}
```

#### Test Coverage (12 tests)
- Basic: set and verify
- Windows: Registry write
- Linux: .bashrc modification
- macOS: .zshrc modification
- Permanent flag: Config persistence
- Temporary: Session-only variables
- Variable substitution: ${baseDir}
- Special characters in values
- Multiple calls: Idempotency
- Error cases: Missing arguments
- Different shells: bash vs zsh
- Overwrite existing variables

---

## Action 4: addPath (MOST COMPLEX)

### Key Challenges
1. **PATH separator differs**: Windows (`;`), Unix (`:`)
2. **Ordering matters**: --prepend adds to beginning
3. **Avoid duplicates**: Don't add same path twice
4. **Multiple shells**: bash, zsh, PowerShell
5. **Different locations**: Registry (Windows), shell files (Unix)

#### Implementation Strategy

```java
@ApplicationScoped
public class AddPathAction implements Action {
    
    @Override
    public String name() {
        return "addPath";
    }
    
    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        // Parse: [--permanent] [--prepend] PATH_TO_ADD
        boolean permanent = false;
        boolean prepend = false;
        String pathToAdd = null;
        
        int i = 0;
        while (i < args.size()) {
            if ("--permanent".equals(args.get(i))) {
                permanent = true;
                i++;
            } else if ("--prepend".equals(args.get(i))) {
                prepend = true;
                i++;
            } else {
                pathToAdd = args.get(i);
                i++;
            }
        }
        
        if (pathToAdd == null) {
            throw new IllegalArgumentException("addPath [--permanent] [--prepend] PATH");
        }
        
        Path resolved = FileUtils.resolve(context.getBaseDir(), pathToAdd);
        
        // Verify path exists
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("Path does not exist: " + resolved);
        }
        
        if (permanent) {
            addPathPermanent(resolved, prepend);
        } else {
            addPathTemporary(resolved, prepend);
        }
    }
    
    private void addPathPermanent(Path path, boolean prepend) {
        if (isWindows()) {
            addPathWindows(path, prepend);
        } else {
            addPathUnix(path, prepend);
        }
    }
    
    private void addPathWindows(Path path, boolean prepend) {
        // Read current PATH from registry
        // Check if path already exists
        // Add to beginning (prepend) or end (append)
        // Write back to registry
    }
    
    private void addPathUnix(Path path, boolean prepend) {
        // Detect shell and get config file
        // Read current PATH from file
        // Check if path already exists
        // Add or prepend as needed
        // Write back to config file
    }
    
    private void addPathTemporary(Path path, boolean prepend) {
        // Modify PATH in current process
        String currentPath = System.getenv("PATH");
        String separator = File.pathSeparator;
        String newPath;
        
        if (prepend) {
            newPath = path + separator + currentPath;
        } else {
            newPath = currentPath + separator + path;
        }
        
        ProcessBuilder.environment().put("PATH", newPath);
    }
}
```

#### Test Coverage (14 tests)
- Basic: add single path
- Permanent: Registry/file update (Windows/Unix)
- Prepend: Path ordering
- Windows PATH format: Uses `;` separator
- Unix PATH format: Uses `:` separator
- Duplicate detection: No duplicates
- Path validation: Reject non-existent paths
- Relative vs absolute: Both work
- Variable substitution: ${JAVA_HOME}/bin
- Multiple invocations: Idempotency
- Edge case: Empty PATH
- Edge case: Path with spaces
- Order preservation: Later additions go to correct position

---

## Integration Points

### ActionExecutor.java Update
```java
// Register all actions
private Map<String, Class<?>> actionClasses = Map.of(
    "copy", CopyAction.class,
    "extract", ExtractAction.class,
    "mkdir", MkdirAction.class,        // NEW
    "setVar", SetVarAction.class,      // NEW
    "setEnv", SetEnvAction.class,      // NEW
    "addPath", AddPathAction.class     // NEW
);
```

### ActionContext.java Update
```java
public class ActionContext {
    // NEW: Recipe-local variables
    private Map<String, String> recipeVariables = new HashMap<>();
    
    // NEW: Getter/setter
    public String getRecipeVariable(String name) {
        return recipeVariables.get(name);
    }
    
    public void setRecipeVariable(String name, String value) {
        recipeVariables.put(name, value);
    }
    
    // UPDATED: Variable resolution includes recipe variables
    public String resolveVariables(String text) {
        // Try recipe variables first
        for (Map.Entry<String, String> entry : recipeVariables.entrySet()) {
            text = text.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        // Then delegate to existing VariableSubstitutor
        return variableSubstitutor.substitute(text);
    }
}
```

### VariableSubstitutor.java Update
```java
// Add support for accessing recipe variables
public String substitute(String text, ActionContext context) {
    // This allows ${varName} to resolve from ActionContext
    // Or modify ActionContext to be accessible within VariableSubstitutor
}
```

---

## Testing Strategy

### Local Testing
```bash
# Test individual action
mvn test -Dtest=MkdirActionTest

# Test all Phase 1 actions
mvn test -Dtest=MkdirActionTest,SetVarActionTest,SetEnvActionTest,AddPathActionTest

# Full test suite
mvn clean test
```

### Integration Testing
```bash
# Test with real recipe
mvn test -Dtest=*IntegrationTest

# Or manually:
mvn clean package -DskipTests
java -jar target/levain-2-2.0.0-SNAPSHOT.jar install maven-3.9
```

---

## Success Criteria

### Unit Tests: 100% Pass Rate
- [ ] 8 mkdir tests pass
- [ ] 10 setVar tests pass
- [ ] 12 setEnv tests pass
- [ ] 14 addPath tests pass
- [ ] Total: 44 new tests

### Integration: Maven 3.9 Installs
```bash
levain install maven
# Should:
# 1. Extract Maven from URL
# 2. Set M2_HOME permanently
# 3. Add $M2_HOME/bin to PATH permanently
# 4. User can run: mvn --version
```

### Cross-Platform: All 3 OSes
- [ ] Windows: Registry modifications verified
- [ ] Linux: .bashrc file verified
- [ ] macOS: .zshrc file verified

### Code Quality
- [ ] All tests pass
- [ ] No compiler warnings
- [ ] Code coverage > 90%
- [ ] All new classes have javadoc

---

## Estimated Timeline

| Action | Days | Complexity | Blocker |
|--------|------|-----------|---------|
| mkdir | 1 | ⭐ | None |
| setVar | 1 | ⭐ | ActionContext update |
| setEnv | 2-3 | ⭐⭐ | Platform detection |
| addPath | 2-3 | ⭐⭐⭐ | PATH parsing, shell detection |
| **Total** | **6-8** | | |

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Shell not found | Detect common shells, fall back to default |
| Registry API issues (Windows) | Use ProcessBuilder with reg.exe as fallback |
| PATH already contains duplicate | Check before adding |
| Permission denied writing config files | Clear error message, document requirements |
| Different PATH on next shell session | Write to config files, not just environment |

---

## Next: Production Recipe Testing

Once Phase 1 is complete, test with real recipes:
1. `maven-3.9.levain.yaml` - Most important
2. `jdk-11.levain.yaml` - JDK setup
3. `git.levain.yaml` - Git installation
4. And 10+ more from incubation directory

This validates our implementations against real-world usage patterns.

---

**Estimated Completion**: 2 weeks  
**MVP Deliverable**: Maven 3.9 fully functional  
**Test Coverage Target**: 510+ tests (435 → 479)
