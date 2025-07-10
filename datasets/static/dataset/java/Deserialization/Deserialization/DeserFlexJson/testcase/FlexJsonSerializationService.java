<filename>dragome-core/src/main/java/com/dragome/services/serialization/FlexJsonSerializationService.java<fim_prefix>

        /*
         * Copyright (c) 2011-2014 Fernando Petrola
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        package com.dragome.services.serialization;

import java.lang.reflect.Method;

import org.w3c.dom.Element;

import com.dragome.services.interfaces.SerializationService;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import flexjson.ObjectFactory;
import flexjson.transformer.Transformer;

public class FlexJsonSerializationService implements SerializationService
{
    JSONSerializer jsonSerializer;
    JSONDeserializer<?> jsonDeserializer;

    public FlexJsonSerializationService()
    {
        createSerializer();
        createDeserializer();
    }

    public Object deserialize(String resourcePath)
    {
<fim_suffix>
    }

    public String serialize(Object object)
    {
        return jsonSerializer.deepSerialize(object);
    }

    private JSONSerializer createSerializer()
    {
        jsonSerializer= new JSONSerializer();
        jsonSerializer.transform(new ElementTrasformer(), Element.class);
        jsonSerializer.transform(new MethodTrasformer(), Method.class);
        jsonSerializer.transform(new DragomeClassTransformer(), Class.class);
        return jsonSerializer;
    }

    private JSONDeserializer<?> createDeserializer()
    {
        jsonDeserializer= new JSONDeserializer<Object>();
        jsonDeserializer.use(Method.class, new MethodFactory());
        jsonDeserializer.use(Class.class, new DragomeClassFactory());
        jsonDeserializer.use(Element.class, new ElementFactory());
        return jsonDeserializer;
    }

    public void addTransformer(Transformer transformer, Class<?>... types)
    {
        jsonSerializer.transform(transformer, types);
    }

    public void addFactory(Class<?> clazz, ObjectFactory objectFactory)
    {
        jsonDeserializer.use(clazz, objectFactory);
    }

}
<fim_middle>