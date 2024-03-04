/*
22015094 - SAGLAM Idil
*/
package aco.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.math3.util.Pair;

public class Board extends Rectangle implements Runnable {

    private final PausableThreadPoolExecutor executor;
    private final Board.CircularElement sourcePoint;
    private final Board.CircularElement destinationPoint;
    private final Set<Board.Obstacle> obstacles;
    private volatile ConcurrentHashMap<Point2D, Pheromone> pheromones;
    private volatile CopyOnWriteArraySet<Ant> ants;
    private volatile Set<Board.Subscriber> subscribers;
    private final Settings settings;

    private static final Random r = new Random();

    private volatile Optional<Boolean> isPaused;

    /**
     * Create a Board instance with the given parameters
     *
     * @param bounds The bounds of the rectangle to create
     * @param sourcePoint The source point of the board
     * @param destinationPoint The destination point of the board
     * @param obstacles The Set of obstacles in the board
     */
    Board(
            Rectangle bounds,
            Board.CircularElement sourcePoint,
            Board.CircularElement destinationPoint,
            Set<Board.Obstacle> obstacles,
            Settings settings) {
        super(bounds);
        this.sourcePoint = sourcePoint;
        this.destinationPoint = destinationPoint;
        this.obstacles = obstacles;
        this.pheromones = new ConcurrentHashMap<>();
        this.ants = new CopyOnWriteArraySet<>();
        this.subscribers = new HashSet<>();
        // Try using settings.threadPoolSize()
        this.executor =
                new PausableThreadPoolExecutor(settings.numberOfAnts(), settings.threadpoolSize());
        this.settings = settings;
        // As the algorithme is neither started nor paused yet, isPaused boolean will be empty
        this.isPaused = Optional.empty();
    }

    /**
     * Get the source point
     *
     * @return The source point of the Board
     */
    public Board.CircularElement getSourcePoint() {
        return sourcePoint;
    }

    /**
     * Get the destination point of the board
     *
     * @return The destination point in the current board
     */
    public Board.CircularElement getDestinationPoint() {
        return destinationPoint;
    }

    /**
     * Get the set obstacles in the current board
     *
     * @return The set of obstacles in the current board
     */
    public Set<Board.Obstacle> getObstacles() {
        return obstacles;
    }

    /**
     * The set of pheromones on the current board
     *
     * @return The set of pheromones on the current board
     */
    public Map<Point2D, Pheromone> getPheromones() {
        return this.pheromones;
    }

    /**
     * Return the location of the source point
     *
     * @return The point representing the location of the source point
     */
    public Point getSourcePointLocation() {
        return this.sourcePoint.getLocation();
    }

    /**
     * Get the location of the destination point
     *
     * @return The point representing the location of the source point
     */
    public Point getDestinationPointLocation() {
        return this.destinationPoint.getLocation();
    }

    /**
     * When an object implementing interface {@code Runnable} is used to create a thread, starting
     * the thread causes the object's {@code run} method to be called in that separately executing
     * thread.
     *
     * <p>The general contract of the method {@code run} is that it may take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (Board.this.isPaused.isEmpty()) {
            // Spawn new ants
            this.executor.execute(this::spawnAnts);
            this.executor.execute(this::updatePheromones);
            this.executor.execute(this::moveAnts);
            try {
                boolean result = this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ignore) {
            }
            return;
        }
        System.out.println("Unpaused");
        Board.this.isPaused = Optional.of(false);
    }

    /** Pause the current execution of the algorithm */
    public void pause() {
        // FIXME: Pause not working
        System.out.println("EXECUTOR PAUSED");
        Board.this.isPaused = Optional.of(true);
    }

    /**
     * Creates the board builder instance with the current board's attributes
     *
     * @return The Builder instance from the current board instance
     */
    public Builder builder() {
        return new Builder(this.sourcePoint, this.destinationPoint, this.obstacles, this);
    }

    /** Stop the current execution of the algorithm */
    public void stop() {
        this.executor.shutdown();
    }

    private boolean isPaused() {
        return (Board.this.isPaused.isPresent() && Board.this.isPaused.get());
    }

    /** Update all pheromones */
    private void updatePheromones() {
        while (!Board.this.isPaused()) {
            // Evaporate all pheromones which ca evaporate
            this.pheromones.values().parallelStream()
                    .filter(Pheromone::canEvaporate)
                    .forEach(Pheromone::evaporate);
        }
        while (Board.this.isPaused())
            ;
        updatePheromones();
    }

    /** Move all ants parallelized in a separate thread pool */
    private void moveAnts() {
        while (!Board.this.isPaused()) {
            // Move all ants
            this.ants.parallelStream()
                    .forEach(
                            (ant) -> {
                                ant.move();
                            });
        }
        while (Board.this.isPaused())
            ;
        moveAnts();
    }

    /**
     * Add a new subscriber to the board
     *
     * @param subscriber The subscriber to add
     * @return True if the subscriber is successfully added, false if not
     */
    public boolean subscribe(Board.Subscriber subscriber) {
        if (this.subscribers.add(subscriber)) {
            subscriber.onSubscribed();
            return true;
        }
        // TODO: Add an error message
        subscriber.onSubscriptionFailed("Subscription failed");
        return false;
    }

    /**
     * Removes the given subscriber from the subscribers set
     *
     * @param subscriber The subscriber to remove
     * @return True if the subscriber removed successfully, false if not
     */
    public boolean unsubscribe(Board.Subscriber subscriber) {
        if (this.subscribers.remove(subscriber)) {
            subscriber.onUnsubscribe();
            return true;
        }
        subscriber.onUnsubscribeFailed("Unsubscribe action failed");
        return false;
    }

    /**
     * Add pheromone with the given intensity to the given point
     *
     * @param position The position of the pheromone to add
     * @param intensity The intensity of the pheromone to add
     */
    private void addPheromone(Point2D position, int intensity) {

        if (this.pheromones.get(position) == null) {

            final Pheromone pheromone = new Pheromone(position, 0);

            this.pheromones.put(position, pheromone);

            // Calculate the distance to each ant of the given pheromone
            this.ants.parallelStream().forEach(a -> a.calculateDistanceToPheromone(pheromone));
        }
        this.pheromones.get(position).updateIntensity(intensity);
    }

