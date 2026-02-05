# Action Implementation Plan for Levain-2

## Executive Summary

Based on analysis of **production recipes** from the `incubation` directory and the `levain-pkgs` repository, this document identifies the minimum set of actions required to support the simplest real-world recipe installations.

**Key Finding**: The simplest recipes (JDK, Maven, Git) require only **5 core actions** to function:
1. `extract` ✅ (already implemented)
2. `copy` ✅ (already implemented)  
3. `mkdir` - **REQUIRED** (directory creation)
4. `setEnv` - **REQUIRED** (environment variables)
5. `addPath` - **REQUIRED** (PATH manipulation)

Plus optional: `echo`, `setVar`, `levainShell`

---

## Current Implementation Status

### Already Implemented ✅
- **CopyAction** (21 comprehensive tests)
  - Local file copying with relative/absolute paths
  - Remote file downloads via FileCache
  - Parent directory auto-creation
  - Binary file support

- **ExtractAction** (21 comprehensive tests)
  - ZIP, TAR.GZ, 7Z formats
  - Strip directory option
  - Type override for misnamed files
  - All error scenarios

**Total Test Coverage: 435 tests** (up from 374 at session start)

---

## Analysis of Production Recipes

### Simplest Real Recipe: Maven 3.9

```yaml
version: 3.9.9
dependencies:
    - jdk-8-default

cmd.install:
    - extract --strip ${downloadUrl}/${version}/binaries/apache-maven-${version}-bin.zip ${baseDir}
    - setEnv --permanent M2_HOME ${baseDir}
    - addPath --permanent ${baseDir}/bin

cmd.env:
    - setVar mavenHome ${baseDir}
    - setEnv M2_HOME ${baseDir}
    - addPath ${baseDir}/bin
```

**Actions Required:**
1. ✅ `extract` - Download and extract zip archive
2. 🔴 `setEnv` - Set M2_HOME environment variable (permanent)
3. 🔴 `addPath` - Add bin directory to PATH
4. 🔴 `setVar` - Set local variable mavenHome

### Simplest JDK Installation

```yaml
version: 11.0.25
description: "Eclipse Temurin JDK 11 LTS"

commands:
  install:
    - extract https://github.com/adoptium/temurin11-binaries/releases/download/...OpenJDK11U-jdk_x64_linux.tar.gz ${levainHome}/jdk-11
  shell:
    - setenv JAVA_HOME ${levainHome}/jdk-11
    - addPath ${JAVA_HOME}/bin
```

**Actions Required:**
1. ✅ `extract` - Download and extract tar.gz
2. 🔴 `setenv` / `setEnv` - Set JAVA_HOME variable
3. 🔴 `addPath` - Add JDK bin to PATH

---

## Action Priority & Complexity Analysis

### PHASE 1: Critical (Needed for Maven/JDK/Git Recipes)

#### 1. **setEnv** - Set Environment Variables
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 CRITICAL  
**Complexity**: ⭐⭐ (Medium)  

**Usage Pattern**:
```yaml
- setEnv --permanent M2_HOME ${baseDir}
- setenv JAVA_HOME ${levainHome}/jdk-11      # Note: both spellings used
- setEnv M2_REPO ${home}/.m2/repository
```

**Requirements**:
- Set system environment variables (shell-specific)
- `--permanent` flag for persistent environment variables
- Cross-platform support (Windows: Registry, Linux/Mac: shell config files)
- Variable substitution support

**Implementation Approach**:
```
SetEnvAction implements Action {
  - Parse arguments: [--permanent] VAR_NAME VAR_VALUE
  - For Windows: Set in registry + Windows env vars
  - For Unix: Add to ~/.bashrc, ~/.zshrc, etc.
  - For permanent: Update shell config files
  - Non-permanent: Use shell command to set for current session
}
```

**Test Coverage Needed**: 12 tests
- Basic: set and read back variable
- Windows: Registry setting
- Unix: Shell config file modification
- Permanent flag: Config file persistence
- Non-permanent: Session-only variables
- Variable substitution in values
- Special characters in values
- Error: Invalid variable names
- Error: Missing arguments
- Different shells (bash, zsh, PowerShell)

---

#### 2. **addPath** - Modify PATH Environment Variable
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 CRITICAL  
**Complexity**: ⭐⭐⭐ (High - shell-specific)  

**Usage Pattern**:
```yaml
- addPath --permanent ${baseDir}/bin
- addPath ${JAVA_HOME}/bin
- addPath --prepend ${HOME}/.cargo/bin
```

**Requirements**:
- Add directory to system PATH
- `--permanent` flag for persistent PATH updates
- `--prepend` option to add to beginning (takes precedence)
- Windows PATH uses semicolons; Unix uses colons
- Cross-platform implementation

