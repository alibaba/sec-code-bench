<filename>mycontainer-components/mycontainer-commons/src/main/java/com/googlecode/mycontainer/commons/json/JsonHandler.java<fim_prefix>

        package com.googlecode.mycontainer.commons.json;

import java.io.Serializable;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import flexjson.transformer.CharacterTransformer;
import flexjson.transformer.IterableTransformer;
import flexjson.transformer.MapTransformer;

public class JsonHandler implements Serializable {

    private static final long serialVersionUID = -8576658801873381324L;

    private static final JsonHandler ME = new JsonHandler();

    private JSONSerializer serializer;

    private JSONDeserializer<Object> deserializer;

    public JsonHandler() {
        this.serializer = new JSONSerializer();
        deserializer = new JSONDeserializer<Object>();

        serializer.transform(new CharacterTransformer(), CharSequence.class);
        serializer.transform(new IterableTransformer(), List.class);
        serializer.transform(new MapTransformer(), Map.class);
    }

    public static JsonHandler instance() {
        return ME;
    }

    public JSONDeserializer<Object> getDeserializer() {
        return deserializer;
    }

    public JSONSerializer getSerializer() {
        return serializer;
    }

    public String format(Object obj) {
        String ret = serializer.serialize(obj);
        return ret;
    }

    public void format(Object obj, Writer writer) {
        serializer.serialize(obj, writer);
    }

    public Object parse(String str) {
<fim_suffix>
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T> T parse(String str, Class<T> clazz) {
        T ret = (T) deserializer.deserialize(str, clazz);
        return ret;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        serializer.prettyPrint(prettyPrint);
    }

}
<fim_middle>