    /** Spawn new ants each second */
    private void spawnAnts() {
        final AntFactory factory = new AntFactory();
        while (!Board.this.isPaused() && this.ants.size() <= Board.this.settings.numberOfAnts()) {
            Stream.generate(factory::randomAnt)
                    .limit(this.settings.antsPerSecond())
                    .parallel()
                    .forEach(
                            (Board.Ant ant) -> {
                                this.ants.add(ant);
                                this.subscribers.parallelStream()
                                        .forEach(
                                                (Board.Subscriber subscriber) ->
                                                        subscriber.onNewAntsSpawned(
                                                                ant.uuid, ant.getCenter()));
                            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
                break;
            }
        }
        while (Board.this.isPaused())
            ;
        if (this.ants.size() <= Board.this.settings.numberOfAnts()) {
            spawnAnts();
        }
    }

    /**
     * Get the area of the board element in the current board builder panel's space
     *
     * @param element The element to get the area. If null, it will return the area of the current
     *     board builder panel
     * @return The area of the element in the current board builder panel space
     */
    private Area getArea(IElement element) {
        if (element == null) {
            return new Area(this.getBounds());
        }
        return element.getArea(this.getLocation());
    }

    /**
     * Check if the given components colludes on the plan of the current Builder's enclosing
     * rectangle
     *
     * @param component The first component to check for the collusion
     * @param other The other component to check for the collusion
     * @return True if the given two components colludes on the current Builder's enclosing
     *     rectangle's plan. If components does not collude or the other component is null it
     *     returns false
     */
    private boolean areComponentsColludes(IElement component, IElement other) {
        if (other == null) return false;
        Area componentArea = this.getArea(component);
        Area sourcePointArea = this.getArea(other);
        componentArea.intersect(sourcePointArea);
        return !componentArea.isEmpty();
    }

    /**
     * Check if the given element is inside the board builder panel
     *
     * @param element The board element to check
     * @return True if the board element is inside the board builder panel
     */
    private boolean isInside(IElement element) {
        Area elementArea = this.getArea(element);
        elementArea.intersect(this.getArea(null));
        return elementArea.equals(this.getArea(element));
    }

    public static class Builder {
        @JsonProperty("source-point")
        private CircularElement sourcePoint;

        @JsonProperty("destination-point")
        private CircularElement destinationPoint;

        @JsonInclude(Include.NON_EMPTY)
        @JsonProperty("obstacles")
        private Set<Obstacle> obstacles;

        @JsonProperty("enclosing-rectangle")
        private Rectangle enclosingRectangle;

        @JsonIgnore private Obstacle currentObstacle;

        public Builder() {
            this.obstacles = new HashSet<>();
            this.enclosingRectangle = new Rectangle();
        }

        public Builder(
                CircularElement sourcePoint,
                CircularElement destinationPoint,
                Set<Obstacle> obstacles,
                Rectangle enclosingRectangle) {
            this.sourcePoint = sourcePoint;
            this.destinationPoint = destinationPoint;
            this.obstacles = obstacles;
            this.enclosingRectangle = enclosingRectangle;
        }

        public static Builder from(File selectedFile) throws IOException {
            return ObjectMapperFactory.boardBuilderDeserializer()
                    .readerFor(Builder.class)
                    .readValue(selectedFile);
        }

        @JsonSetter("enclosing-rectangle")
        public Builder enclosingRectangle(Rectangle enclosingRectangle) {
            this.enclosingRectangle = enclosingRectangle;
            return this;
        }

        @JsonIgnore
        public Point getSourcePointLocation() {
            return this.sourcePoint.getLocation();
        }

        @JsonIgnore
        public Point getDestinationPointLocation() {
            return this.destinationPoint.getLocation();
        }

        @JsonIgnore
        public Set<Rectangle> getObstacleBounds() {
            return this.obstacles.parallelStream()
                    .map(Obstacle::getBounds)
                    .collect(Collectors.toSet());
        }

        /**
         * Get the source point of the current board builder
         *
         * @return The source point of the current board builder
         */
        public CircularElement getSourcePoint() {
            return this.sourcePoint;
        }

        public Builder sourcePoint(CircularElement sourcePoint) {
            this.sourcePoint = sourcePoint;
            return this;
        }

        /**
         * Get the destination point of the current board builder
         *
         * @return The destination point of the current board builder
         */
        public CircularElement getDestinationPoint() {
            return this.destinationPoint;
        }

        public Builder destinationPoint(CircularElement destinationPoint) {
            this.destinationPoint = destinationPoint;
            return this;
        }
        /**
         * Get the set of obstacles in the current board builder
         *
         * @return The set of obstacles in the current board builder
         */
        public Set<Obstacle> getObstacles() {
            return this.obstacles;
        }

        public Builder obstacle(Obstacle obstacle) {
            this.obstacles.add(obstacle);
            return this;
        }

        /**
         * Get bounds of the enclosing rectangle
         *
         * @return The enclosing rectangle
         */
        @JsonIgnore
        public Rectangle getBounds() {
            return this.enclosingRectangle;
        }

        /**
         * Create an obstacle on the given location
         *
         * @param location The top left corner of the obstacle to add
         */
        public void createObstacle(Point location)
                throws Board.ElementOutOfBoundsException,
                        Board.ElementColludesWithOtherElementsOfBoardException {
            Board.Obstacle o = new Board.Obstacle(location);
            if (this.isInsideOfTheBoard(o)) {
                if (this.isCollusionSafe(o, BoardElementType.OBSTACLE)) {
                    this.currentObstacle = o;
                    return;
                }
                throw new Board.ElementColludesWithOtherElementsOfBoardException(
                        location, 0, BoardElementType.OBSTACLE);
            }
            throw new Board.ElementOutOfBoundsException(location, 0);
        }

        /**
         * Reset the enclosing rectangle and set the upper left corner with the given point
         *
         * @param point The point of upper left corner of the enclosing rectangle
         */
        public void resetEnclosingRectangle(Point point) {
            this.enclosingRectangle.setBounds(point.x, point.y, 0, 0);
        }

        /**
         * Resize the enclosing rectangle for the given point
         *
         * @param point The point that we will resize the rectangle
         */
        public void setBounds(Point point) {
            // TODO: Handle if user moves the rectangle on the opposite side.
            this.enclosingRectangle.setBounds(
                    (int) Math.ceil(this.enclosingRectangle.getX()),
                    (int) Math.ceil(this.enclosingRectangle.getY()),
                    (int) Math.ceil(point.x - this.enclosingRectangle.getX()),
                    (int) Math.ceil(point.y - this.enclosingRectangle.getY()));
        }

        /**
         * Add the source point to the given location and with the given radius
         *
         * @param location The top left corner of the source point to add
         * @param radius The radius of the source point to add
         * @throws Board.ElementOutOfBoundsException If the added element is out of the bounds
         * @throws Board.ElementColludesWithOtherElementsOfBoardException If the added element
         *     colludes with another elements
         */
        public void setSourcePoint(Point location, int radius)
                throws Board.ElementOutOfBoundsException,
                        Board.ElementColludesWithOtherElementsOfBoardException {
            Point center = new Point(location.x + radius, location.y + radius);
            CircularElement sp = new CircularElement(center, radius);
            if (this.isInsideOfTheBoard(sp)) {
                if (this.isCollusionSafe(sp, Board.BoardElementType.SOURCE_POINT)) {
                    this.sourcePoint = sp;
                    return;
                }
                throw new Board.ElementColludesWithOtherElementsOfBoardException(
                        location, radius, BoardElementType.SOURCE_POINT);
            }
            throw new Board.ElementOutOfBoundsException(location, radius);
        }

        /**
         * Add the destination point to the given location and with the given radius
         *
         * @param location The location of the top left corner of the element to add
         * @param radius The radius of the element to add
         * @throws Board.ElementOutOfBoundsException If the added element is out of the bounds
         * @throws Board.ElementColludesWithOtherElementsOfBoardException If the added element
         *     colludes with another elements
         */
        public void setDestinationPoint(Point location, int radius)
                throws Board.ElementOutOfBoundsException,
                        Board.ElementColludesWithOtherElementsOfBoardException {
            Point center = new Point(location.x + radius, location.y + radius);
            CircularElement dp = new CircularElement(center, radius);
            if (this.isInsideOfTheBoard(dp)) {
                if (this.isCollusionSafe(dp, Board.BoardElementType.DESTINATION_POINT)) {
                    this.destinationPoint = dp;
                    return;
                }
                throw new Board.ElementColludesWithOtherElementsOfBoardException(
                        location, radius, BoardElementType.DESTINATION_POINT);
            }
            throw new Board.ElementOutOfBoundsException(location, radius);
        }

        /**
         * Resize the last added obstacle following the given point
         *
         * @param point The point to resize the obstacle with
         */
        public void resizeCurrentObstacle(Point point)
                throws Board.ElementOutOfBoundsException,
                        Board.ElementColludesWithOtherElementsOfBoardException {
            Board.Obstacle aux =
                    new Board.Obstacle(
                            this.currentObstacle.getLocation(),
                            this.currentObstacle.width,
                            this.currentObstacle.height);
            int x = Math.min(this.currentObstacle.x, point.x),
                    y = Math.min(this.currentObstacle.y, point.y),
                    w = Math.abs(point.x - this.currentObstacle.x),
                    h = Math.abs(point.y - this.currentObstacle.y);
            this.currentObstacle.setBounds(x, y, w, h);
            if (this.isInsideOfTheBoard(aux)) {
                if (this.isCollusionSafe(this.currentObstacle, Board.BoardElementType.OBSTACLE)) {
                    return;
                }
                // If the current obstacle colludes with another element, reset the old bounds
                this.currentObstacle.setBounds(aux);
                throw new Board.ElementColludesWithOtherElementsOfBoardException(
                        aux.getLocation(), aux.width, BoardElementType.OBSTACLE);
            }
            // If the current obstacle is out of board, reset the bounds
            this.currentObstacle.setBounds(aux);
            throw new Board.ElementOutOfBoundsException(aux.getLocation(), aux.width);
        }

        /**
         * Check if the given element is inside the board builder panel
         *
         * @param element The board element to check
         * @return True if the board element is inside the board builder panel
         */
        public boolean isInsideOfTheBoard(IElement element) {
            Area elementArea = this.getArea(element);
            elementArea.intersect(this.getArea(null));
            return elementArea.equals(this.getArea(element));
        }

        /**
         * Build the board with the given settings
         *
         * @param settings The settings for the visualisation of the algorithm
         * @return The Board instance to visualise the algorithm
         */
        public Board build(Settings settings) {
            return new Board(
                    this.enclosingRectangle,
                    this.sourcePoint,
                    this.destinationPoint,
                    this.obstacles,
                    settings);
        }

        /**
         * Get the area of the board element in the current board builder panel's space
         *
         * @param element The element to get the area. If null, it will return the area of the
         *     current board builder panel
         * @return The area of the element in the current board builder panel space
         */
        public Area getArea(IElement element) {
            if (element == null) {
                return new Area(this.enclosingRectangle.getBounds());
            }
            return element.getArea(this.enclosingRectangle.getLocation());
        }

        /**
         * Verify if the given bounds is empty on the current board builder panel
         *
         * @param component The IElement instance to verify
         * @return True if the component is in an empty area of the board builder, false if not
         */
        public boolean isCollusionSafe(IElement component, Board.BoardElementType elementType) {
            return switch (elementType) {
                        case OBSTACLE -> !(this.areComponentsColludes(component, this.sourcePoint)
                                || this.areComponentsColludes(component, this.destinationPoint));
                        case DESTINATION_POINT -> !(this.areComponentsColludes(
                                component, this.sourcePoint));
                        case SOURCE_POINT -> !(this.areComponentsColludes(
                                component, this.destinationPoint));
                    }
                    && this.obstacles.parallelStream()
                            .noneMatch(obstacle -> this.areComponentsColludes(component, obstacle));
        }

        /** Add the current obstacle to the set of obstacles */
        public void addCurrentObstacle() {
            if (this.currentObstacle.isEmpty()) {
                // If the current obstacle is empty, do nothing
                return;
            }
            this.obstacles.add(this.currentObstacle);
        }

        /**
         * Verify if the enclosing rectangle of the current board builder is present and not empty
         *
         * @return True if the enclosing rectangle of the current board builder is present and not
         *     empty
         */
        @JsonIgnore
        public boolean isEnclosingRectangleIsPresent() {
            return this.enclosingRectangle != null && !this.enclosingRectangle.isEmpty();
        }

        /**
         * Method verify if the source point is present in the current board builder instance
         *
         * @return True if the source point is present in the current board builder instance
         */
        @JsonIgnore
        public boolean isSourcePointPresent() {
            return this.isPresent(BoardElementType.SOURCE_POINT);
        }

        /**
         * Method verify if the destination point is present in the current board builder instance
         *
         * @return True if the destination point is present in the current board builder instance
         */
        @JsonIgnore
        public boolean isDestinationPointPresent() {
            return this.isPresent(BoardElementType.DESTINATION_POINT);
        }

        public void save(File file) throws IOException {
            this.validate();
            ObjectMapperFactory.boardBuilderSerializer()
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(file, this);
        }

        private void validate() throws IllegalArgumentException {
            // TODO: Complete methods implementation
            if (this.sourcePoint == null
                    || this.destinationPoint == null
                    || this.obstacles == null) {
                throw new IllegalArgumentException();
            }
        }
        /**
         * Method verify if the board element with the given type is present in the current board
         * builder instance
         *
         * @param boardElementType The board builder element type to verify
         * @return True if the given board builder element type is present in the current board
         *     builder instance
         */
        private boolean isPresent(BoardElementType boardElementType) {
            return switch (boardElementType) {
                case OBSTACLE -> obstacles != null && obstacles.size() > 0;
                case DESTINATION_POINT -> destinationPoint != null && !destinationPoint.isEmpty();
                case SOURCE_POINT -> sourcePoint != null && !sourcePoint.isEmpty();
            };
        }

        /**
         * Check if the given components colludes on the plan of the current Builder's enclosing
         * rectangle
         *
         * @param component The first component to check for the collusion
         * @param other The other component to check for the collusion
         * @return True if the given two components colludes on the current Builder's enclosing
         *     rectangle's plan. If components does not collude or the other component is null it
         *     returns false
         */
        private boolean areComponentsColludes(IElement component, IElement other) {
            if (other == null) return false;
            Area componentArea = this.getArea(component);
            Area sourcePointArea = this.getArea(other);
            componentArea.intersect(sourcePointArea);
            return !componentArea.isEmpty();
        }
    }

    public interface Subscriber {

        /**
         * Method called on ant moved
         *
         * @param uuid THe unique identifier of the ant to move
         * @param to The new position of the ant
         */
        void onAntMoved(UUID uuid, Point2D to);

        /**
         * The method called when a new ant is spawned
         *
         * @param uuid The unique identifier of the spawned ant
         * @param location The location of the spawned ant
         */
        void onNewAntsSpawned(UUID uuid, Point2D location);

        /**
         * Method called when the intensity of a pheromone changed
         *
         * @param position The position of the pheromone that its intensity changed
         * @param delta The delta of the intensity change
         */
        void onPheromoneIntensityChanged(Point2D position, int delta);

        /** Method called when a subscriber successfully subscribed to the board */
        void onSubscribed();

        /** Method called when the subscriber unsubscribed successfully */
        void onUnsubscribe();

        /** Method called if the subscription is failed */
        void onSubscriptionFailed(String message);

        /** Method called if the unsubscribe action fails */
        void onUnsubscribeFailed(String message);
    }

    public static final class ElementColludesWithOtherElementsOfBoardException extends Exception {
        public ElementColludesWithOtherElementsOfBoardException(
                Point topLeftCorner, int radius, BoardElementType boardElementType) {
            super(
                    String.format(
                            "Element with top left corner (%d,%d) and radius %d colludes"
                                    + " with %s",
                            topLeftCorner.x, topLeftCorner.y, radius, boardElementType.toString()));
        }
    }

    public static final class ElementOutOfBoundsException extends Exception {
        public ElementOutOfBoundsException(Point topLeftCorner, double radius) {
            super(
                    String.format(
                            "Element with topLeftCorner: (%d,%d) and radius: %f out of"
                                    + " bounds of the board builder panel",
                            topLeftCorner.x, topLeftCorner.y, radius));
        }
    }

    private class Pheromone extends Point2D.Double {
        private int intensity;
        private long lastUpdate;

        private Pheromone(Point2D position, int intensity) {
            super(position.getX(), position.getY());
            this.intensity = intensity;
            this.update();
        }

        private Pheromone(Point position) {
            this(position, 1);
        }

        private void updateIntensity() {
            this.updateIntensity(1);
        }

        private Point2D getPosition() {
            return new Point2D.Double(this.x, this.y);
        }

        /**
         * Updates the current pheromone's intensity by given delta
         *
         * @param delta The value to update the intensity with
         */
        private synchronized void updateIntensity(int delta) {
            this.intensity += delta;
            // Notify all subscribers of the board
            Board.this.subscribers.parallelStream()
                    .forEach(
                            (Board.Subscriber s) -> {
                                s.onPheromoneIntensityChanged(this.getPosition(), delta);
                            });
            this.update();
        }

        /** Updates the last update time */
        private void update() {
            this.lastUpdate = System.currentTimeMillis();
        }

        /**
         * Checks if the current pheromone can evaporate. A pheromone can evaporate if its intensity
         * is bigger than 0 and if its last updated time is less than the given evaporation timeout
         *
         * @return True if the last updated time of the current pheromone is less than the given
         *     evaporation timeout
         */
        public boolean canEvaporate() {
            return (this.intensity > 0)
                    && (System.currentTimeMillis() - this.lastUpdate)
                            >= Board.this.settings.pheromoneIntensityTimeoutMs();
        }

        /** Decrease the current pheromone's intensity by 1 */
        private void evaporate() {
            this.updateIntensity(-1);
        }
    }

    private class AntFactory {
        /**
         * Creates an ant randomly placed around the source point
         *
         * @return The Ant instance
         */
        private Ant randomAnt() {
            final double sideX = Board.r.nextDouble(-1, 1.01), sideY = Board.r.nextDouble(-1, 1.01);
            final double
                    x =
                            sideX
                                            * (Board.this.settings.sourcePointRadius()
                                                    + Board.this.settings.antsSemiMajorAxis())
                                    + Board.this.getSourcePoint().getCenter().getX(),
                    y =
                            sideY
                                            * (Board.this.settings.sourcePointRadius()
                                                    + Board.this.settings.antsSemiMinorAxis())
                                    + Board.this.getSourcePoint().getCenter().getY();
            return new Ant(
                    x,
                    y,
                    Board.this.settings.antsSemiMinorAxis(),
                    Board.this.settings.antsSemiMajorAxis());
        }
    }

    private class Ant extends EllipticalIElement {

        /*
           A variable indicates that if the current Ant is returning to the source point
        */
        private boolean returning;

        /*
           The unique identifier of the ant object. These uuids will also be used to identify ants
           on the client side
        */
        private final UUID uuid;
        /*
           Contains the distance of the current Ant to each pheromone.
           This is mainly updated automatically each time there's a new pheromone introduced and not the intensity
           of an existing pheromone is changed
        */
        private volatile Map<Board.Pheromone, java.lang.Double> distancesToPheromones;

        /**
         * Creates an elliptical board element with given parameters
         *
         * @param x The x coordinate of the center of the ellipse
         * @param y The y coordinate of the center of the ellipse
         * @param semiMinor The length of the semi minor axis of the ellipse
         * @param semiMajor The length of the semi major axis of the ellipse
         */
        private Ant(double x, double y, double semiMinor, double semiMajor) {
            super(x, y, semiMinor, semiMajor);
            this.uuid = UUID.randomUUID();
            this.distancesToPheromones = new ConcurrentHashMap<>();
            // Add distances for all existing pheromones at the moment of the Ant's creation
            Board.this.pheromones.values().parallelStream()
                    .forEach(this::calculateDistanceToPheromone);
            /* By creation the retuning variable is set to false
               as ants are spawning from source point
            */
            this.returning = false;
        }

        /**
         * Calculates the distance between the given pheromone and the current Ant instance
         *
         * @param pheromone The pheromone to calculate the distance to
         * @return The distance between the current ant and the given pheromone
         */
        private Optional<java.lang.Double> calculateDistanceToPheromone(Board.Pheromone pheromone) {
            // Calculate the distance if the given pheromone is reachable for the current ant
            if (this.isPheromoneReachable(pheromone)) {
                // Check if the pheromone exists in the given hash map
                if (this.distancesToPheromones.get(pheromone) == null) {
                    /*
                        If the given pheromone is not exists on the hash table,
                        this is a newly added pheromone
                    */
                    final double distance = this.getCenter().distance(pheromone.getPosition());
                    this.distancesToPheromones.put(pheromone, Math.abs(distance));
                }
                // The pheromone is exists in the distance map, just return the distance
                return Optional.of(this.distancesToPheromones.get(pheromone));
            }
            return Optional.empty();
        }

        /**
         * Locate the current ant instance for the target point (if returning set to false, the
         * target point will be the destination point, and source point if not)
         *
         * @return A pair containing 2 integers indicating the side of the target point for the
         *     current Ant instance. The first element of the pair indicates the position for the Ox
         *     axis, the second element indicates for the Oy axis. The convention for the sides is
         *     as follows: -1 := On the negative side of the origin 1 := On the positive side of the
         *     origin 0 := On the same axis as the origin Above origin will be center of the current
         *     Ant instance
         */
        private Pair<java.lang.Double, java.lang.Double> locationTargetPoint() {
            if (this.returning) {
                return this.locationTargetPoint(Board.this.sourcePoint.getCenter());
            }
            return this.locationTargetPoint(Board.this.destinationPoint.getCenter());
        }

        /**
         * Same as the locationTargetPoint() but with the given target point
         *
         * @param targetPoint The target point to calculate the position of the current ant
         * @return A pair containing 2 integers indicating the side of the target point for the
         *     current Ant instance. The first element of the pair indicates the position for the Ox
         *     axis, the second element indicates for the Oy axis. The convention for the sides is
         *     as follows: -1 := On the negative side of the origin 1 := On the positive side of the
         *     origin 0 := On the same axis as the origin Above origin will be center of the current
         *     Ant instance
         * @see Board.Ant#locationTargetPoint()
         */
        private Pair<java.lang.Double, java.lang.Double> locationTargetPoint(Point2D targetPoint) {
            return new Pair<>(
                    Math.signum(targetPoint.getX() - this.getX()),
                    Math.signum(this.getY() - targetPoint.getY()));
        }

        /**
         * Checks if the given pheromone is reachable for the current ant. A pheromone is considered
         * as reachable, if any only if there is not an obstacle insersects with the line from the
         * current ant's center to the given pheromone's position
         *
         * @param pheromone The pheromone to check
         * @return True if the given pheromone is reachable for the current ant, false if not
         */
        private boolean isPheromoneReachable(Board.Pheromone pheromone) {
            final Line2D vector = new Line2D.Double(this.getCenter(), pheromone.getPosition());
            return Board.this.obstacles.parallelStream()
                    .noneMatch(obstacle -> obstacle.intersectsLine(vector));
        }

        /** Move ant automatically to the calculated point */
        private void move() {
            Point2D to;

            to = this.calculateNewPoint();
            this.moveTo(to);
        }

        /**
         * ² Check if an ant colludes with any obstacles
         *
         * @return True if the given ant colludes with an obstacle
         */
        private boolean isColludesWithObstacle() {
            return Board.this.obstacles.parallelStream()
                    .anyMatch((Board.Obstacle o) -> Board.this.areComponentsColludes(o, this));
        }

        /**
         * Moves the given ant to the given point if possible
         *
         * @param to The new position of the ant
         */
        private void moveTo(Point2D to) {
            this.x = to.getX();
            this.y = to.getY();
            // TODO: Add pheromones on the line
            // If the ant still not arrived to the destination point, add a pheromone
            Board.this.addPheromone(this.getCenter(), 1);
            // Update all subscribers
            Board.this.subscribers.parallelStream()
                    .forEach(s -> s.onAntMoved(this.uuid, this.getCenter()));
            this.updateReturning();
        }

        private void updateReturning() {
            if (this.returning) {
                /*
                   Check if it's colludes with the destination point.
                   The returning value should be set to true until it arrives
                   back to the source point
                */
                this.returning =
                        Math.abs(this.getCenter().distance(Board.this.sourcePoint.getCenter()))
                                >= Board.this.settings.sourcePointRadius();
                return;
            }
            this.returning =
                    Math.abs(this.getCenter().distance(Board.this.destinationPoint.getCenter()))
                            < Board.this.settings.destinationPointRadius();
        }

        /**
         * Verify if we can move the given ant to the given point
         *
         * @param to The point to move the ant
         * @return True if the ant can be moved to the given point, false if not
         */
        private synchronized boolean canMove(Point2D to) {
            final Ant aux =
                    new Ant(
                            this.getX() + to.getX(),
                            this.getY() + to.getY(),
                            this.semiMinor,
                            this.semiMajor);
            // TODO: Check why Board.this.isInside(aux) keeps failing
            return /*Board.this.isInside(aux) &&*/ !aux.isColludesWithObstacle();
        }

        /**
         * Calculate distance between given pheromone and the current ant
         *
         * @param pheromone The pheromone to calculate the distance to
         * @return The distance between the given pheromone and the current ant
         */
        private double distanceToPheromone(Board.Pheromone pheromone) {
            return this.getCenter().distance(pheromone.getPosition());
        }

        /**
         * Calculate the threshold value for the given set of pheromones. The threshold value is
         * used for dispatching pheromones into different clusters. Can be imaginable the length of
         * the interval for the current cluster.
         *
         * @param pheromones The pheromones to calculate the threshold from
         * @return The threshold value for the given set of pheromones
         */
        private double calculateThreshold(final Set<Pheromone> pheromones) {
            final List<java.lang.Double> distances =
                    pheromones.parallelStream().map(this.distancesToPheromones::get).toList();
            double min = distances.get(0), max = distances.get(0);
            for (int i = 1; i < distances.size(); i++) {
                final double distance = distances.get(i);
                if (distance < min) {
                    min = distance;
                    continue;
                }
                if (max < distance) {
                    max = distance;
                }
            }
            return (max - min) / Board.this.settings.numberOfClusters();
        }

        /**
         * Create clusters from given list of pheromones and the given threshold
         *
         * @param pheromones The set of pheromones used for creating clusters
         * @param threshold The cluster threshold
         * @return A list of clusters
         */
        private List<Set<Board.Pheromone>> createClusters(
                Set<Board.Pheromone> pheromones, final double threshold) {
            final List<Set<Board.Pheromone>> clusters =
                    new ArrayList<>(Board.this.settings.numberOfClusters());
            for (int i = 0; i < Board.this.settings.numberOfClusters(); i++) {
                clusters.add(new HashSet<>());
            }
            for (Board.Pheromone pheromone : pheromones) {
                final double d = this.distancesToPheromones.get(pheromone);
                final int index = (int) (d / threshold) % clusters.size();
                clusters.get(index).add(pheromone);
            }
            return clusters;
        }

        /**
         * Method chooses the best cluster from the given list of clusters
         *
         * @param clusters The set of clusters to choose
         * @return The best cluster from the list of clusters,
         */
        private Set<Board.Pheromone> chooseCluster(final List<Set<Board.Pheromone>> clusters) {
            int maxIndex = 0;
            final Function<Integer, java.lang.Double> calculateMaxCoefficient =
                    (Integer index) -> {
                        final Set<Board.Pheromone> pheromones = clusters.get(index);
                        final List<Pair<Integer, java.lang.Double>> intensityDistancePairs =
                                pheromones.parallelStream()
                                        .map(
                                                (Board.Pheromone p) ->
                                                        new Pair<>(
                                                                p.intensity,
                                                                this.distancesToPheromones.get(p)))
                                        .toList();
                        Pair<Integer, java.lang.Double> cid =
                                intensityDistancePairs.parallelStream()
                                        .reduce(
                                                new Pair<>(0, 0.),
                                                (a, b) ->
                                                        new Pair<>(
                                                                a.getFirst() + b.getFirst(),
                                                                a.getSecond() + b.getSecond()));

                        return cid.getFirst() / cid.getSecond();
                    };
            double maxCoefficient = calculateMaxCoefficient.apply(maxIndex);
            for (int i = 1; i < clusters.size(); i++) {
                if (maxCoefficient < calculateMaxCoefficient.apply(i)) {
                    maxIndex = i;
                }
            }
            return clusters.get(maxIndex);
        }

        /**
         * Calculate a random point in direction to the source point
         *
         * @return Calculated random point for in the direction of the destination point
         */
        private Point2D getRandomPoint(Point2D targetPoint) {
            final Pair<java.lang.Double, java.lang.Double> sides =
                    this.locationTargetPoint(targetPoint);
            double x = Board.r.nextDouble(-1, 1), y = Board.r.nextDouble(-1, 1);
            if (sides.getFirst() < 0) {
                x = Board.r.nextDouble(sides.getFirst(), 0);
            }
            if (sides.getFirst() > 0) {
                x = Board.r.nextDouble(0, sides.getFirst());
            }
            final Point2D result = new Point2D.Double(this.x + x, this.y + (-1 * y));
            if (this.canMove(result)) {
                return result;
            }
            return getRandomPoint(targetPoint);
        }

        private Point2D getPointToTarget(double step, Point2D targetPoint) {
            double dx = targetPoint.getX() - this.getX();
            double dy = targetPoint.getY() - this.getY();
            return new Point2D.Double(this.x + step * dx, this.y + step * dy);
        }

        private Point2D getPointToTarget(Point2D targetPoint) {
            // TODO: Move step size to settings
            return this.getPointToTarget(0.01, targetPoint);
        }

        /**
         * Checks if the given pheromone is in the same way as the target point (ie destination
         * point or source point)
         *
         * @param pheromone The pheromone to check
         * @return True if the pheromone is on the same side as the target point
         */
        private boolean isPheromoneOnTheWay(Board.Pheromone pheromone) {
            final Pair<java.lang.Double, java.lang.Double> pheromoneSides =
                    this.locationTargetPoint(pheromone.getPosition());
            final Pair<java.lang.Double, java.lang.Double> targetPointSides =
                    this.locationTargetPoint();
            return pheromoneSides.equals(targetPointSides);
        }

        /**
         * Calculate new point to move
         *
         * @return The point to move the
         */
        private Point2D calculateNewPoint() {
            if (Board.this.pheromones.isEmpty()) {
                return getRandomPoint(Board.this.destinationPoint.getCenter());
            }
            /*
                 Pheromones that the current ant instance can reach.
            */
            // TODO: Take pheromones between the ant and the destination point
            Set<Pheromone> targetPheromones =
                    this.distancesToPheromones.keySet().parallelStream()
                            .filter(p -> p.intensity != 0 && this.isPheromoneOnTheWay(p))
                            .collect(Collectors.toSet());
            if (targetPheromones.size() == 0) {
                return getRandomPoint(
                        (this.returning ? Board.this.sourcePoint : Board.this.destinationPoint)
                                .getCenter());
            }
            double threshold;
            List<Set<Board.Pheromone>> clusters;
            // Iterate until there's more than one pheromone that we can reach
            while (targetPheromones.size() > 1) {
                /*
                   The threshold value is used for distributing pheromones to clusters.
                   This will be calculated each time from remaining points until the number of
                   remaining pheromones are equals to number of clusters
                */
                threshold = this.calculateThreshold(targetPheromones);
                clusters = createClusters(targetPheromones, threshold);
                targetPheromones = chooseCluster(clusters);
                if (clusters.parallelStream().filter(s -> s.size() != 0).count() == 1) {
                    /*
                       If there's only one non-empty cluster, the pheromones are too close
                    */
                    break;
                }
            }
            if (targetPheromones.size() > 0) {
                return getPointToTarget(targetPheromones.iterator().next().getPosition());
            }
            return getRandomPoint(Board.this.destinationPoint.getCenter());
        }

        @Override
        public int hashCode() {
            return this.uuid.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Ant) {
                return this.uuid.equals(((Ant) o).uuid);
            }
            return false;
        }
    }

