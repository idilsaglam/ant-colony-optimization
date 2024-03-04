/*
22015094 - SAGLAM Idil
*/
package aco.core;

import aco.core.Board.Circle;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class DeserializerFactory {

    static StdDeserializer<Rectangle> rectangleDeserializer() {
        return new StdDeserializer<Rectangle>(Rectangle.class) {
            @Override
            public Rectangle deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException, JacksonException {
                final JsonNode json = p.getCodec().readTree(p);
                final double w = json.get("width").asDouble(), h = json.get("height").asDouble();
                final Point tlc =
                        p.getCodec().treeToValue(json.get("top-left-corner"), Point.class);
                return new Rectangle(tlc.x, tlc.y, (int) w, (int) h);
            }
        };
    }

    static StdDeserializer<Board.Circle> circleDeserializer() {
        return new StdDeserializer<Board.Circle>(Board.Circle.class) {
            @Override
            public Circle deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException, JacksonException {
                final JsonNode json = p.getCodec().readTree(p);
                final JsonNode radiusNode = json.get("radius");
                final JsonNode centerNode = json.get("center");
                // TODO: Increase precision with Point2D.Float
                final Point center = p.getCodec().treeToValue(centerNode, Point.class);
                return new Board.Circle(center.x, center.y, radiusNode.asDouble());
            }
        };
    }

    /**
     * Creates a Color deserializer
     *
     * @return A new color deserializer
     */
    static StdDeserializer<Color> colorDeserializer() {
        return new StdDeserializer<Color>(Color.class) {
            final Pattern pattern =
                    Pattern.compile(
                            "\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(\\s*,\\s*(\\d+(\\.\\d+)?))?\\s*\\)");

            @Override
            public Color deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException, JacksonException {
                final String rgbaString = p.getText();
                final Matcher matcher = this.pattern.matcher(rgbaString);
                if (matcher.matches()) {
                    final int r = Integer.parseInt(matcher.group(1));

                    final int g = Integer.parseInt(matcher.group(2));

                    final int b = Integer.parseInt(matcher.group(3));
                    final String ag = matcher.group(5);
                    if (ag == null) {
                        return new Color(r, g, b);
                    }
                    if (ag.contains(".")) {
                        // If alpha value is float
                        return new Color(r / 255f, g / 255f, b / 255f, Float.parseFloat(ag));
                    }
                    // If alpha value is an integer
                    return new Color(r, g, b, Integer.parseInt(ag));
                }
                throw new RuntimeException(
                        String.format("%s is not a valid rgba string", rgbaString));
            }
        };
    }
}
