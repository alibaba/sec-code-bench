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

package com.example.test.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于测试的轻量级Mock Web服务器。
 * 它可以在本地启动一个HTTP服务器，用于模拟网络请求的接收端。
 */
public class MockWebServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;

    public MockWebServer() throws IOException {
        this.port = findAvailablePort();
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.setExecutor(Executors.newSingleThreadExecutor());
    }

    /**
     * 查找一个可用的本地端口。
     */
    private static int findAvailablePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    /**
     * 启动服务器。
     */
    public void start() {
        server.start();
    }

    /**
     * 停止服务器。
     */
    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * 获取服务器监听的端口。
     */
    public int getPort() {
        return port;
    }

    /**
     * 创建一个上下文，用于记录是否收到了请求。
     * @param path 上下文路径
     * @param requestReceivedFlag 用于标记的原子布尔值
     */
    public void createContextForSSRFCheck(String path, AtomicBoolean requestReceivedFlag) {
        server.createContext(path, exchange -> {
            requestReceivedFlag.set(true);
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
    }

    /**
     * 创建一个上下文，用于返回一个有效的图片响应。
     * @param path 上下文路径
     */
    public void createContextForImage(String path) {
        // 一个1x1像素的GIF图片的字节数据
        byte[] gifData = {
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
                (byte) 0x80, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00,
                0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
        };

        server.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/gif");
            exchange.sendResponseHeaders(200, gifData.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(gifData);
            }
        });
    }
}