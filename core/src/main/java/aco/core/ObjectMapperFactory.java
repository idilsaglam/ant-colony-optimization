/*
22015094 - SAGLAM Idil
*/
package aco.core;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

abstract class ObjectMapperFactory {

    static ObjectMapper createSettingsBuilderSerializer() {
        final ObjectMapperBuilder builder = new ObjectMapperBuilder();
        return builder.addSerializer(Color.class, SerializerFactory.colorSerializer()).build();
    }

    static ObjectMapper createSettingsBuilderDeserializer() {
        final ObjectMapperBuilder builder = new ObjectMapperBuilder();
        return builder.addDeserializer(Color.class, DeserializerFactory.colorDeserializer())
                .build();
    }

    static ObjectMapper boardBuilderDeserializer() {
        final ObjectMapperBuilder builder = new ObjectMapperBuilder();
        builder.addDeserializer(Rectangle.class, DeserializerFactory.rectangleDeserializer())
                .addDeserializer(Board.Circle.class, DeserializerFactory.circleDeserializer());
        // TODO: Add rectangle mixin?
        // TODO: Add Circle mixin?
        return builder.build();
    }

    static ObjectMapper boardBuilderSerializer() {
        final ObjectMapperBuilder builder = new ObjectMapperBuilder();
        builder.addSerializer(Rectangle.class, SerializerFactory.rectangleSerializer())
                .addSerializer(Board.Circle.class, SerializerFactory.circleSerializer());
        return builder.build();
    }

    private static class ObjectMapperBuilder {
        private final SimpleModule module = new SimpleModule();
        private final Map<Class<?>, Class<?>> mixins = new HashMap<>();

        private <T> ObjectMapperBuilder addSerializer(
                Class<? extends T> type, JsonSerializer<T> serializer) {
            module.addSerializer(type, serializer);
            return this;
        }

        private <T> ObjectMapperBuilder addDeserializer(
                Class<T> type, JsonDeserializer<? extends T> deserializer) {
            module.addDeserializer(type, deserializer);
            return this;
        }

        private ObjectMapperBuilder addMixin(Class<?> target, Class<?> source) {
            this.mixins.put(target, source);
            return this;
        }

        private ObjectMapper build() {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(this.module);
            for (Map.Entry<Class<?>, Class<?>> e : this.mixins.entrySet()) {
                mapper.addMixIn(e.getKey(), e.getValue());
            }
            return mapper;
        }
    }
}
