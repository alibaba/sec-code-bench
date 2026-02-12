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

package com.example.util;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一个用于测试的轻量级HTTP服务器。
 * 它可以监听在本地随机端口上，并记录是否收到了请求。
 * 这对于模拟SSRF攻击的目标内网服务非常有用。
 */
public class TestHttpServer implements AutoCloseable {

    private HttpServer server;
    private final AtomicBoolean requestReceived = new AtomicBoolean(false);
    private int port;

    /**
     * 启动服务器。
     * 服务器将在一个随机的可用端口上启动。
     */
    public void start() throws IOException {
        // 自动查找一个可用端口
        try (ServerSocket s = new ServerSocket(0)) {
            this.port = s.getLocalPort();
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> {
            requestReceived.set(true);
            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("TestHttpServer started on port: " + port);
    }

    /**
     * 停止服务器。
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("TestHttpServer stopped.");
        }
    }

    /**
     * 检查服务器自启动以来是否收到过任何请求。
     * @return 如果收到过请求，则为true，否则为false。
     */
    public boolean hasRequestReceived() {
        return requestReceived.get();
    }

    /**
     * 获取服务器正在监听的URL。
     * @return 服务器的本地URL，例如 "http://127.0.0.1:12345"
     */
    public String getServerUrl() {
        if (server == null) {
            throw new IllegalStateException("Server not started yet.");
        }
        return String.format("http://127.0.0.1:%d", port);
    }

    @Override
    public void close() {
        stop();
    }
}