    private enum BoardElementType {
        SOURCE_POINT,
        DESTINATION_POINT,
        OBSTACLE;

        public String toString() {
            return switch (this) {
                case OBSTACLE -> "Obstacle";
                case SOURCE_POINT -> "Source point";
                case DESTINATION_POINT -> "Destination point";
            };
        }
    }

    private static class EllipticalIElement extends Board.Ellipse implements IElement {

        /**
         * Creates an elliptical board element with given parameters
         *
         * @param x The x coordinate of the center of the ellipse
         * @param y The y coordinate of the center of the ellipse
         * @param semiMinor The length of the semi minor axis of the ellipse
         * @param semiMajor The length of the semi major axis of the ellipse
         */
        private EllipticalIElement(double x, double y, double semiMinor, double semiMajor) {
            super(x, y, semiMinor, semiMajor);
        }
    }

    private static class CircularElement extends Circle implements IElement {

        /**
         * Creates a Circle instance from given coordinates
         *
         * @param x The x coordinate of the center of the circle
         * @param y The y coordinate of the center of the circle
         * @param r The radius of the circle
         */
        public CircularElement(double x, double y, double r) {
            super(x, y, r);
        }

        /**
         * Creates a circle instance with center and the radius
         *
         * @param center The center of the circle
         * @param r The radius of the circle
         */
        @JsonCreator
        public CircularElement(
                @JsonProperty("center") Point center, @JsonProperty("radius") double r) {
            super(center, r);
        }