**Implementation Approach**:
```
AddPathAction implements Action {
  - Parse arguments: [--permanent] [--prepend] PATH_TO_ADD
  - For Windows: Update Registry HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment
  - For Unix: Prepend to PATH in ~/.bashrc, ~/.zshrc
  - Check if path already exists (avoid duplicates)
  - Handle absolute and relative paths
  - Validate path exists
}
```

**Test Coverage Needed**: 14 tests
- Basic: add single path
- Permanent: Registry/config file update
- Prepend: Path ordering verification
- Windows: Registry + PATH format (semicolons)
- Unix: .bashrc/.zshrc modification
- Duplicate detection: Don't add twice
- Path validation: Reject non-existent paths
- Relative vs absolute paths
- Variable substitution
- Multiple invocations: Ensure idempotency
- Special characters in paths

---

#### 3. **setVar** - Set Local Variables (In-Recipe)
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 HIGH  
**Complexity**: ⭐ (Simple)  

**Usage Pattern**:
```yaml
- setVar mavenHome ${baseDir}
- setVar javaHome ${pkg.jdk-21.baseDir}
```

**Requirements**:
- Store variables that can be referenced later in same recipe
- Variables available only within recipe context
- Support variable substitution in values
- Support indirect package references (${pkg.jdk-21.version})

**Implementation Approach**:
```
SetVarAction implements Action {
  - Parse arguments: VAR_NAME VAR_VALUE
  - Store in ActionContext's variable map
  - Support ${...} substitution in VAR_VALUE
  - Variables persist for duration of recipe installation
  - Make available in cmd.env and cmd.shell sections
}
```

**Test Coverage Needed**: 10 tests
- Basic: set and retrieve variable
- Variable substitution: ${...} in values
- Package references: ${pkg.*.attribute}
- Overwriting: Set same variable twice
- Recipe-level scope: Variables not visible in other recipes
- Context persistence
- Special characters in variable names (allowed?)
- Empty values
- Numeric values

---

#### 4. **mkdir** - Create Directories
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 HIGH  
**Complexity**: ⭐ (Simple)  

**Usage Pattern**:
```yaml
- mkdir ${baseDir}
- mkdir ${home}/.m2
- mkdir ${WLP_HOME}/etc/extensions
```

**Requirements**:
- Create single or multiple directories
- Recursive directory creation (mkdir -p)
- Support variable substitution
- Handle already-existing directories gracefully

**Implementation Approach**:
```
MkdirAction implements Action {
  - Parse arguments: PATH [PATH...]
  - Substitute variables in paths
  - Use Files.createDirectories() for recursive creation
  - Don't fail if directory already exists
  - Set appropriate permissions
}
```

**Test Coverage Needed**: 8 tests
- Basic: Create single directory
- Multiple: Create several directories
- Nested: Create a/b/c/d/e structure
- Existing: Don't fail if directory exists
- Variable substitution
- Already exists: Idempotency
- Permissions: Readable/writable
- Parent directory doesn't exist initially

---

### PHASE 2: Common (Used in ~20% of recipes)

#### 5. **echo** - Print Output
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 MEDIUM-HIGH  
**Complexity**: ⭐ (Simple)  

**Usage Pattern**:
```yaml
- echo "Installing Maven 3.9..."
- echo Downloading Eclipse Temurin JDK 11...
- echo Configuration complete
```

**Requirements**:
- Print messages to console
- Variable substitution in messages
- No complex formatting needed

---

#### 6. **levainShell** - Execute Shell Commands
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 MEDIUM  
**Complexity**: ⭐⭐⭐ (High - shell execution)  

**Usage Pattern**:
```yaml
- levainShell ${WLP_HOME}\bin\installUtility install --to=bndes ${extDir}\iew-userregistry-liberty-feature-${version}.esa
- levainShell --saveVar=maven.master --stripCRLF ${mavenHome}\bin\mvn --encrypt-master-password ${levain.password}
```

**Requirements**:
- Execute arbitrary shell commands
- Windows: PowerShell or cmd.exe
- Unix: bash or sh
- Variable substitution
- Optional: `--saveVar` to capture output
- Optional: `--stripCRLF` to remove line endings

---

### PHASE 3: Advanced (Used in <5% of recipes)

#### 7. **template** - File Template Processing
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 MEDIUM  
**Complexity**: ⭐⭐⭐⭐ (High)  

**Usage Pattern**:
```yaml
- template --replace=/@@mavenMaster@@/g --with=${maven.master} ${pkgDir}/maven-security.xml ${home}\maven-master-password.xml
```

---

#### 8. **clone** - Git Clone Repository
**Status**: 🔴 NOT IMPLEMENTED  
**Priority**: 📍 LOW-MEDIUM  
**Complexity**: ⭐⭐⭐ (Medium)  

---

## Recommended Implementation Sequence

### Week 1-2: Phase 1 Actions
**Goal**: Enable Maven 3.9 and JDK installation

