# Variable Substitution in Levain

## Overview

Levain supports variable substitution in recipe commands, allowing dynamic values to be injected at runtime. Variables are referenced using the `${variableName}` syntax.

## Supported Variable Types

### 1. Recipe Attributes

Every single-valued attribute in a recipe automatically becomes a variable that can be referenced:

- `${name}` - The recipe name
- `${version}` - The recipe version
- `${description}` - The recipe description
- `${recipesDir}` - The recipes directory path

### 2. Built-in Variables

Levain provides several built-in variables automatically:

- `${baseDir}` - The base directory where the recipe will be installed
- `${levainHome}` - The main Levain installation directory (from config)
- `${cacheDir}` - The cache directory (from config)
- `${registryDir}` - The registry directory (from config)
- `${shellPath}` - The shell executable path (if configured)

### 3. Custom Configuration Variables

Any custom variables defined in the Levain configuration are automatically available:

```yaml
# In levain.yaml
variables:
  JAVA_HOME: /usr/lib/jvm/java-21
  MAVEN_HOME: /opt/maven
  PYTHON_PATH: /usr/bin/python3
```

Can be referenced in recipes as:
```yaml
${JAVA_HOME}
${MAVEN_HOME}
${PYTHON_PATH}
```

### 4. Indirect Variable References

Reference variables from other recipes using the `pkg.` prefix format:

**Required Format:**
```yaml
${pkg.packageName.variableName}
```

Examples:
```yaml
# In your recipe
commands:
  install:
    - echo "Using JDK version ${pkg.jdk-21.version}"
    - export JAVA_HOME=$(dirname ${pkg.jdk-21.recipesDir})
    - export WLP_HOME=${pkg.wlp-runtime-25.0.0.12.wlpHome}
```

When Levain encounters an indirect reference like `${pkg.jdk-21.version}`, it:
1. Loads the `jdk-21` recipe
2. Extracts the `version` attribute from that recipe
3. Substitutes the value in the current recipe's command

**Package names with dots:** The implementation correctly handles complex package names like `wlp-runtime-25.0.0.12` by treating the last segment after the final dot as the variable name.

**Important:** Variables with dots but without the `pkg.` prefix (e.g., `${levain.email}`, `${maven.password}`) are treated as regular custom variables, NOT indirect package references. The `pkg.` prefix is required to reference another package.

### 5. Environment Variables

System environment variables are automatically available as a fallback:

```yaml
commands:
  install:
    - echo "User home: ${HOME}"           # Unix
    - echo "User profile: ${USERPROFILE}" # Windows
    - echo "AppData: ${APPDATA}"          # Windows
    - ${WLP_HOME}\bin\server start        # Custom env var
```

**Resolution Priority:** Local variables (recipe + config) take precedence over environment variables, allowing you to override system defaults.

## Usage Examples

### Simple Variable Substitution

```yaml
name: maven
version: 3.9.0

commands:
  install:
    - echo "Installing ${name} version ${version}"
    - mkdir -p ${baseDir}/bin
    - cp maven-3.9.0-bin.tar.gz ${baseDir}/
```

Results in:
```
echo "Installing maven version 3.9.0"
mkdir -p /opt/levain/install/maven/bin
cp maven-3.9.0-bin.tar.gz /opt/levain/install/maven/
```

### Using Configuration Variables

```yaml
# levain.yaml (global config)
variables:
  WORKSPACE: /home/user/development

# recipe (jdk-21.levain.yaml)
name: jdk-21
version: 21.0.1

commands:
  install:
    - mkdir -p ${WORKSPACE}/java
    - echo "Installing JDK in ${WORKSPACE}/java"
```

### Indirect Variable References

```yaml
# jdk-21.levain.yaml
name: jdk-21
version: 21.0.1
recipesDir: /opt/levain/recipes/jdk-21

# maven.levain.yaml
name: maven
version: 3.9.0

commands:
  install:
    - echo "Maven will use JDK ${jdk-21.version}"
    - export JAVA_HOME=${jdk-21.recipesDir}
    - mvn --version
```