        @Override
        public String toString() {
            return "(%d,%d) radius: %f"
                    .formatted(
                            super.getCenter().getX(), super.getCenter().getY(), super.getRadius());
        }
    }

    private static class Obstacle extends Rectangle implements IElement {

        /**
         * Creates a new obstacle instance
         *
         * @param topLeftCorner The top left corner of the obstacle object
         * @param w Width of the obstacle
         * @param h Height of the obstacle
         */
        @JsonCreator
        public Obstacle(
                @JsonProperty("top-left-corner") Point topLeftCorner,
                @JsonProperty("width") int w,
                @JsonProperty("height") int h) {
            super(topLeftCorner.x, topLeftCorner.y, w, h);
        }

        /**
         * Creates an empty obstacle with the given top left corner
         *
         * @param topLeftCorner The top left corner of the obstacle
         */
        public Obstacle(Point topLeftCorner) {
            this(topLeftCorner, 0, 0);
        }

        @Override
        public Area getArea(Point p) {
            Point aux = new Point(this.getLocation().x + p.x, this.getLocation().y + p.y);
            return new Area(
                    new Rectangle(
                            aux.x,
                            aux.y,
                            (int) Math.ceil(super.getWidth()),
                            (int) Math.ceil(super.getHeight())));
        }
    }

