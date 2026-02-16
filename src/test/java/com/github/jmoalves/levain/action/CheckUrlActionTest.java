package com.github.jmoalves.levain.action;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckUrlActionTest {

    @Test
    void shouldValidateUrlStatus() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200));
        server.createContext("/missing", exchange -> respond(exchange, 404));
        server.start();

        String base = "http://localhost:" + server.getAddress().getPort();
        CheckUrlAction action = new CheckUrlAction();

        try {
            assertDoesNotThrow(() -> action.execute(null, List.of("--method=GET", base + "/ok")));
            assertThrows(IllegalArgumentException.class,
                    () -> action.execute(null, List.of("--method=GET", base + "/missing")));
        } finally {
            server.stop(0);
        }
    }

    private void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
