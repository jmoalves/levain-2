# Levain 2 Roadmap

This document tracks features from the original [levain](https://github.com/jmoalves/levain) (TypeScript/Deno) that are missing in levain-2 (Java), planned features, and development improvements.

## Legend

- ✅ **Completed** - Feature fully implemented
- 🔄 **In Progress** - Currently being worked on
- 📋 **Planned** - Scheduled for future implementation
- 🔍 **Under Review** - Being evaluated for inclusion
- ❌ **Won't Implement** - Decided against implementation

---

## Core Features Status

### Commands

| Command | Status | Notes |
|---------|--------|-------|
| `list` | ✅ | Recipe listing with filtering |
| `install` | ✅ | Package installation with dependencies |
| `shell` | ✅ | Opens configured shell environment |
| `config` | ✅ | Configuration management (repo add/list/remove) |
| `clean` | 📋 | Cache, backup, temp, and log cleanup |
| `actions` | 📋 | List available recipe actions |
| `info` | 📋 | Display system and environment information |
| `explain` | 📋 | Show recipe details and action list |
| `clone` | 📋 | Clone recipe repositories |
| `update` | 📋 | Update packages and recipes |

### Recipe Actions

| Action | levain (original) | levain-2 | Priority |
|--------|------------------|----------|----------|
| `addPath` | ✅ | ✅ | ✅ Done |
| `addToDesktop` | ✅ | ❌ | 📋 Medium |
| `addToStartMenu` | ✅ | ❌ | 📋 Medium |
| `addToStartup` | ✅ | ❌ | 📋 Low |
| `assertContains` | ✅ | ❌ | 📋 Low |
| `backupFile` | ✅ | ❌ | 📋 Low |
| `checkChainDirExists` | ✅ | ❌ | 📋 Low |
| `checkFileExists` | ✅ | ❌ | 📋 Low |
| `checkPort` | ✅ | ❌ | 📋 Medium |
| `checkUrl` | ✅ | ❌ | 📋 Medium |
| `clone` (git) | ✅ | ❌ | 📋 High |
| `contextMenu` | ✅ | ❌ | 📋 Medium |
| `contextMenuRemove` | ✅ | ❌ | 📋 Low |
| `copy` | ✅ | ✅ | ✅ Done |
| `defaultPackage` | ✅ | ❌ | 📋 Medium |
| `echo` | ✅ | ❌ | 📋 High |
| `extract` | ✅ | ✅ | ✅ Done |
| `inspect` | ✅ | ❌ | 📋 Medium |
| `jsonGet` | ✅ | ❌ | 📋 Medium |
| `jsonSet` | ✅ | ❌ | 📋 Medium |
| `jsonRemove` | ✅ | ❌ | 📋 Low |
| `killProcess` | ✅ | ❌ | 📋 Low |
| `levainShell` | ✅ | ✅ | ✅ Done |
| `mavenCopy` | ✅ | ❌ | 📋 Low |
| `mkdir` | ✅ | ✅ | ✅ Done |
| `propertyGet` | ✅ | ❌ | 📋 Medium |
| `propertySet` | ✅ | ❌ | 📋 Medium |
| `removeFromRegistry` | ✅ | ❌ | 📋 Low |
| `setEnv` | ✅ | ✅ | ✅ Done |
| `setVar` | ✅ | ✅ | ✅ Done |
| `shellPath` | ✅ | ❌ | 📋 Medium |
| `template` | ✅ | ❌ | 📋 Medium |

### Shell Environment Integration

| Feature | Status | Notes |
|---------|--------|-------|
| Environment variable setting | ✅ | `setEnv` action implemented |
| PATH manipulation | ✅ | `addPath` action implemented |
| Shell action execution | ✅ | Via `cmd.install` in recipes |
| Interactive shell | ✅ | `levain shell` command |
| Package environment isolation | ✅ | Each shell loads only specified packages |
| Custom shell path | ❌ | 📋 `shellPath` action needed |
| Shell variable substitution | 🔄 | Partial - needs expansion |

### Repository Features

| Feature | Status | Notes |
|---------|--------|-------|
| Resource Repository (JAR) | ✅ | Built-in recipes |
| Directory Repository | ✅ | Local filesystem |
| Git Repository | ✅ | Auto-clone with caching |
| ZIP Repository | ✅ | Auto-extract |
| Remote HTTP/HTTPS | ✅ | Direct recipe loading |
| Multiple repositories | ✅ | Chain with deduplication |
| Repository configuration | ✅ | Add/remove/list via CLI |
| Temporary repositories | 🔄 | `--tempRepo` flag exists, needs implementation |
| Repository mirrors | 📋 | For Nexus/proxy support |

---

## Missing Features (High Priority)

### 1. Recipe Actions System 📋

The original levain has a rich action system for recipes. Levain-2 needs:

**High Priority Actions (remaining):**
- `clone` - Git clone repositories
- `echo` - Display messages during installation

**Medium Priority Actions:**
- `template` - Template file generation with variable substitution
- `inspect` - Inspect files for patterns
- `jsonGet`/`jsonSet` - JSON manipulation
- `propertyGet`/`propertySet` - Properties file manipulation
- `checkPort`/`checkUrl` - Validation checks
- `contextMenu` - Windows context menu integration
- `shellPath` - Configure default shell path

### 2. CLI Commands 📋

**Missing Commands:**
- `clean` - Cleanup caches, backups, temp files, logs
- `actions` - List available recipe actions
- `info` - Display system information
- `explain` - Explain recipe contents and actions
- `update` - Update installed packages
- `clone` - Clone recipe repositories

### 3. Configuration System Enhancements 📋

- **Auto-update** - Automatic levain version checking
- **Shell check for updates** - Update check when opening shell
- **Custom environment variables** - User-defined env vars in config
- **Default package** - Default package for shell when none specified
- **Levain home directory** - Configurable via `--levainHome`
- **Cache directory** - Configurable via `--levainCache`

### 4. Recipe Format Extensions 📋

- **`cmd.shell` actions** - Commands to run when opening shell
- **`cmd.env` actions** - Environment setup commands
- **`validate` command** - Post-install validation
- **Variable substitution** - `${var}`, `${pkg.name}`, etc.
- **Conditional actions** - OS-specific or conditional execution

### 5. Windows Integration 📋

- **Context menu integration** - Right-click "Open Levain Shell Here"
- **Desktop shortcuts** - `addToDesktop` action
- **Start menu integration** - `addToStartMenu` action
- **Startup programs** - `addToStartup` action
- **Permanent environment variables** - `setEnv --permanent`
- **Windows Registry operations** - For deep integration

---

## Development & CI/CD Improvements

### GitHub Actions 📋

**Missing Workflows from Original Levain:**
- ❌ `checkSources.yml` - Code quality checks (lint, format)
- ❌ `test-e2e.yml` - End-to-end integration tests
- ❌ `test-unit.yml` - Separate unit test workflow

**Current Workflows:**
- ✅ `maven.yml` - Build, test, and coverage (combine unit + e2e)
- ✅ `release.yml` - Release automation
- ✅ `cleanup.yml` - Artifact cleanup

**Proposed New Workflows:**
- 📋 **Code Quality** - Checkstyle, SpotBugs, PMD
- 📋 **Dependency Check** - OWASP dependency vulnerability scanning
- 📋 **Performance Tests** - Installation and shell performance benchmarks
- 📋 **Multi-OS Testing** - Test on Windows, Linux, macOS
- 📋 **Docker Build** - Container image for levain

### Testing Improvements 📋

- **E2E test separation** - Dedicated workflow for integration tests
- **Multi-OS testing** - Windows and Linux test matrices
- **Performance benchmarks** - Track installation speed
- **Recipe validation tests** - Test all built-in recipes
- **Shell integration tests** - Test shell environment setup
- **Coverage targets** - Maintain >95% code coverage

### Code Quality 📋

- **Static analysis** - Checkstyle, SpotBugs, PMD integration
- **Code formatting** - Enforce consistent style
- **Dependency scanning** - Security vulnerability detection
- **License compliance** - Verify dependency licenses
- **Documentation checks** - Javadoc completeness

---

## Recipe Usage Analysis (levain-pkgs + incubation)

### levain-pkgs (production recipes)
Top actions in use (counted from cmd.install/cmd.env/cmd.shell entries):
- `addPath` (411)
- `setEnv` (378)
- `extract` (267)
- `setVar` (8)
- `levainShell` (4)
- `copy` (3)

**Implication:** All common actions used by levain-pkgs are implemented. Next priority is `clone` and `echo` for broader recipe coverage.

### incubation/bnd-levain-pkg (legacy recipes)
Additional actions observed:
- `addToDesktop`, `addToStartMenu`, `addToStartup`
- `assertContains`, `backupFile`, `checkChainDirExists`, `checkFileExists`
- `clone`, `contextMenu`, `defaultPackage`, `jsonSet`, `mavenCopy`
- `removeFromRegistry`, `template`

**Implication:** These are legacy/experimental actions. Implement as needed based on target audience.

---

## Documentation Redundancy Check

Potential overlaps to consolidate:
- README installation section overlaps with docs/installation/INSTALLATION.md
- DISTRIBUTION.md overlaps with docs/build/BUILD_RELEASES.md (build/release instructions)
- IMPLEMENTATION_ROADMAP.md overlaps with ROADMAP.md (feature tracking)

**Suggested cleanup:** choose a single canonical doc for each topic and link to it from README.

---

## Planned Features (New)

### 1. Package Update Management 📋

- **Version tracking** - Track installed package versions
- **Update notifications** - Notify when updates available
- **Selective updates** - Update specific packages
- **Rollback support** - Revert to previous versions
- **Update all** - `levain update --all`

### 2. Dependency Visualization 📋

- **Dependency tree** - `levain info --tree <package>`
- **Circular dependency detection** - Warn about circular deps
- **Dependency graph export** - Export to DOT/GraphViz

### 3. Recipe Validation 📋

- **Recipe linting** - Validate recipe YAML syntax
- **Action validation** - Check if actions exist
- **Dependency validation** - Check if dependencies exist
- **Recipe testing** - `levain test <recipe>`

### 4. Plugin System 🔍

- **Custom repository types** - Extensible repository implementations
- **Custom actions** - User-defined recipe actions
- **Event hooks** - Pre/post install hooks
- **Language plugins** - Support for other languages

### 5. Performance Optimization 📋

- **Parallel installation** - Install independent packages in parallel
- **Incremental caching** - Smarter cache management
- **Download resume** - Resume interrupted downloads
- **Repository caching** - Cache repository metadata

### 6. User Experience 📋

- **Interactive mode** - Guided installation wizard
- **Progress indicators** - Better visual feedback
- **Colored output** - Syntax highlighting for logs
- **Tab completion** - Shell completion for bash/zsh
- **Configuration wizard** - Interactive config setup

---

## Won't Implement ❌

These features from the original levain are not planned for levain-2:

- **Deno-specific features** - levain-2 is Java-based
- **TypeScript integration** - Not relevant for Java version
- **Web UI** (`inception/webui`) - Out of scope for CLI tool
- **Some Windows-only utilities** - Unless critical demand

---

## Implementation Priority

### Phase 1 (Next Release - v2.1.0) 🔄
Focus: Core recipe actions and shell environment

- [x] Implement `addPath` action
- [x] Implement `setEnv` action  
- [x] Implement `levainShell` action
- [ ] Implement `echo` action
- [x] Implement `copy` action
- [x] Implement `mkdir` action
- [x] Implement `extract` action
- [ ] Add `clean` command
- [ ] Add `info` command
- [ ] Improve shell environment variable handling

### Phase 2 (v2.2.0) 📋
Focus: Advanced recipe capabilities

- [ ] Implement `template` action
- [x] Implement `setVar` action
- [ ] Implement `inspect` action
- [ ] Add `explain` command
- [ ] Add `update` command
- [x] Variable substitution in recipes (`${var}`)
- [ ] Recipe validation and linting

### Phase 3 (v2.3.0) 📋
Focus: Windows integration and utilities

- [ ] Implement `contextMenu` action
- [ ] Implement `addToDesktop` action
- [ ] Implement `addToStartMenu` action
- [ ] Implement Windows registry operations
- [ ] Implement permanent environment variables
- [ ] JSON/Properties manipulation actions

### Phase 4 (v3.0.0) 📋
Focus: Advanced features and optimization

- [ ] Package update management
- [ ] Dependency visualization
- [ ] Plugin system
- [ ] Parallel installation
- [ ] Performance optimizations
- [ ] Interactive configuration wizard

---

## GitHub Actions Enhancement Plan

### Short Term 📋

1. **Code Quality Workflow**
   ```yaml
   - Checkstyle validation
   - SpotBugs analysis
   - PMD code analysis
   - Javadoc generation
   ```

2. **E2E Test Separation**
   ```yaml
   - Separate e2e test workflow
   - Test matrix: Windows + Linux
   - Recipe installation tests
   - Shell integration tests
   ```

3. **Security Scanning**
   ```yaml
   - OWASP dependency check
   - CodeQL analysis
   - Dependency vulnerability scan
   ```

### Long Term 📋

1. **Multi-Platform Builds**
   - Docker image publication
   - Homebrew formula
   - Chocolatey package

2. **Performance Benchmarks**
   - Track installation speed over time
   - Memory usage profiling
   - Startup time measurement

3. **Automated Release**
   - Automatic changelog generation
   - Version bumping
   - Release notes from commits
   - Asset upload (JAR)

---

## Contributing

Missing a feature? Want to implement something from this roadmap?

1. Check if there's an existing issue for the feature
2. Create a new issue if needed, referencing this roadmap
3. Discuss the approach in the issue
4. Submit a PR with tests and documentation

See [DEVELOPMENT.md](docs/DEVELOPMENT.md) for development guidelines.

---

## Notes

- This roadmap is subject to change based on user feedback and priorities
- Features marked 🔍 "Under Review" need community input
- Check [Issues](https://github.com/jmoalves/levain-2/issues) for current work
- See [Projects](https://github.com/jmoalves/levain-2/projects) for sprint planning

**Last Updated:** February 3, 2026