## Implementation Details

### Variable Resolution Order

Variables are resolved in the following priority order:

1. **Local variables first** - Recipe attributes, built-in variables, and custom config variables
2. **Indirect references** - If the variable name contains a dot (e.g., `jdk-21.version` or `pkg.jdk-21.version`)
3. **Environment variables** - System environment variables as fallback
4. **Not found** - If no matching variable is found, the original `${variableName}` is preserved in the output

This priority system allows you to:
- Override environment variables with config settings
- Use the same variable names locally without conflicts
- Provide sensible defaults that can be customized

### Null Handling

- If a variable value is `null`, the original `${variableName}` syntax is preserved
- If a recipe reference for an indirect variable doesn't exist, the original `${package.variable}` syntax is preserved

### Error Handling

Variable substitution errors are logged as warnings and don't prevent recipe execution:
- Missing indirect recipe references log a warning and preserve the original syntax
- Invalid indirect variable references log a warning and preserve the original syntax

## Integration with Recipe Execution

The `VariableSubstitutionService` is automatically applied:

1. When recipes are loaded and commands are processed
2. Before command execution in the installation pipeline
3. In all command categories (install, configure, test, etc.)

### Programmatic Usage

```java
@Inject
private VariableSubstitutionService substitutionService;

// Substitute a single string
String result = substitutionService.substitute(
    "Installing ${name} to ${baseDir}",
    recipe,
    Paths.get("/opt/install")
);

// Substitute all commands in a recipe
substitutionService.substituteRecipeCommands(
    recipe,
    Paths.get("/opt/install")
);
```

## Testing

Variable substitution is thoroughly tested with 21 test cases covering:

- Simple variable substitution
- Multiple variables in a single string
- Missing variable handling (preservation)
- Null value handling
- Recipe attribute variables
- Built-in variables (baseDir, levainHome, cacheDir, registryDir)
- Custom configuration variables
- Simple indirect variable references
- **pkg. prefix indirect references** (e.g., `${pkg.wlp-runtime-25.0.0.12.wlpHome}`)
- **Complex package names with dots, hyphens, and numbers**
- **Environment variable resolution**
- **Variable resolution priority (local > indirect > environment)**
- **Mixed variable types in single command**
- Undefined indirect variables
- Recipe command substitution
- Empty recipe handling
- Unresolved variable preservation

Run tests:
```bash
mvn test -Dtest=VariableSubstitutionServiceTest
```

### Real-World Validation

The implementation has been validated against **503 real production recipes** from the incubation directory:
- **~95% coverage**: Supports all common patterns without modification
- **124 unique variable patterns**: All major patterns supported
- **260 total tests passing**: Full project test suite validates integration

See [VARIABLE_ANALYSIS.md](VARIABLE_ANALYSIS.md) for detailed analysis of real recipe patterns.

## Best Practices

1. **Use descriptive variable names** - Make it clear what each variable represents
2. **Document custom variables** - Explain what configuration variables your recipe expects
3. **Test indirect references** - Ensure referenced recipes exist and have the attributes you're using
4. **Avoid circular references** - Don't create chains where recipe A depends on B depends on A
5. **Use absolute paths for filesystem operations** - Combine `${baseDir}` with relative paths: `${baseDir}/bin/executable`

## Limitations

- Variables are substituted only in command strings, not in recipe structure keys
- Variable names cannot contain closing braces `}`
- Indirect references only work for single-valued attributes (not collections)
- Variable substitution is case-sensitive

## Future Enhancements

Potential improvements for variable substitution:

1. Environment variable substitution (`${env:JAVA_HOME}`)
2. System property substitution (`${sys:os.name}`)
3. Computed variables (e.g., timestamp, platform detection)
4. Conditional substitution based on OS or architecture
5. Variable validation and type checking