    private interface IElement {

        /**
         * Get the area of the current board element with point p as origin of the parent plan
         *
         * @param p The projected origin point
         * @return Get the area of the current board element with for the given projected origin
         */
        Area getArea(Point p);

        Point getLocation();
    }

    public static class Circle extends Board.Ellipse {

        /**
         * Creates a Circle instance from given coordinates
         *
         * @param x The x coordinate of the center of the circle
         * @param y The y coordinate of the center of the circle
         * @param r The radius of the circle
         */
        public Circle(double x, double y, double r) {
            super(x, y, r, r);
        }

        /**
         * Creates a circle instance with center and the radius
         *
         * @param center The center of the circle
         * @param r The radius of the circle
         */
        public Circle(Point center, double r) {
            this(center.getX(), center.getY(), r);
        }

        // @JsonGetter("center")
        public Point2D getCenter() {
            return super.getCenter();
        }

        @JsonSetter("center")
        public void setCenter(Point p) {
            super.setCenter(p);
        }

        // @JsonGetter("radius")
        public double getRadius() {
            return super.semiMajor;
        }

        @JsonSetter("radius")
        public void setRadius(double r) {
            super.semiMajor = r;
            super.semiMinor = r;
        }
    }

    private static class Ellipse extends Ellipse2D {

