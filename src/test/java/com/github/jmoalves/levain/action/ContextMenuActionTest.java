package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.util.EnvironmentUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void shouldRejectMissingArgs() {
        ContextMenuAction action = new ContextMenuAction();
        assertThrows(IllegalArgumentException.class, () -> action.execute(null, List.of()));
    }

    @Test
    void shouldRejectMissingTargetValue() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--target")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectInvalidTarget() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new RecordingContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--target=unknown", "Label", "cmd")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingCommand() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("Label")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingIconValue() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--icon", "Label", "cmd")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingCmdValue() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--cmd", "Label")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldUseIdAsLabelWhenPositionalsMissing() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target=files", "--id=my-id", "--cmd=run"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\*\\shell\\my-id")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleBackgroundTargetFromPositional() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("background", "Open", "cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("Directory\\Background\\shell")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleAllTargetAlias() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target=all", "Open", "cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\*\\shell")));
            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("Directory\\shell")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingNameValue() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--name")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldRejectMissingIdValue() {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            ContextMenuAction action = new ContextMenuAction();
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--id")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldJoinCommandFromExtraPositionals() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target", "files", "Label", "cmd", "arg"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("/d cmd arg")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleDirectoryTarget() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target", "directory", "Label", "cmd.exe"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\Directory\\shell\\")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleFolderTarget() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target", "directories", "--name=MyAction", "--cmd=echo test"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\Directory\\shell\\")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleFilesTarget() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target", "file", "Label", "cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\*\\shell\\")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleBackgroundTarget() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--target=background", "Label", "cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\Directory\\Background\\shell\\")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldSimplifyIdForRegistryKey() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--id", "My Action!", "--cmd=cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\shell\\My Action!")));
        } finally {
            System.setProperty("os.name", previous);
        }
    }

    @Test
    void shouldHandleIdWithMultipleSpecialChars() throws Exception {
        String previous = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 10");

        try {
            RecordingContextMenuAction action = new RecordingContextMenuAction();
            action.execute(null, List.of("--id", "Test@#$%", "--cmd=cmd"));

            assertTrue(action.commands.stream().anyMatch(cmd -> cmd.contains("\\shell\\Test@#$%")));
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
