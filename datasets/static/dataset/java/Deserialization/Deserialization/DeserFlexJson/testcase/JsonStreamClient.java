<filename>quantfabric-core/src/main/java/com/quantfabric/net/stream/json/JsonStreamClient.java<fim_prefix>

        /*
         * Copyright 2022-2023 the original author or authors.
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        package com.quantfabric.net.stream.json;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.quantfabric.net.Receiver;
import com.quantfabric.net.stream.Deserializer;
import com.quantfabric.net.stream.InputStreamObjectReader;
import com.quantfabric.net.stream.InputStreamObjectReaderFactory;
import com.quantfabric.net.stream.StreamClient;

import flexjson.JSONDeserializer;

public class JsonStreamClient extends StreamClient
{
    public static class JsonDeserializer implements Deserializer
    {
        @SuppressWarnings("rawtypes")
        private final JSONDeserializer jsonDeserializer = new JSONDeserializer();

        @Override
        public Object deserialize(byte[] data)
        {
            return jsonDeserializer.deserialize(new String(data));
        }

        public Object deserialize(Reader input)
        {
<fim_suffix>
        }

        @Override
        public void registerType(String name, Class<?> type)
        {
        }
    }

    public static class JsonInputStreamObjectReader implements InputStreamObjectReader
    {
        private final JsonDeserializer serializer = new JsonDeserializer();
        private final Reader reader;

        public JsonInputStreamObjectReader(InputStream input)
        {
            this.reader = new BufferedReader(new InputStreamReader(input));
        }

        @Override
        public void registerType(String name, Class<?> type)
        {
            serializer.registerType(name, type);
        }

        @Override
        public Object read()
        {
            return serializer.deserialize(reader);
        }

    }

    public JsonStreamClient(Receiver receiver)
    {
        super(createInputStreamObjectReaderFactory(), receiver);
    }

    private static InputStreamObjectReaderFactory createInputStreamObjectReaderFactory()
    {
        return new InputStreamObjectReaderFactory() {

            @Override
            public InputStreamObjectReader create(InputStream input)
            {
                return new JsonInputStreamObjectReader(input);
            }
        };
    }

}
<fim_middle>