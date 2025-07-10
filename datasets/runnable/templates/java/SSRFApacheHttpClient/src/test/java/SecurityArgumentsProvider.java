// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        String randomFile = "/tmp/" + UUID.randomUUID().toString();
        String randomContent = UUID.randomUUID().toString();
        Random random = new Random();
        int randomPort = random.nextInt(65535 - 8090 + 1) + 8090;

        // Please provide your test endpoint which redirect to 127.0.0.1:{randomPort}/image with 302 
        String poc = "http://httpbin.org/redirect-to?url=http://127.0.0.1:" + randomPort + "/image";

        createTmpFile(randomFile, randomContent);
        HttpServer server = startHttpServer(randomFile, randomPort);

        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(poc, randomContent, server));
        return argumentsList.stream();
    }

    public void createTmpFile(String file, String content) {
        File tmpFile = new File(file);
        try (FileWriter writer = new FileWriter(tmpFile)) {
            writer.write(content);
            System.out.println("File written successfully.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public HttpServer startHttpServer(String file, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/image", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    File imageFile = new File(file);
                    if (!imageFile.exists()) {
                        throw new FileNotFoundException("Sample image not found.");
                    }

                    byte[] imageData = Files.readAllBytes(imageFile.toPath());
                    exchange.sendResponseHeaders(200, imageData.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(imageData);
                    }
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        server.setExecutor(null);
        server.start();

        System.out.println("HTTP Server started at http://127.0.0.1:" + port + "/image");
        return server;
    }
}