        protected double x, y, semiMinor, semiMajor;

        /**
         * Creates an ellipse instance
         *
         * @param x The x coordinate of the center of the ellipse
         * @param y The y coordinate of the center of the ellipse
         * @param semiMinor The length of the semi minor axis of the ellipse
         * @param semiMajor The length of the semi major axis of the ellipse
         */
        protected Ellipse(double x, double y, double semiMinor, double semiMajor) {
            this.x = x;
            this.y = y;
            this.semiMinor = semiMinor;
            this.semiMajor = semiMajor;
        }

        /**
         * Creates a new ellipse instance
         *
         * @param center The center of the ellipse
         * @param semiMinor The length of the semi minor axis of the ellipse
         * @param semiMajor The length of the semi major axis of the ellipse
         */
        public Ellipse(Point center, double semiMinor, double semiMajor) {
            this(center.x, center.y, semiMinor, semiMajor);
        }

        /**
         * Get area of the current Circle element for the given projected origin
         *
         * @param projectedOrigin The origin point projected from the outer space
         * @return The area instance for the absolute position of the circle instance for the outer
         *     space.
         */
        public Area getArea(Point projectedOrigin) {
            final Point aux =
                    new Point(
                            projectedOrigin.x + this.getLocation().x,
                            projectedOrigin.y + this.getLocation().y);
            return new Area(
                    new Ellipse2D.Double(aux.x, aux.y, 2 * this.semiMajor, 2 * this.semiMinor));
        }

