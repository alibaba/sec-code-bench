// Copyright (c) 2025 Alibaba Group and its affiliates

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//     http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software

// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.webapp.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;


public class TestHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;
    private final Set<String> requestedPaths = ConcurrentHashMap.newKeySet();

    public TestHttpServer() throws IOException {
        // Find a free port
        try (ServerSocket s = new ServerSocket(0)) {
            this.port = s.getLocalPort();
        }

        // Create and configure the server
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.createContext("/", new RequestRecorderHandler());
        this.server.setExecutor(Executors.newSingleThreadExecutor());
    }

    /**
     * Starts the server in a background thread.
     */
    public void start() {
        if (server != null) {
            server.start();
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Gets the URL for a specific path on this test server.
     *
     * @param path The path component of the URL (e.g., "/test").
     * @return The full URL string.
     */
    public String getEndpointUrl(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return String.format("http://127.0.0.1:%d%s", port, path);
    }

    /**
     * Checks if a request was received for a specific path.
     *
     * @param path The path to check.
     * @return True if a request was received for the given path, false otherwise.
     */
    public boolean hasRequestForPath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return requestedPaths.contains(path);
    }

    @Override
    public void close() {
        stop();
    }

    private class RequestRecorderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            requestedPaths.add(path);

            // Respond to the client to complete the HTTP exchange
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}