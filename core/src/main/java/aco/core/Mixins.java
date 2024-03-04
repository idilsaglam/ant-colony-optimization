/*
22015094 - SAGLAM Idil
*/
package aco.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.awt.geom.Rectangle2D;

abstract class Mixins {
    abstract static class Point {
        Point(@JsonProperty("x") int x, @JsonProperty("y") int y) {}
    }

    abstract static class Rectangle {

        @JsonGetter("top-left-corner")
        abstract Point getLocation();

        @JsonGetter("width")
        abstract double getWidth();

        @JsonGetter("height")
        abstract double getHeight();

        @JsonSetter("top-left-corner")
        abstract void setLocation(Point p);

        @JsonIgnore
        abstract Rectangle getBounds();

        @JsonIgnore
        abstract Rectangle2D getBounds2D();
    }

    abstract static class Circle {
        Circle(@JsonProperty("center") Point center, @JsonProperty("radius") double radius) {}
    }
}