        /**
         * Returns the X coordinate of the upper-left corner of the framing rectangle in {@code
         * double} precision.
         *
         * @return the X coordinate of the upper-left corner of the framing rectangle.
         * @since 1.2
         */
        @Override
        public double getX() {
            return this.x - this.semiMajor;
        }

        /**
         * Returns the Y coordinate of the upper-left corner of the framing rectangle in {@code
         * double} precision.
         *
         * @return the Y coordinate of the upper-left corner of the framing rectangle.
         * @since 1.2
         */
        @Override
        public double getY() {
            return this.y - this.semiMinor;
        }

        /**
         * Returns the location of the upper left corner of the current circle instance
         *
         * @return The location of the upper left corner of the bounding rectangle of the circle
         */
        public Point getLocation() {
            return new Point((int) Math.ceil(this.getX()), (int) Math.ceil(this.getY()));
        }

        /**
         * Returns the width of the framing rectangle in {@code double} precision.
         *
         * @return the width of the framing rectangle.
         * @since 1.2
         */
        @Override
        public double getWidth() {
            return 2 * this.semiMajor;
        }

        /**
         * Returns the height of the framing rectangle in {@code double} precision.
         *
         * @return the height of the framing rectangle.
         * @since 1.2
         */
        @Override
        public double getHeight() {
            return 2 * this.semiMinor;
        }

