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
    void shouldThrowWhenPortClosed() throws Exception {
        CheckPortAction action = new CheckPortAction();
        int port;

        try (ServerSocket server = new ServerSocket(0)) {
            port = server.getLocalPort();
        }

        assertThrows(IllegalArgumentException.class,
                () -> action.execute(null, List.of("--timeout=200", String.valueOf(port))));
    }
}