1. **setEnv** (2-3 days)
   - Implement for current OS
   - Add cross-platform support
   - Comprehensive tests (12 tests)

2. **addPath** (2-3 days)
   - Implement PATH manipulation
   - Platform-specific code
   - Tests (14 tests)

3. **mkdir** (1 day)
   - Simple implementation
   - Tests (8 tests)

4. **setVar** (1 day)
   - Context variable storage
   - Tests (10 tests)

**Expected**: 435 → ~490 tests

### Week 3: Phase 2 Actions
**Goal**: Improve UX and enable more recipes

5. **echo** (0.5 day, 5 tests)
6. **levainShell** (2-3 days, 15 tests)

**Expected**: ~490 → ~510 tests

### Week 4: Validation & Optimization
- Test complete Maven/JDK installation flow
- Test with real levain-pkgs recipes
- Performance optimization
- Documentation

---

## Integration Points

### ActionContext Updates Needed
```java
// Store local recipe variables
Map<String, String> recipeVariables

// Provide method to resolve variables
String resolveVariable(String varName)

// Provide platform detection
Platform getPlatform()  // WINDOWS, LINUX, MAC
```

### Variable Substitution Integration
- Reuse existing VariableSubstitutor
- Handle `${setVar_variableName}` patterns
- Support `${pkg.*.attribute}` in action values

### Shell Service Integration
- Integrate with existing ShellService for command execution
- Handle different shell types (bash, PowerShell, cmd.exe)
- Stream output to console
- Handle return codes

---

## Risk Analysis

### High Risk
1. **Cross-platform PATH/environment handling** - Windows registry vs Unix shell files
2. **Shell detection and compatibility** - Different shells, shell versions
3. **Variable persistence** - Ensuring environment variables stick after installation

### Medium Risk
1. **setVar scope management** - Recipe-local variables in complex dependencies
2. **Command execution and output capture** - levainShell with --saveVar

### Low Risk
1. **mkdir** - Standard Java NIO support
2. **echo** - Simple console output
3. **setVar** - In-memory variable storage

---

## Success Criteria

✅ **MVP Success**: Maven 3.9 installation works end-to-end
- Extract action downloads and extracts ZIP
- setEnv sets M2_HOME
- addPath adds ${baseDir}/bin to PATH
- User can run `mvn --version` after installation

✅ **Full Success**: All Phase 1 actions implemented with >90% test coverage
- 20+ setEnv tests  
- 20+ addPath tests
- 10+ setVar tests
- 10+ mkdir tests
- Total: ~500+ tests

---

## Files to Modify/Create

### New Action Classes
- `src/main/java/com/github/jmoalves/levain/action/SetEnvAction.java`
- `src/main/java/com/github/jmoalves/levain/action/AddPathAction.java`
- `src/main/java/com/github/jmoalves/levain/action/MkdirAction.java`
- `src/main/java/com/github/jmoalves/levain/action/SetVarAction.java`
- `src/main/java/com/github/jmoalves/levain/action/EchoAction.java`
- `src/main/java/com/github/jmoalves/levain/action/LevainShellAction.java`

### New Test Classes
- `src/test/java/com/github/jmoalves/levain/action/SetEnvActionTest.java` (12 tests)
- `src/test/java/com/github/jmoalves/levain/action/AddPathActionTest.java` (14 tests)
- `src/test/java/com/github/jmoalves/levain/action/MkdirActionTest.java` (8 tests)
- `src/test/java/com/github/jmoalves/levain/action/SetVarActionTest.java` (10 tests)
- `src/test/java/com/github/jmoalves/levain/action/EchoActionTest.java` (5 tests)
- `src/test/java/com/github/jmoalves/levain/action/LevainShellActionTest.java` (15 tests)

### Updates to Existing Files
- `ActionContext.java` - Add recipeVariables map
- `ActionExecutor.java` - Register new actions
- `VariableSubstitutor.java` - Ensure setVar variables are available

---

## References

- Maven 3.9 Recipe: `/incubation/bnd-levain-pkg/receitas/maven/maven-3.9.levain.yaml`
- JDK 11 Recipe: `/incubation/bnd-levain-pkg/receitas/jdk/jdk-8-default.levain.yaml`
- Original Levain: https://github.com/jmoalves/levain
- levain-pkgs: https://github.com/jmoalves/levain-pkgs

---

## Conclusion

The next phase should focus on **Phase 1 actions** (setEnv, addPath, mkdir, setVar) to enable real-world recipe installations. These are the minimum required actions to support the simplest production recipes like Maven and JDK.

**Estimated Timeline**: 2-3 weeks for Phase 1 + comprehensive tests
**Expected Test Coverage**: 435 → 510+ tests  
**Production Readiness**: Can install Maven 3.9, JDK, and basic packages after Phase 1
