package com.github.jmoalves.levain.action;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckPortActionTest {

    @Test
    void shouldPassWhenPortOpen() throws Exception {
        CheckPortAction action = new CheckPortAction();

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            assertDoesNotThrow(() -> action.execute(null, List.of(String.valueOf(port))));
        }
    }

    @Test
    void shouldSupportPortFlag() throws Exception {
        CheckPortAction action = new CheckPortAction();

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            assertDoesNotThrow(() -> action.execute(null, List.of("--port=" + port)));
        }
    }

    @Test
    void shouldSupportHostAndPortFlags() throws Exception {
        CheckPortAction action = new CheckPortAction();

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            assertDoesNotThrow(() -> action.execute(null, List.of("--host=localhost", "--port", String.valueOf(port))));
        }
    }

    @Test
    void shouldRejectInvalidPortFlag() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--port=bad")));
    }

    @Test
    void shouldThrowWhenPortClosed() throws Exception {
        CheckPortAction action = new CheckPortAction();
        int port;

        try (ServerSocket server = new ServerSocket(0)) {
            port = server.getLocalPort();
        }

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--timeout=200", String.valueOf(port))));
    }

    @Test
    void shouldRejectMissingArgs() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of()));
    }

    @Test
    void shouldRejectInvalidTimeout() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--timeout=-1", "8080")));
    }

    @Test
    void shouldRejectNonNumericTimeout() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--timeout=abc", "8080")));
    }

    @Test
    void shouldRejectPortOutOfRange() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("70000")));
    }

    @Test
    void shouldRejectMultipleHostsWithPortFlag() {
        CheckPortAction action = new CheckPortAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--host=localhost", "--port=8080", "otherhost")));
    }
}
