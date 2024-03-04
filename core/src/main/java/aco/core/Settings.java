/*
22015094 - SAGLAM Idil
*/
package aco.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record Settings(
        Color sourcePointColor,
        Color destinationPointColor,
        Color obstacleColor,
        Color mapBackgroundColor,
        Color antColor,
        Color mapFrameColor,
        Color statusBarCircleNextColor,
        Color statusBarCircleCurentColor,
        Color statusBarCircleCompletedColor,
        Color boardBorderColor,
        Color boardBuilderDraftBorderColor,
        Color mapBuilderDraftBackgroundColor,
        Color pheromoneColor,
        int numberOfAnts,
        int antsPerSecond,
        int sourcePointRadius,
        int destinationPointRadius,
        int mapBorderThickness,
        int pheromoneIntensityTimeoutMs,
        double antsSemiMinorAxis,
        double antsSemiMajorAxis,
        int threadpoolSize,
        int numberOfClusters) {

    public SettingsBuilder builder() {
        return new SettingsBuilder(
                this.sourcePointColor,
                this.destinationPointColor,
                this.obstacleColor,
                this.mapBackgroundColor,
                this.antColor,
                this.mapFrameColor,
                this.mapBorderThickness,
                this.sourcePointRadius,
                this.destinationPointRadius,
                this.statusBarCircleNextColor,
                this.statusBarCircleCompletedColor,
                this.statusBarCircleCompletedColor,
                this.boardBuilderDraftBorderColor,
                this.mapBuilderDraftBackgroundColor,
                this.antColor,
                this.numberOfAnts,
                this.antsPerSecond,
                this.pheromoneColor,
                this.pheromoneIntensityTimeoutMs,
                this.antsSemiMajorAxis,
                this.antsSemiMinorAxis,
                this.threadpoolSize,
                this.numberOfClusters);
    }

    public static class SettingsBuilder {

        private final Set<Subscriber> listeners;

        private Color sourcePointColor;

        private Color destinationPointColor;

        private Color obstacleColor;

        private Color mapBackgroundColor;

        private Color antColor;

        private Color mapFrameColor;

        private Color statusBarCircleNextColor;

        private Color statusBarCircleCurrentColor;

        private Color statusBarCircleCompletedColor;

        private Color boardBorderColor;

        private Color boardBuilderDraftBorderColor;

        private Color mapBuilderDraftBackgroundColor;

        private Color pheromoneColor;

        private int numberOfAnts, antsPerSecond;
        private int sourcePointRadius, destinationPointRadius;
        private int mapBorderThickness;
        private int pheromoneEvaporationTimeout;
        private double antSemiMinorAxis, antSemiMajorAxis;

        private int threadpoolSize;
        private int numberOfClusters;

        public SettingsBuilder() {
            this(
                    Color.YELLOW,
                    Color.GREEN,
                    Color.BLACK,
                    Color.BLACK,
                    Color.LIGHT_GRAY,
                    Color.GRAY,
                    3,
                    25,
                    25,
                    Color.LIGHT_GRAY,
                    Color.BLUE,
                    Color.GREEN,
                    Color.BLACK,
                    new Color(0, 0, 0, 0),
                    Color.BLACK,
                    10,
                    5,
                    Color.ORANGE,
                    1000,
                    5,
                    2,
                    5000,
                    10000);
        }

        public SettingsBuilder(
                Color sourcePointColor,
                Color destinationPointColor,
                Color obstacleColor,
                Color boardBorderColor,
                Color mapBackgroundColor,
                Color mapFrameColor,
                int mapBorderThickness,
                int sourcePointRadius,
                int destinationPointRadius,
                Color statusBarCircleNextColor,
                Color statusBarCircleCurrentColor,
                Color statusBarCircleCompletedColor,
                Color boardBuilderDraftBorderColor,
                Color mapBuilderDraftBackgroundColor,
                Color antColor,
                int numberOfAnts,
                int antsPerSecond,
                Color pheromoneColor,
                int pheromoneEvaporationTimeout,
                double antSemiMajorAxis,
                double antSemiMinorAxis,
                int threadpoolSize,
                int numberOfClusters) {
            this.listeners = new HashSet<>();
            // TODO: Add more granular settings (max, min value for sliders etc.)
            this.sourcePointColor = sourcePointColor;
            this.destinationPointColor = destinationPointColor;
            this.obstacleColor = obstacleColor;
            this.boardBorderColor = boardBorderColor;
            this.mapBackgroundColor = mapBackgroundColor;
            this.mapFrameColor = mapFrameColor;
            this.mapBorderThickness = mapBorderThickness;
            this.sourcePointRadius = sourcePointRadius;
            this.destinationPointRadius = destinationPointRadius;
            this.statusBarCircleNextColor = statusBarCircleNextColor;
            this.statusBarCircleCurrentColor = statusBarCircleCurrentColor;
            this.statusBarCircleCompletedColor = statusBarCircleCompletedColor;
            this.boardBuilderDraftBorderColor = boardBuilderDraftBorderColor;
            this.mapBuilderDraftBackgroundColor = mapBuilderDraftBackgroundColor;
            this.antColor = antColor;
            this.numberOfAnts = numberOfAnts;
            this.antsPerSecond = antsPerSecond;
            this.pheromoneColor = pheromoneColor;
            this.pheromoneEvaporationTimeout = pheromoneEvaporationTimeout;
            this.antSemiMajorAxis = antSemiMajorAxis;
            this.antSemiMinorAxis = antSemiMinorAxis;
            this.threadpoolSize = threadpoolSize;
            this.numberOfClusters = numberOfClusters;
        }

        /**
         * Read a JSON file and load in to the current SettingsBuilder instance
         *
         * @param path The path of the JSON file read
         * @return current SettingsBuilder instance loaded from the given JSON file
         */
        public SettingsBuilder load(String path) throws IOException {
            return this.load(Paths.get(path).toFile());
        }

        /**
         * Load the settings builder attributes from the given JSON file
         *
         * @param file The JSON file
         * @return The current SettingsBuilder instance with updated values
         * @throws IOException if the JSON file does not exist, or can not be opened
         */
        public SettingsBuilder load(File file) throws IOException {
            this.reader().readValue(file);
            return this;
        }

        /**
         * Load the other SettingsBuilder instance in to the current one
         *
         * @param o The other SettingsBuilder instance
         * @return The current SettingsBuilder instance
         */
        public SettingsBuilder load(SettingsBuilder o) {
            try {
                this.fromJSON(o.toJSON());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new IllegalArgumentException();
            }
            return this;
        }

        public SettingsBuilder reset() {
            this.load(new SettingsBuilder());
            return this;
        }

        /**
         * Add a new listener to the list of listeners
         *
         * @param subscriber The subscriber instance to add
         */
        public void subscribe(SettingsBuilder.Subscriber subscriber) {
            if (this.listeners.add(subscriber)) {
                subscriber.on(SettingsBuilder.Subscriber.Event.SUBSCRIBED);
            }
        }

        /**
         * Remove the given subscriber from the list of subscribers
         *
         * @param subscriber The subscriber instance to remove
         */
        public void unsubscribe(SettingsBuilder.Subscriber subscriber) {
            if (this.listeners.remove(subscriber)) {
                subscriber.on(SettingsBuilder.Subscriber.Event.UNSUBSCRIBED);
            }
        }

        /**
         * Save the current settings builder instance to the given file
         *
         * @param path The path of the file to write
         * @throws IOException if something wrong happens while saving the file
         * @throws IllegalArgumentException if one of the arguments in the current SettingsBuilder
         *     instance is not valid
         */
        public void save(String path) throws IOException, IllegalArgumentException {
            this.save(Paths.get(path).toFile());
        }

        public void save(File file) throws IOException, IllegalArgumentException {
            this.validate();
            ObjectMapperFactory.createSettingsBuilderSerializer()
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(file, this);
        }

        public SettingsBuilder fromJSON(String jsonString) throws JsonProcessingException {
            System.out.println(jsonString);
            this.reader().readValue(jsonString);
            return this;
        }

        public String toJSON() throws JsonProcessingException {
            this.validate();
            return ObjectMapperFactory.createSettingsBuilderSerializer()
                    .writer()
                    .writeValueAsString(this);
        }

        /**
         * Return the background color of the map builder draft
         *
         * @return The color of the map builder draft's background
         */
        @JsonGetter("map-builder-draft-background-color")
        public Color getMapBuilderDraftBackgroundColor() {
            return mapBuilderDraftBackgroundColor;
        }

        /**
         * Change the background color of the map builder draft
         *
         * @param mapBuilderDraftBackgroundColor The new background color
         */
        @JsonSetter("map-builder-draft-background-color")
        public void setMapBuilderDraftBackgroundColor(Color mapBuilderDraftBackgroundColor) {
            this.mapBuilderDraftBackgroundColor = mapBuilderDraftBackgroundColor;
            this.notifyListeners(
                    SettingsBuilder.Subscriber.Event.SELECTOR_BACKGROUND_COLOR_CHANGED);
        }

        /**
         * Return the border thickness of the map
         *
         * @return The border thickness of the map
         */
        @JsonGetter("map-border-thickness")
        public int getMapBorderThickness() {
            return this.mapBorderThickness;
        }

        /**
         * Updates the border thickness of the map
         *
         * @param mapBorderThickness The new border thickness value of the map
         */
        @JsonSetter("map-border-thickness")
        public void setMapBorderThickness(int mapBorderThickness) {
            this.mapBorderThickness = mapBorderThickness;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.MAP_BORDER_THICKNESS_CHANGED);
        }

        /**
         * Get the color of the source point
         *
         * @return The color of the source point
         */
        @JsonGetter("source-point-color")
        public Color getSourcePointColor() {
            return sourcePointColor;
        }

        /**
         * Updates the color of the source point
         *
         * @param sourcePointColor The new source point color value
         */
        @JsonSetter("source-point-color")
        public void setSourcePointColor(Color sourcePointColor) {
            this.sourcePointColor = sourcePointColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.SOURCE_POINT_COLOR_CHANGED);
        }

        /**
         * Get the color of the destination point
         *
         * @return The color of the destination point
         */
        @JsonGetter("destination-point-color")
        public Color getDestinationPointColor() {
            return destinationPointColor;
        }

        /**
         * Updates the color of the destination point
         *
         * @param destinationPointColor The new color value for the destination point
         */
        @JsonSetter("destination-point-color")
        public void setDestinationPointColor(Color destinationPointColor) {
            this.destinationPointColor = destinationPointColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.DESTINATION_POINT_COLOR_CHANGED);
        }

        /**
         * Get the color of the obstacle
         *
         * @return The color of the obstacle
         */
        @JsonGetter("obstacle-color")
        public Color getObstacleColor() {
            return obstacleColor;
        }

        /**
         * Updates the color of the obstacles
         *
         * @param obstacleColor The new obstacle color
         */
        @JsonSetter("obstacle-color")
        public void setObstacleColor(Color obstacleColor) {
            this.obstacleColor = obstacleColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.OBSTACLE_COLOR_CHANGED);
        }

        /**
         * Return the background color of the map
         *
         * @return The background color of the map
         */
        @JsonGetter("map-background-color")
        public Color getMapBackgroundColor() {
            return mapBackgroundColor;
        }

        /**
         * Updates the background color of the map
         *
         * @param mapBackgroundColor The new background color of the map
         */
        @JsonSetter("map-background-color")
        public void setMapBackgroundColor(Color mapBackgroundColor) {
            this.mapBackgroundColor = mapBackgroundColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.MAP_BACKGROUND_COLOR_CHANGED);
        }

        /**
         * Return the color of ants
         *
         * @return Color of ants
         */
        @JsonGetter("ant-color")
        public Color getAntColor() {
            return antColor;
        }

        /**
         * Updates the color of ants
         *
         * @param antColor The new color for ants
         */
        @JsonSetter("ant-color")
        public void setAntColor(Color antColor) {
            this.antColor = antColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.ANT_COLOR_CHANGED);
        }

        /**
         * Get the color of the map's frame
         *
         * @return The color of the map's frame
         */
        @JsonGetter("map-frame-color")
        public Color getMapFrameColor() {
            return mapFrameColor;
        }

        /**
         * Updates the color of the map's frame
         *
         * @param mapFrameColor The new color of the map frame
         */
        @JsonSetter("map-frame-color")
        public void setMapFrameColor(Color mapFrameColor) {
            this.mapFrameColor = mapFrameColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.MAP_FRAME_COLOR_CHANGED);
        }

        /**
         * Get the color of the next status in the status bar
         *
         * @return The color of the next state in the status bar
         */
        @JsonGetter("status-bar-circle-next-color")
        public Color getStatusBarCircleNextColor() {
            return statusBarCircleNextColor;
        }

        /**
         * Updates the color of the next state in the status bar
         *
         * @param statusBarCircleNextColor The new color for the next state in the status bar
         */
        @JsonSetter("status-bar-circle-next-color")
        public void setStatusBarCircleNextColor(Color statusBarCircleNextColor) {
            this.statusBarCircleNextColor = statusBarCircleNextColor;
            this.notifyListeners(
                    SettingsBuilder.Subscriber.Event.STATUS_BAR_CIRCLE_NEXT_COLOR_CHANGED);
        }

        /**
         * Get the color of the current state in the status bar
         *
         * @return The color of the current state in the status bar
         */
        @JsonGetter("status-bar-circle-current-color")
        public Color getStatusBarCircleCurrentColor() {
            return statusBarCircleCurrentColor;
        }

        /**
         * Updates the color of the current status on the status bar
         *
         * @param statusBarCircleCurrentColor The new color for the next status on the status bar
         */
        @JsonGetter("status-bar-circle-current-color")
        public void setStatusBarCircleCurrentColor(Color statusBarCircleCurrentColor) {
            this.statusBarCircleCurrentColor = statusBarCircleCurrentColor;
            this.notifyListeners(
                    SettingsBuilder.Subscriber.Event.STATUS_BAR_CIRCLE_CURRENT_COLOR_CHANGED);
        }

        /**
         * Get the color for the completed states on the status bar
         *
         * @return The color of the completed states on the status bar
         */
        @JsonGetter("status-bar-circle-completed-color")
        public Color getStatusBarCircleCompletedColor() {
            return statusBarCircleCompletedColor;
        }

        /**
         * Updates the color of the completed states in the status bar
         *
         * @param statusBarCircleCompletedColor The new color for the completed states in the status
         *     bar
         */
        @JsonSetter("status-bar-circle-completed-color")
        public void setStatusBarCircleCompletedColor(Color statusBarCircleCompletedColor) {
            this.statusBarCircleCompletedColor = statusBarCircleCompletedColor;
            this.notifyListeners(
                    SettingsBuilder.Subscriber.Event.STATUS_BAR_CIRCLE_COMPLETED_COLOR_CHANGED);
        }

        /**
         * Get the color of the board's border
         *
         * @return The color of the board's border
         */
        @JsonGetter("board-border-color")
        public Color getBoardBorderColor() {
            return boardBorderColor;
        }

        /**
         * Update the color of the board's border
         *
         * @param boardBorderColor The new color for the board's border
         */
        @JsonSetter("board-border-color")
        public void setBoardBorderColor(Color boardBorderColor) {
            this.boardBorderColor = boardBorderColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.BOARD_BORDER_COLOR_CHANGED);
        }

        /**
         * Get the color of the board builder draft
         *
         * @return The color of the board builder draft
         */
        @JsonGetter("board-builder-draft-border-color")
        public Color getBoardBuilderDraftBorderColor() {
            return boardBuilderDraftBorderColor;
        }

        /**
         * Updates the border color of the board builder draft
         *
         * @param boardBuilderDraftBorderColor The new color value for the board builder draft
         */
        @JsonSetter("board-builder-draft-border-color")
        public void setBoardBuilderDraftBorderColor(Color boardBuilderDraftBorderColor) {
            this.boardBuilderDraftBorderColor = boardBuilderDraftBorderColor;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.SELECTOR_BORDER_COLOR_CHANGED);
        }

        /**
         * Get the number of ants
         *
         * @return The number of ants
         */
        @JsonGetter("number-of-ants")
        public int getNumberOfAnts() {
            return numberOfAnts;
        }

        /**
         * Updates the number of ants
         *
         * @param numberOfAnts The number of ants
         */
        @JsonSetter("number-of-ants")
        public void setNumberOfAnts(int numberOfAnts) {
            SettingsBuilder.NumberSpinnerConfig antsConfig = this.numberOfAntsSpinnerConfig();
            if (antsConfig.verify(numberOfAnts)) {
                this.numberOfAnts = numberOfAnts;
                this.notifyListeners(SettingsBuilder.Subscriber.Event.NUMBER_OF_ANTS_CHANGED);
                return;
            }
            throw new IllegalArgumentException(
                    "%d <= %d <= %d is not true"
                            .formatted(antsConfig.min(), numberOfAnts, antsConfig.max()));
        }

        /**
         * Get the radius of the source point
         *
         * @return The radius of the source point
         */
        @JsonGetter("source-point-radius")
        public int getSourcePointRadius() {
            return sourcePointRadius;
        }

        /**
         * Update the radius of the source point
         *
         * @param sourcePointRadius The new radius of the source point
         */
        @JsonSetter("source-point-radius")
        public void setSourcePointRadius(int sourcePointRadius) {
            this.sourcePointRadius = sourcePointRadius;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.SOURCE_POINT_RADIUS_CHANGED);
        }

        /**
         * Update the size of the thread pool
         *
         * @param threadpoolSize The new size of the thread pool
         */
        @JsonSetter("thread-pool-size")
        public void setThreadpoolSize(int threadpoolSize) {
            this.threadpoolSize = threadpoolSize;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.THREAD_POOL_SIZE_CHANGED);
        }

        @JsonSetter("total-clusters")
        public void setNumberOfClusters(int nbClusters) {
            this.numberOfClusters = nbClusters;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.NUMBER_OF_CLUSTERS_CHANGED);
        }

        /**
         * Get the radius of the destination point
         *
         * @return The radius of the destination point
         */
        @JsonGetter("destination-point-radius")
        public int getDestinationPointRadius() {
            return destinationPointRadius;
        }

        /**
         * Update the radius of the destination point
         *
         * @param destinationPointRadius The radius of the destination point
         */
        @JsonSetter("destination-point-radius")
        public void setDestinationPointRadius(int destinationPointRadius) {
            this.destinationPointRadius = destinationPointRadius;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.DESTINATION_POINT_RADIUS_CHANGED);
        }

        /**
         * Get the value of the ants per second
         *
         * @return The number of ants per second
         */
        @JsonGetter("ants-per-second")
        public int getAntsPerSecond() {
            return this.antsPerSecond;
        }

        /**
         * Updates the number of ants per second
         *
         * @param antsPerSecond The new number of ants per second
         */
        @JsonSetter("ants-per-second")
        public void setAntsPerSecond(int antsPerSecond) {
            this.antsPerSecond = antsPerSecond;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.ANTS_PER_SECOND_CHANGED);
        }

        /**
         * Get the evaporation timeout value for a pheromone
         *
         * @return The evaporation timeout value for a pheromone
         */
        @JsonGetter("pheromone-evaporation-timeout")
        public int getPheromoneEvaporationTimeout() {
            return this.pheromoneEvaporationTimeout;
        }

        @JsonGetter("thread-pool-size")
        public int getThreadpoolSize() {
            return this.threadpoolSize;
        }

        /**
         * Get the total number of clusters
         *
         * @return The total number of clusters
         */
        @JsonGetter("total-clusters")
        public int getNumberOfClusters() {
            return this.numberOfClusters;
        }

        /**
         * Update the evaporation timeout for the pheromones
         *
         * @param timeOutMs The timeout value in milliseconds to evaporate a pheromone by 1
         *     intensity
         */
        @JsonSetter("pheromone-evaporation-timeout")
        public void setPheromoneEvaporationTimeout(int timeOutMs) {
            this.pheromoneEvaporationTimeout = timeOutMs;
            this.notifyListeners(
                    SettingsBuilder.Subscriber.Event.PHEROMONE_EVAPORATION_TIMEOUT_CHANGED);
        }

        /**
         * Get the semi minor axis value of the ellipse representing an ant
         *
         * @return The value of the semi minor axis representing an ant
         */
        @JsonGetter("ant-semi-minor-axis")
        public double getAntSemiMinorAxis() {
            return this.antSemiMinorAxis;
        }

        /**
         * Updates the semi minor axis value of the ellipse representing an ant
         *
         * @param val The value of the semi minor axis of the ellipse representing an ant
         */
        @JsonSetter("ant-semi-minor-axis")
        public void setAntSemiMinorAxis(double val) {
            this.antSemiMinorAxis = val;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.ANT_SEMI_MINOR_AXIS_CHANGED);
        }

        /**
         * Get the value of the semi major axis of the ellipse representing an ant
         *
         * @return The value of the semi major axis of the ellipse representing an ant
         */
        @JsonGetter("ant-semi-major-axis")
        public double getAntSemiMajorAxis() {
            return this.antSemiMajorAxis;
        }

        /**
         * Updates the semi major value of the ellipse representing an ant
         *
         * @param antSemiMajorAxis The semi major axis value of the ellipse representing an ant
         */
        @JsonSetter("ant-semi-major-axis")
        public void setAntSemiMajorAxis(double antSemiMajorAxis) {
            this.antSemiMajorAxis = antSemiMajorAxis;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.ANT_SEMI_MAJOR_AXIS_CHANGED);
        }

        /**
         * Get the color of a pheromone
         *
         * @return The color of a pheromone
         */
        @JsonGetter("pheromone-color")
        public Color getPheromoneColor() {
            return this.pheromoneColor;
        }

        /**
         * Update the color of a pheromone
         *
         * @param color The new color value for pheromone
         */
        @JsonSetter("pheromone-color")
        public void setPheromoneColor(Color color) {
            this.pheromoneColor = color;
            this.notifyListeners(SettingsBuilder.Subscriber.Event.PHEROMONE_COLOR_CHANGED);
        }

        public SettingsBuilder.SliderConfig mapBorderThicknessSliderConfig() {
            SettingsBuilder.SliderConfig.Builder sliderConfigBuilder =
                    new SettingsBuilder.SliderConfig.Builder();
            sliderConfigBuilder
                    .minValue(0)
                    .maxValue(10)
                    .minorTickSpacing(1)
                    .majorTickSpacing(5)
                    .paintTicks(true)
                    .paintLabels(true)
                    .supplier(this::getMapBorderThickness)
                    .event(SettingsBuilder.Subscriber.Event.MAP_BORDER_THICKNESS_CHANGED);
            return sliderConfigBuilder.build();
        }

        public SettingsBuilder.SliderConfig sourcePointRadiusSliderConfig() {
            SettingsBuilder.SliderConfig.Builder builder =
                    new SettingsBuilder.SliderConfig.Builder();
            builder.minValue(10)
                    .maxValue(110)
                    .majorTickSpacing(50)
                    .minorTickSpacing(10)
                    .paintTicks(true)
                    .paintLabels(true)
                    .supplier(this::getSourcePointRadius)
                    .event(SettingsBuilder.Subscriber.Event.SOURCE_POINT_RADIUS_CHANGED);

            return builder.build();
        }

        public SettingsBuilder.SliderConfig destinationPointRadiusSliderConfig() {
            SettingsBuilder.SliderConfig.Builder builder =
                    new SettingsBuilder.SliderConfig.Builder();
            builder.minValue(10)
                    .maxValue(110)
                    .majorTickSpacing(50)
                    .minorTickSpacing(10)
                    .paintTicks(true)
                    .paintLabels(true)
                    .supplier(this::getDestinationPointRadius)
                    .event(SettingsBuilder.Subscriber.Event.DESTINATION_POINT_RADIUS_CHANGED);

            return builder.build();
        }

        public SettingsBuilder.NumberSpinnerConfig numberOfAntsSpinnerConfig() {
            SettingsBuilder.NumberSpinnerConfig.Builder builder =
                    new SettingsBuilder.NumberSpinnerConfig.Builder();
            builder.min(1)
                    .max(Integer.MAX_VALUE)
                    .step(1000)
                    .supplier(this::getNumberOfAnts)
                    .event(SettingsBuilder.Subscriber.Event.NUMBER_OF_ANTS_CHANGED);
            return builder.build();
        }

        public SettingsBuilder.NumberSpinnerConfig antsPerSecondSpinnerConfig() {
            SettingsBuilder.NumberSpinnerConfig.Builder builder =
                    new SettingsBuilder.NumberSpinnerConfig.Builder();
            builder.min(1)
                    .max(this.getNumberOfAnts())
                    .step(2)
                    .supplier(this::getAntsPerSecond)
                    .event(SettingsBuilder.Subscriber.Event.ANTS_PER_SECOND_CHANGED);
            return builder.build();
        }

        public SettingsBuilder.NumberSpinnerConfig pheromoneEvaporationTimeoutSpinnerConfig() {
            SettingsBuilder.NumberSpinnerConfig.Builder builder =
                    new SettingsBuilder.NumberSpinnerConfig.Builder();
            builder.min(100)
                    .max(Integer.MAX_VALUE)
                    .step(100)
                    .supplier(this::getPheromoneEvaporationTimeout)
                    .event(SettingsBuilder.Subscriber.Event.PHEROMONE_EVAPORATION_TIMEOUT_CHANGED);
            return builder.build();
        }

        public SettingsBuilder.NumberSpinnerConfig threadPoolSizeConfig() {
            SettingsBuilder.NumberSpinnerConfig.Builder builder =
                    new SettingsBuilder.NumberSpinnerConfig.Builder();
            builder.min(1)
                    .max(10000)
                    .step(10)
                    .supplier(this::getThreadpoolSize)
                    .event(SettingsBuilder.Subscriber.Event.THREAD_POOL_SIZE_CHANGED);
            return builder.build();
        }

        public SettingsBuilder.NumberSpinnerConfig numberOfClustersConfig() {
            SettingsBuilder.NumberSpinnerConfig.Builder builder =
                    new SettingsBuilder.NumberSpinnerConfig.Builder();
            builder.min(3)
                    .max(Integer.MAX_VALUE)
                    .step(1)
                    .supplier(this::getNumberOfClusters)
                    .event(SettingsBuilder.Subscriber.Event.NUMBER_OF_CLUSTERS_CHANGED);
            return builder.build();
        }

        /**
         * Creates a Settings instance from the current SettingsBuilder
         *
         * @return The settings instance created from the current SettingsBuilder
         */
        public Settings build() {
            this.validate();
            return new Settings(
                    this.sourcePointColor,
                    this.destinationPointColor,
                    this.obstacleColor,
                    this.mapBackgroundColor,
                    this.antColor,
                    this.mapFrameColor,
                    this.statusBarCircleNextColor,
                    this.statusBarCircleCurrentColor,
                    this.statusBarCircleCompletedColor,
                    this.boardBorderColor,
                    this.boardBuilderDraftBorderColor,
                    this.mapBuilderDraftBackgroundColor,
                    this.pheromoneColor,
                    this.numberOfAnts,
                    this.antsPerSecond,
                    this.sourcePointRadius,
                    this.destinationPointRadius,
                    this.mapBorderThickness,
                    this.pheromoneEvaporationTimeout,
                    this.antSemiMinorAxis,
                    this.antSemiMajorAxis,
                    this.threadpoolSize,
                    this.numberOfClusters);
        }

        /**
         * Checks the current settings builder configuration
         *
         * @throws IllegalArgumentException if one of the arguments are correct in the current
         *     settings builder
         */
        private void validate() throws IllegalArgumentException {
            // TODO: Complete methods implementation
        }

        /**
         * Notify all listener for the given event
         *
         * @param event The event to notify listeners with
         */
        private void notifyListeners(SettingsBuilder.Subscriber.Event event) {
            this.listeners.parallelStream().forEach(s -> s.on(event));
        }

        private ObjectReader reader() {
            final ObjectMapper mapper = ObjectMapperFactory.createSettingsBuilderDeserializer();
            return mapper.readerForUpdating(this);
        }

        public interface Subscriber {

            void on(Event event);

            enum Event {
                SUBSCRIBED,
                UNSUBSCRIBED,
                SOURCE_POINT_COLOR_CHANGED,
                DESTINATION_POINT_COLOR_CHANGED,
                OBSTACLE_COLOR_CHANGED,
                BOARD_BORDER_COLOR_CHANGED,
                MAP_BACKGROUND_COLOR_CHANGED,
                MAP_FRAME_COLOR_CHANGED,
                MAP_BORDER_THICKNESS_CHANGED,
                SOURCE_POINT_RADIUS_CHANGED,
                DESTINATION_POINT_RADIUS_CHANGED,
                STATUS_BAR_CIRCLE_COMPLETED_COLOR_CHANGED,
                STATUS_BAR_CIRCLE_CURRENT_COLOR_CHANGED,
                STATUS_BAR_CIRCLE_NEXT_COLOR_CHANGED,
                SELECTOR_BORDER_COLOR_CHANGED,
                SELECTOR_BACKGROUND_COLOR_CHANGED,
                ANT_COLOR_CHANGED,
                NUMBER_OF_ANTS_CHANGED,
                ANTS_PER_SECOND_CHANGED,
                PHEROMONE_COLOR_CHANGED,
                PHEROMONE_EVAPORATION_TIMEOUT_CHANGED,
                ANT_SEMI_MAJOR_AXIS_CHANGED,
                ANT_SEMI_MINOR_AXIS_CHANGED,
                THREAD_POOL_SIZE_CHANGED,
                NUMBER_OF_CLUSTERS_CHANGED;
            }
        }

        public record NumberSpinnerConfig(
                int min,
                int max,
                int step,
                Supplier<Integer> supplier,
                SettingsBuilder.Subscriber.Event event) {

            boolean verify(int value) {
                return this.min <= value && value <= this.max;
            }

            private static class Builder {

                private int min;
                private int max;
                private int step;
                private Supplier<Integer> supplier;
                private SettingsBuilder.Subscriber.Event event;

                Builder min(int min) {
                    this.min = min;
                    return this;
                }

                Builder max(int max) {
                    this.max = max;
                    return this;
                }

                Builder step(int step) {
                    this.step = step;
                    return this;
                }

                Builder supplier(Supplier<Integer> supplier) {
                    this.supplier = supplier;
                    return this;
                }

                Builder event(SettingsBuilder.Subscriber.Event event) {
                    this.event = event;
                    return this;
                }

                NumberSpinnerConfig build() {
                    return new NumberSpinnerConfig(
                            this.min, this.max, this.step, this.supplier, this.event);
                }
            }
        }

        public record SliderConfig(
                int minValue,
                int maxValue,
                int minorTickSpacing,
                int majorTickSpacing,
                boolean paintTicks,
                boolean paintLabels,
                Supplier<Integer> supplier,
                SettingsBuilder.Subscriber.Event event) {

            private static class Builder {

                private int minValue;
                private int maxValue;
                private int majorTickSpacing;
                private int minorTickSpacing;
                private boolean paintTicks;
                private boolean paintLabels;
                private Supplier<Integer> supplier;
                private SettingsBuilder.Subscriber.Event event;

                Builder minValue(int minValue) {
                    this.minValue = minValue;
                    return this;
                }

                Builder maxValue(int maxValue) {
                    this.maxValue = maxValue;
                    return this;
                }

                Builder majorTickSpacing(int majorTickSpacing) {
                    this.majorTickSpacing = majorTickSpacing;
                    return this;
                }

                Builder minorTickSpacing(int minorTickSpacing) {
                    this.minorTickSpacing = minorTickSpacing;
                    return this;
                }

                Builder paintLabels(boolean paintLabels) {
                    this.paintLabels = paintLabels;
                    return this;
                }

                Builder paintTicks(boolean paintTicks) {
                    this.paintTicks = paintTicks;
                    return this;
                }

                Builder supplier(Supplier<Integer> supplier) {
                    this.supplier = supplier;
                    return this;
                }

                Builder event(SettingsBuilder.Subscriber.Event event) {
                    this.event = event;
                    return this;
                }

                SliderConfig build() {
                    return new SliderConfig(
                            this.minValue,
                            this.maxValue,
                            this.minorTickSpacing,
                            this.majorTickSpacing,
                            this.paintTicks,
                            this.paintLabels,
                            this.supplier,
                            this.event);
                }
            }
        }
    }
}
