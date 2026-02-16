package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextMenuActionTest {

    @Test
    void shouldNoOpOnNonWindows() {
        Assumptions.assumeFalse(EnvironmentUtils.isWindows());

        ContextMenuAction action = new ContextMenuAction();
        assertDoesNotThrow(() -> action.execute(null, List.of("Open with Test", "echo %1")));
    }

    @Test
    void shouldHandleLegacyFlagsOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of(
                    "folders",
                    "--id=devEnv-cmd",
                    "--name=dev-env cmd",
                    "--cmd=levain shell"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("HKCU\\Software\\Classes\\Directory\\shell\\devEnv-cmd")));
            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("MUIVerb")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleTargetAndIconOnWindows() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of(
                    "--target=files",
                    "--icon=cmd.exe",
                    "My Action",
                    "run.exe"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("HKCU\\Software\\Classes\\*\\shell\\My Action")));
            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("Icon")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    private static class RecordingContextMenuAction extends ContextMenuAction {
        private final List<String> commands = new ArrayList<>();

        @Override
        protected void runRegAdd(List<String> command) {
            commands.add(String.join(" ", command));
        }
    }
}