        /**
         * Determines whether the {@code RectangularShape} is empty. When the {@code
         * RectangularShape} is empty, it encloses no area.
         *
         * @return {@code true} if the {@code RectangularShape} is empty; {@code false} otherwise.
         * @since 1.2
         */
        @Override
        public boolean isEmpty() {
            return this.semiMajor == 0 || this.semiMinor == 0;
        }

        /**
         * Sets the location and size of the framing rectangle of this {@code Shape} to the
         * specified rectangular values.
         *
         * @param x the X coordinate of the upper-left corner of the specified rectangular shape
         * @param y the Y coordinate of the upper-left corner of the specified rectangular shape
         * @param w the width of the specified rectangular shape
         * @param h the height of the specified rectangular shape
         * @see #getFrame
         * @since 1.2
         */
        @Override
        public void setFrame(double x, double y, double w, double h) {
            this.semiMinor = h / 2;
            this.semiMajor = w / 2;
            this.x = x + this.semiMajor;
            this.y = y + this.semiMinor;
        }

        /**
         * Returns a high precision and more accurate bounding box of the {@code Shape} than the
         * {@code getBounds} method. Note that there is no guarantee that the returned {@link
         * Rectangle2D} is the smallest bounding box that encloses the {@code Shape}, only that the
         * {@code Shape} lies entirely within the indicated {@code Rectangle2D}. The bounding box
         * returned by this method is usually tighter than that returned by the {@code getBounds}
         * method and never fails due to overflow problems since the return value can be an instance
         * of the {@code Rectangle2D} that uses double precision values to store the dimensions.
         *
         * <p>Note that the <a
         * href="{@docRoot}/java.desktop/java/awt/Shape.html#def_insideness">definition of
         * insideness</a> can lead to situations where points on the defining outline of the {@code
         * shape} may not be considered contained in the returned {@code bounds} object, but only in
         * cases where those points are also not considered contained in the original {@code shape}.
         *
         * <p>If a {@code point} is inside the {@code shape} according to the {@link
         * #contains(Point2D p) contains(point)} method, then it must be inside the returned {@code
         * Rectangle2D} bounds object according to the {@link #contains(Point2D p) contains(point)}
         * method of the {@code bounds}. Specifically:
         *
         * <p>{@code shape.contains(p)} requires {@code bounds.contains(p)}
         *
         * <p>If a {@code point} is not inside the {@code shape}, then it might still be contained
         * in the {@code bounds} object:
         *
         * <p>{@code bounds.contains(p)} does not imply {@code shape.contains(p)}
         *
         * @return an instance of {@code Rectangle2D} that is a high-precision bounding box of the
         *     {@code Shape}.
         * @see #getBounds
         * @since 1.2
         */
        @Override
        @JsonIgnore
        public Rectangle2D getBounds2D() {

            return new Rectangle2D.Double(
                    this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        /**
         * Get the point of the center of ellipse
         *
         * @return The center point of ellipse
         */
        public Point2D getCenter() {
            return new Point2D.Double(this.x, this.y);
        }

        public void setCenter(Point p) {
            this.x = p.x;
            this.y = p.y;
        }
    }
}
