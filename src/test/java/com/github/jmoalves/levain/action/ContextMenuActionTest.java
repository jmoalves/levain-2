package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ContextMenuActionTest {

    @Test
    void shouldNoOpOnNonWindows() {
        Assumptions.assumeFalse(EnvironmentUtils.isWindows());

        ContextMenuAction action = new ContextMenuAction();
        assertDoesNotThrow(() -> action.execute(null, List.of("Open with Test", "echo %1")));
    }
}
