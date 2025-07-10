<filename>drcBase/src/main/java/com/timevale/drc/base/serialize/GenericJackson2JsonRedisSerializer.java<fim_prefix>

package com.timevale.drc.base.serialize;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.cache.support.NullValue;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author gwk_2
 * @date 2022/1/8 04:37
 */
public class GenericJackson2JsonRedisSerializer {

    private final ObjectMapper mapper;

    /**
     * Creates {@link GenericJackson2JsonRedisSerializer} and configures {@link ObjectMapper} for
     * default typing.
     */
    public GenericJackson2JsonRedisSerializer() {
        this((String) null);
    }

    /**
     * Creates {@link GenericJackson2JsonRedisSerializer} and configures {@link ObjectMapper} for
     * default typing using the given {@literal name}. In case of an {@literal empty} or {@literal
     * null} String the default {@link JsonTypeInfo.Id#CLASS} will be used.
     *
     * @param classPropertyTypeName Name of the JSON property holding type information. Can be
     * {@literal null}.
     */
    public GenericJackson2JsonRedisSerializer(String classPropertyTypeName) {

        this(new ObjectMapper());

        // simply setting {@code mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)} does not help here since we need
        // the type hint embedded for deserialization using the default typing feature.
        mapper.registerModule(
                new SimpleModule().addSerializer(new NullValueSerializer(classPropertyTypeName)));

        if (StringUtils.hasText(classPropertyTypeName)) {
        <fim_suffix>
        }
    }

    /**
     * Setting a custom-configured {@link ObjectMapper} is one way to take further control of the
     * JSON serialization process. For example, an extended {@link SerializerFactory} can be
     * configured that provides custom serializers for specific types.
     *
     * @param mapper must not be {@literal null}.
     */
    public GenericJackson2JsonRedisSerializer(ObjectMapper mapper) {

        Assert.notNull(mapper, "ObjectMapper must not be null!");
        this.mapper = mapper;
    }

    public byte[] serialize(Object source)   {

        if (source == null) {
            return null;
        }

        try {
            return mapper.writeValueAsBytes(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write JSON: " + e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     */
    public Object deserialize(byte[] source) throws RuntimeException {
        return deserialize(source, Object.class);
    }

    /**
     * @param source can be {@literal null}.
     * @param type must not be {@literal null}.
     * @return {@literal null} for empty source.
     */
    public <T> T deserialize(byte[] source, Class<T> type) throws RuntimeException {

        Assert.notNull(type,
                "Deserialization type must not be null! Pleaes provide Object.class to make use of Jackson2 default typing.");

        if (source == null || source.length == 0) {
            return null;
        }

        try {
            return mapper.readValue(source, type);
        } catch (Exception ex) {
            throw new RuntimeException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@link StdSerializer} adding class information required by default typing. This allows
     * de-/serialization of {@link NullValue}.
     *
     * @author Christoph Strobl
     * @since 1.8
     */
    private class NullValueSerializer extends StdSerializer<NullValue> {

        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        /**
         * @param classIdentifier can be {@literal null} and will be defaulted to {@code @class}.
         */
        NullValueSerializer(String classIdentifier) {

            super(NullValue.class);
            this.classIdentifier =
                    StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        /*
         * (non-Javadoc)
         * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

            jgen.writeStartObject();
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }
    }

    @Override
    public String toString() {
        return "GenericJackson2JsonRedisSerializer{" +
                "mapper" + +
                '}';
    }
}
<fim_middle>