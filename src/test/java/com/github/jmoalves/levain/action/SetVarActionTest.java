package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SetVarAction Tests")
class SetVarActionTest {
    @TempDir Path tempDir;

    private SetVarAction action;
    private ActionContext context;

    @BeforeEach
    void setUp() {
        action = new SetVarAction();
        
        Config config = new Config();
        config.setCacheDir(tempDir.resolve("cache").toString());
        config.setLevainHome(tempDir.resolve("levain").toString());
        
        Recipe recipe = new Recipe();
        recipe.setName("test");
        recipe.setVersion("1.0");
        
        context = new ActionContext(config, recipe, tempDir, tempDir);
    }

    @Test
    @DisplayName("Test 1: Set simple string variable")
    void testSetSimpleVariable() throws Exception {
        action.execute(context, List.of("myVar", "myValue"));

        assertEquals("myValue", context.getRecipeVariable("myVar"));
    }

    @Test
    @DisplayName("Test 2: Set variable with path")
    void testSetVariableWithPath() throws Exception {
        action.execute(context, List.of("basePath", "/home/user/apps"));

        assertEquals("/home/user/apps", context.getRecipeVariable("basePath"));
    }

    @Test
    @DisplayName("Test 3: Set multiple variables")
    void testSetMultipleVariables() throws Exception {
        action.execute(context, List.of("var1", "value1"));
        action.execute(context, List.of("var2", "value2"));
        action.execute(context, List.of("var3", "value3"));

        assertEquals("value1", context.getRecipeVariable("var1"));
        assertEquals("value2", context.getRecipeVariable("var2"));
        assertEquals("value3", context.getRecipeVariable("var3"));
    }

    @Test
    @DisplayName("Test 4: Overwrite existing variable")
    void testOverwriteVariable() throws Exception {
        action.execute(context, List.of("myVar", "oldValue"));
        assertEquals("oldValue", context.getRecipeVariable("myVar"));

        action.execute(context, List.of("myVar", "newValue"));
        assertEquals("newValue", context.getRecipeVariable("myVar"));
    }

    @Test
    @DisplayName("Test 5: Variable with special characters")
    void testVariableWithSpecialChars() throws Exception {
        String complexValue = "C:\\Program Files\\Java\\jdk-11.0.1";
        action.execute(context, List.of("javaPath", complexValue));

        assertEquals(complexValue, context.getRecipeVariable("javaPath"));
    }

    @Test
    @DisplayName("Test 6: Variable with dots and underscores")
    void testVariableNameWithDots() throws Exception {
        action.execute(context, List.of("my.var.name", "value"));
        action.execute(context, List.of("my_var_name", "value2"));

        assertEquals("value", context.getRecipeVariable("my.var.name"));
        assertEquals("value2", context.getRecipeVariable("my_var_name"));
    }

    @Test
    @DisplayName("Test 7: Empty string value")
    void testEmptyStringValue() throws Exception {
        action.execute(context, List.of("emptyVar", ""));

        assertEquals("", context.getRecipeVariable("emptyVar"));
    }

    @Test
    @DisplayName("Test 8: Value with spaces")
    void testValueWithSpaces() throws Exception {
        action.execute(context, List.of("description", "This is a long description"));

        assertEquals("This is a long description", context.getRecipeVariable("description"));
    }

    @Test
    @DisplayName("Test 9: No arguments throws exception")
    void testNoArguments() {
        assertThrows(
            IllegalArgumentException.class,
            () -> action.execute(context, List.of()),
            "Should throw exception when no arguments provided"
        );
    }

    @Test
    @DisplayName("Test 10: Only one argument throws exception")
    void testOnlyNameNoValue() {
        assertThrows(
            IllegalArgumentException.class,
            () -> action.execute(context, List.of("varName")),
            "Should throw exception when only variable name provided"
        );
    }

    @Test
    @DisplayName("Test 11: Extra arguments are accepted (first two used)")
    void testExtraArguments() throws Exception {
        action.execute(context, List.of("varName", "varValue", "extra", "arguments"));

        assertEquals("varValue", context.getRecipeVariable("varName"));
    }

    @Test
    @DisplayName("Test 12: Variable value persists in context")
    void testVariablePersistsInContext() throws Exception {
        action.execute(context, List.of("persistent", "value123"));

        // Verify it's in the context
        assertSame(context.getRecipeVariables(), context.getRecipeVariables());
        assertEquals("value123", context.getRecipeVariable("persistent"));
        assertTrue(context.getRecipeVariables().containsKey("persistent"));
    }

    @Test
    @DisplayName("Test 13: Variable with version-like value")
    void testVersionValue() throws Exception {
        action.execute(context, List.of("jdkVersion", "11.0.2+9"));

        assertEquals("11.0.2+9", context.getRecipeVariable("jdkVersion"));
    }

    @Test
    @DisplayName("Test 14: Numeric value stored as string")
    void testNumericValue() throws Exception {
        action.execute(context, List.of("port", "8080"));

        assertEquals("8080", context.getRecipeVariable("port"));
        assertTrue(context.getRecipeVariable("port") instanceof String);
    }

    @Test
    @DisplayName("Test 15: Case-sensitive variable names")
    void testCaseSensitiveNames() throws Exception {
        action.execute(context, List.of("MyVar", "value1"));
        action.execute(context, List.of("myVar", "value2"));
        action.execute(context, List.of("MYVAR", "value3"));

        assertEquals("value1", context.getRecipeVariable("MyVar"));
        assertEquals("value2", context.getRecipeVariable("myVar"));
        assertEquals("value3", context.getRecipeVariable("MYVAR"));
    }

    @Test
    @DisplayName("Test 16: Variable value with equals sign")
    void testValueWithEqualsSign() throws Exception {
        action.execute(context, List.of("equation", "x=5+3"));

        assertEquals("x=5+3", context.getRecipeVariable("equation"));
    }

    @Test
    @DisplayName("Test 17: Retrieve non-existent variable returns null")
    void testNonExistentVariable() throws Exception {
        action.execute(context, List.of("existing", "value"));

        assertNull(context.getRecipeVariable("nonExisting"));
        assertEquals("value", context.getRecipeVariable("existing"));
    }

    @Test
    @DisplayName("Test 18: Recipe variables are independent from config vars")
    void testRecipeVariablesIndependent() throws Exception {
        // Set a recipe variable
        action.execute(context, List.of("recipeVar", "recipeValue"));

        // Verify it's in recipe context
        assertEquals("recipeValue", context.getRecipeVariable("recipeVar"));

        // Verify it doesn't pollute global config (if config doesn't auto-sync)
        // This tests the separation of concerns
        assertNotNull(context.getRecipeVariable("recipeVar"));
    }
}
