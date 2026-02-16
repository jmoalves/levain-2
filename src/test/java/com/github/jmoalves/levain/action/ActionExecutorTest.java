package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import com.github.jmoalves.levain.service.VariableSubstitutionService;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActionExecutorTest {

    @TempDir
    Path tempDir;

    @Mock
    Config config;

    @Mock
    Instance<Action> actionInstances;

    @Mock
    Action mockAction1;

    @Mock
    Action mockAction2;

    @Mock
    VariableSubstitutionService variableSubstitutionService;

    private ActionExecutor actionExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockAction1.name()).thenReturn("test-action");
        when(mockAction2.name()).thenReturn("another-action");
        
        when(actionInstances.stream()).thenReturn(Stream.of(mockAction1, mockAction2));
        when(actionInstances.iterator()).thenReturn(Arrays.asList(mockAction1, mockAction2).iterator());
        
        when(variableSubstitutionService.substitute(anyString(), any(ActionContext.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        actionExecutor = new ActionExecutor(actionInstances, variableSubstitutionService);
    }

    @Test
    void testExecuteCommandsWithNull() {
        ActionContext context = createContext();
        assertDoesNotThrow(() -> actionExecutor.executeCommands(null, context));
    }

    @Test
    void testExecuteCommandsWithEmptyList() {
        ActionContext context = createContext();
        assertDoesNotThrow(() -> actionExecutor.executeCommands(List.of(), context));
    }

    @Test
    void testExecuteCommandsWithNullCommand() {
        ActionContext context = createContext();
        List<String> commands = Arrays.asList(null, "test-action arg1");
        
        assertDoesNotThrow(() -> actionExecutor.executeCommands(commands, context));
    }

    @Test
    void testExecuteCommandsWithBlankCommand() {
        ActionContext context = createContext();
        List<String> commands = Arrays.asList("", "   ", "test-action arg1");
        
        assertDoesNotThrow(() -> actionExecutor.executeCommands(commands, context));
    }

    @Test
    void testExecuteKnownAction() throws Exception {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action arg1 arg2");
        
        actionExecutor.executeCommands(commands, context);
        
        verify(mockAction1, times(1)).execute(eq(context), eq(Arrays.asList("arg1", "arg2")));
    }

    @Test
    void testExecuteUnknownAction() {
        ActionContext context = createContext();
        List<String> commands = List.of("unknown-action arg1");
        
        // Should log warning but not throw
        assertDoesNotThrow(() -> actionExecutor.executeCommands(commands, context));
    }

    @Test
    void testTokenizeSimpleCommand() {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action simple arg");
        
        assertDoesNotThrow(() -> actionExecutor.executeCommands(commands, context));
    }

    @Test
    void testTokenizeQuotedArguments() throws Exception {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action \"quoted arg\" 'single quoted'");
        
        actionExecutor.executeCommands(commands, context);
        
        verify(mockAction1, times(1)).execute(eq(context), argThat(args -> 
            args.size() == 2 && 
            args.get(0).equals("quoted arg") && 
            args.get(1).equals("single quoted")
        ));
    }

    @Test
    void testTokenizeMixedQuotes() throws Exception {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action normal \"quoted\" 'single' mixed");
        
        actionExecutor.executeCommands(commands, context);
        
        verify(mockAction1, times(1)).execute(eq(context), argThat(args -> args.size() == 4));
    }

    @Test
    void testExecuteMultipleCommands() throws Exception {
        ActionContext context = createContext();
        List<String> commands = Arrays.asList(
            "test-action arg1",
            "another-action arg2"
        );
        
        actionExecutor.executeCommands(commands, context);
        
        verify(mockAction1, times(1)).execute(eq(context), anyList());
        verify(mockAction2, times(1)).execute(eq(context), anyList());
    }

    @Test
    void testActionThrowsException() throws Exception {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action failing");
        
        doThrow(new RuntimeException("Action failed"))
            .when(mockAction1).execute(any(), any());
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            actionExecutor.executeCommands(commands, context);
        });
        
        assertTrue(exception.getMessage().contains("Action 'test-action' failed"));
    }

    @Test
    void testTokenizeEmptyQuotes() throws Exception {
        ActionContext context = createContext();
        List<String> commands = List.of("test-action \"\" ''");
        
        actionExecutor.executeCommands(commands, context);
        
        // Empty quotes are removed during tokenization, resulting in no arguments
        verify(mockAction1, times(1)).execute(eq(context), argThat(List::isEmpty));
    }

    @Test
    void testMultipleActionsRegistered() {
        // Verify both actions are registered
        assertNotNull(actionExecutor);
        
        ActionContext context = createContext();
        assertDoesNotThrow(() -> {
            actionExecutor.executeCommands(List.of("test-action"), context);
            actionExecutor.executeCommands(List.of("another-action"), context);
        });
    }

    private ActionContext createContext() {
        Recipe recipe = new Recipe();
        recipe.setName("test-recipe");
        return new ActionContext(config, recipe, tempDir, tempDir);
    }
}
