<filename>Chapter18/MySQL/Maven/Providers/ConverterProvider/B250ConverterProvider/src/main/java/com/classicmodels/provider/MyJsonUtil.java<fim_prefix>

        package com.example.json;

import java.io.Serializable;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import flexjson.transformer.CharacterTransformer;
import flexjson.transformer.IterableTransformer;
import flexjson.transformer.MapTransformer;

public class MyJsonUtil implements Serializable {

    private static final long serialVersionUID = 123456789L;

    private static final MyJsonUtil INSTANCE = new MyJsonUtil();

    private final JSONSerializer serializer;
    private final JSONDeserializer<Object> deserializer;

    public MyJsonUtil() {
        serializer = new JSONSerializer();
        deserializer = new JSONDeserializer<Object>();

        serializer.transform(new CharacterTransformer(), CharSequence.class);
        serializer.transform(new IterableTransformer(), List.class);
        serializer.transform(new MapTransformer(), Map.class);
    }

    public static MyJsonUtil getInstance() {
        return INSTANCE;
    }

    public JSONSerializer getSerializer() {
        return serializer;
    }

    public JSONDeserializer<Object> getDeserializer() {
        return deserializer;
    }

    public String toJson(Object obj) {
        return serializer.serialize(obj);
    }

    public void toJson(Object obj, Writer writer) {
        serializer.serialize(obj, writer);
    }

    public Object fromJson(String json) {
<fim_suffix>
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, Class<T> clazz) {
        return (T) deserializer.deserialize(json, clazz);
    }

    public void setPrettyPrint(boolean pretty) {
        serializer.prettyPrint(pretty);
    }
}

<fim_middle>