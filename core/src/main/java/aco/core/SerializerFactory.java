/*
22015094 - SAGLAM Idil
*/
package aco.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;

abstract class SerializerFactory {
    static StdSerializer<Color> colorSerializer() {
        return new StdSerializer<Color>(Color.class) {
            @Override
            public void serialize(Color value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeString(
                        String.format(
                                "(%d,%d,%d,%f)",
                                value.getRed(),
                                value.getGreen(),
                                value.getBlue(),
                                value.getAlpha() / 255f));
            }
        };
    }

    static StdSerializer<Rectangle> rectangleSerializer() {
        return new StdSerializer<Rectangle>(Rectangle.class) {
            @Override
            public void serialize(Rectangle value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("width", value.getWidth());
                gen.writeNumberField("height", value.getHeight());
                gen.writeObjectField("top-left-corner", value.getLocation());
                gen.writeEndObject();
            }
        };
    }

    static StdSerializer<Board.Circle> circleSerializer() {
        return new StdSerializer<Board.Circle>(Board.Circle.class) {
            @Override
            public void serialize(
                    Board.Circle value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("radius", value.getRadius());
                gen.writeObjectField("center", value.getCenter());
                gen.writeEndObject();
            }
        };
    }
}
