/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.Board;
import aco.core.Settings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

class PreviewPanel extends AbstractMapPanel {

    private final Header header;
    private final PreviewContainer previewContainer;
    private final PreviewPanelController controller;

    PreviewPanel(Dimension size, Board board, Settings settings) {
        super(size, settings);
        this.controller = new PreviewPanelController(board);
        this.header = new Header();

        this.previewContainer = new PreviewContainer();
        this.setTopComponent(this.header);
        this.setBottomComponent(this.previewContainer);
    }

    private final class PreviewPanelController
            extends AbstractController.AbstractStatefulController<ACOPreviewState>
            implements Board.Subscriber {

        private final Board board;
        private BoardWorker boardWorker;
        private BoardPreviewWorker boardPreviewWorker;

        PreviewPanelController(Board board) {
            super(ACOPreviewState.PAUSED);
            this.board = board;
            this.board.subscribe(this);
        }

        /**
         * Method called on ant moved
         *
         * @param uuid The uuid of the ant to move
         * @param to The new position of the ant
         */
        @Override
        public void onAntMoved(UUID uuid, Point2D to) {
            PreviewPanel.this.previewContainer.previewBoard.moveAnt(uuid, to);
        }

        /**
         * The method called when new ants are spawned
         *
         * @param uuid The unique identifier of the created ant
         * @param location The location of the spawned ant
         */
        @Override
        public void onNewAntsSpawned(UUID uuid, Point2D location) {
            PreviewPanel.this.previewContainer.previewBoard.addAnt(uuid, location);
        }

        /**
         * Method called when the intensity of a pheromone changed
         *
         * @param position The position of the pheromone that its intensity changed
         * @param delta The delta of the intensity change
         */
        @Override
        public void onPheromoneIntensityChanged(Point2D position, int delta) {
            PreviewPanel.this.previewContainer.previewBoard.updatePheromoneIntensity(
                    position, delta);
        }

        /** Method called when a subscriber successfully subscribed to the board */
        @Override
        public void onSubscribed() {
            System.out.println("Subscribed to the board");
        }

        /** Method called when the subscriber unsubscribed successfully */
        @Override
        public void onUnsubscribe() {
            System.out.println("Unsubscribed from the board");
        }

        /**
         * Method called if the subscription is failed
         *
         * @param message The error message
         */
        @Override
        public void onSubscriptionFailed(String message) {
            System.err.println(message);
        }

        /**
         * Method called if the unsubscribe action fails
         *
         * @param message The error message
         */
        @Override
        public void onUnsubscribeFailed(String message) {
            System.err.println(message);
        }

        private ActionListener getHeaderButtonActionListener(
                PreviewPanel.Header.ACOPreviewButtonType buttonType) {
            // TODO: Complete implementation
            return switch (buttonType) {
                case START -> (ActionEvent ignore) -> {
                    this.initWorkers();
                    System.out.println("Running");
                    this.nextState();
                };
                case PAUSE -> (ActionEvent ignore) -> {
                    this.cancelWorkers();
                    this.nextState();
                };
                case OPEN_SETTINGS -> (ActionEvent ignore) -> {
                    System.out.println("Clicked on settings button");
                };
            };
        }

        /** Initialises all workers */
        private void initWorkers() {
            /*
             * Swing workers is made to be executed only once. That's why we need to reinitialise
             * each worker because otherwise calling execute on a canceled executor will return
             * error.
             */
            this.boardWorker = new BoardWorker();
            this.boardWorker.execute();

            this.boardPreviewWorker = new BoardPreviewWorker();
            this.boardPreviewWorker.execute();
        }

        /** Cancel all workers */
        private void cancelWorkers() {
            this.boardWorker.cancel();
            this.boardPreviewWorker.cancel();
        }

        @Override
        protected void updateButtonsVisibility() {
            // Settings button is enabled only once the execution is paused
            PreviewPanel.this
                    .header
                    .buttonsContainer
                    .buttons
                    .get(PreviewPanel.Header.ACOPreviewButtonType.OPEN_SETTINGS)
                    .setVisible(super.getCurrentState() == ACOPreviewState.PAUSED);
        }

        @Override
        protected boolean previousState() {
            return super.previousState(this.getCurrentState()::previous);
        }

        @Override
        protected boolean nextState() {
            return super.nextState(this.getCurrentState()::next);
        }

        @Override
        protected void updateChildren() {
            PreviewPanel.this.header.statusBar.updateDisplay();
            PreviewPanel.this.header.statusBar.revalidate();
            PreviewPanel.this.header.statusBar.repaint();

            PreviewPanel.this.header.buttonsContainer.updateButtons();
            PreviewPanel.this.header.buttonsContainer.revalidate();
            PreviewPanel.this.header.buttonsContainer.repaint();

            PreviewPanel.this.previewContainer.previewBoard.toggleRunner();
            PreviewPanel.this.previewContainer.revalidate();
            PreviewPanel.this.previewContainer.repaint();
        }

        private class BoardWorker extends SwingWorker<Void, Void> {

            /**
             * Computes a result, or throws an exception if unable to do so.
             *
             * <p>Note that this method is executed only once.
             *
             * <p>Note: this method is executed in a background thread.
             *
             * @return the computed result
             * @throws Exception if unable to compute a result
             */
            @Override
            protected Void doInBackground() throws Exception {
                PreviewPanelController.this.board.run();
                return null;
            }

            private void cancel() {
                PreviewPanelController.this.board.pause();
                super.cancel(true);
            }
        }

        private class BoardPreviewWorker extends SwingWorker<Void, Void> {

            /**
             * Computes a result, or throws an exception if unable to do so.
             *
             * <p>Note that this method is executed only once.
             *
             * <p>Note: this method is executed in a background thread.
             *
             * @return the computed result
             * @throws Exception if unable to compute a result
             */
            @Override
            protected Void doInBackground() throws Exception {
                while (true) {
                    PreviewPanel.this.previewContainer.previewBoard.revalidate();
                    PreviewPanel.this.previewContainer.previewBoard.repaint();
                }
            }

            protected void cancel() {
                super.cancel(true);
            }
        }
    }

    private final class PreviewContainer extends JPanel {

        private final PreviewBoard previewBoard;

        private PreviewContainer() {
            this.setMinimumSize(
                    new Dimension(
                            PreviewPanel.this.size.width,
                            (int)
                                    Math.ceil(
                                            PreviewPanel.this.size.height
                                                    * (1
                                                            - AbstractMapPanel
                                                                    .HEADER_DEFAULT_VERTICAL_RATIO))));
            this.setPreferredSize(
                    new Dimension(
                            PreviewPanel.this.size.width, 9 * PreviewPanel.this.size.height / 10));
            this.setLayout(null);
            this.setBackground(PreviewPanel.this.settings.mapFrameColor());

            this.previewBoard = new PreviewBoard();
            this.previewBoard.setBounds(PreviewPanel.this.controller.board);
            this.add(previewBoard);
        }

        private final class PreviewBoard extends AbstractBoardPanel {
            private final ConcurrentHashMap<UUID, Ellipse2D> ants;
            private final ConcurrentHashMap<Point2D, Integer> pheromones;

            private PreviewBoard() {
                this.ants = new ConcurrentHashMap<>();
                this.pheromones = new ConcurrentHashMap<>();
                this.setBorder(
                        BorderFactory.createLineBorder(
                                PreviewPanel.this.settings.boardBorderColor(), 3, false));
                this.setLayout(null);
            }

            /**
             * Add a new ant to the preview board
             *
             * @param center The center of the ant to add
             */
            private synchronized void addAnt(UUID uuid, Point2D center) {
                final Ellipse2D ant =
                        new Ellipse2D.Double(
                                center.getX() - PreviewPanel.this.settings.antsSemiMajorAxis(),
                                center.getY() - PreviewPanel.this.settings.antsSemiMinorAxis(),
                                2 * PreviewPanel.this.settings.antsSemiMajorAxis(),
                                2 * PreviewPanel.this.settings.antsSemiMinorAxis());
                this.ants.put(uuid, ant);
            }

            /**
             * Move the ant from one point to another.An ant should
             *
             * @param uuid The unique identifier of the ant to move
             * @param to The new point to move the ant
             */
            private synchronized void moveAnt(UUID uuid, Point2D to) {
                final Ellipse2D ant = this.ants.get(uuid);
                if (ant != null) {
                    ant.setFrame(
                            to.getX() - PreviewPanel.this.settings.antsSemiMajorAxis(),
                            to.getY() - PreviewPanel.this.settings.antsSemiMinorAxis(),
                            2 * PreviewPanel.this.settings.antsSemiMajorAxis(),
                            2 * PreviewPanel.this.settings.antsSemiMinorAxis());
                }
            }

            /**
             * Updates the intensity of the pheromone by the given value at the given location
             *
             * @param location The location of the pheromone to update
             * @param intensity The change in the intensity
             */
            private synchronized void updatePheromoneIntensity(Point2D location, int intensity) {
                this.pheromones.put(
                        location, (this.pheromones.getOrDefault(location, 0) + intensity));
            }

            private void toggleRunner() {
                // TODO: Complete function implementation
            }

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                final Color baseColor = PreviewPanel.this.settings.mapBackgroundColor();
                g2.setPaint(baseColor);
                g2.fill(
                        new Rectangle2D.Double(
                                0,
                                0,
                                PreviewPanel.this.controller.board.getWidth(),
                                PreviewPanel.this.controller.board.getHeight()));
                final Ellipse2D sourcePoint, destinationPoint;
                g2.setPaint(PreviewPanel.this.settings.sourcePointColor());

                sourcePoint =
                        new Ellipse2D.Double(
                                PreviewPanel.this.controller.board.getSourcePointLocation().getX(),
                                PreviewPanel.this.controller.board.getSourcePointLocation().getY(),
                                PreviewPanel.this.settings.sourcePointRadius() * 2,
                                PreviewPanel.this.settings.sourcePointRadius() * 2);
                g2.fill(sourcePoint);
                g2.setPaint(PreviewPanel.this.settings.destinationPointColor());
                destinationPoint =
                        new Ellipse2D.Double(
                                PreviewPanel.this
                                        .controller
                                        .board
                                        .getDestinationPointLocation()
                                        .getX(),
                                PreviewPanel.this
                                        .controller
                                        .board
                                        .getDestinationPointLocation()
                                        .getY(),
                                PreviewPanel.this.settings.destinationPointRadius() * 2,
                                PreviewPanel.this.settings.destinationPointRadius() * 2);
                g2.fill(destinationPoint);

                g2.setPaint(PreviewPanel.this.settings.obstacleColor());
                PreviewPanel.this.controller.board.getObstacles().parallelStream()
                        .forEach(
                                (Rectangle o) -> {
                                    g2.fill(o);
                                });
                g2.setPaint(PreviewPanel.this.settings.antColor());
                this.ants.values().parallelStream().forEach(g2::fill);
                this.pheromones.entrySet().parallelStream()
                        .forEach(
                                (Map.Entry<Point2D, Integer> p) -> {
                                    Color c =
                                            new Color(
                                                    PreviewPanel.this
                                                            .settings
                                                            .pheromoneColor()
                                                            .getRed(),
                                                    PreviewPanel.this
                                                            .settings
                                                            .pheromoneColor()
                                                            .getGreen(),
                                                    PreviewPanel.this
                                                            .settings
                                                            .pheromoneColor()
                                                            .getBlue(),
                                                    /*
                                                       Don't use float constructor of Color it will
                                                       require to divide all RGB values by 255f
                                                       with is not necessary, instead use modulus
                                                       on alpha with integer Color constructor
                                                    */
                                                    p.getValue() % 255);
                                    g2.setPaint(c);
                                    g2.fill(
                                            new Ellipse2D.Double(
                                                    p.getKey().getX() - 1,
                                                    p.getKey().getY() - 1,
                                                    2,
                                                    2));
                                });

                g2.setPaint(baseColor);
            }
        }
    }

    private final class Header extends AbstractHeader {
        private final ButtonsContainer buttonsContainer;
        private final StatusBar statusBar;

        private Header() {
            super();
            this.statusBar = new StatusBar();
            this.buttonsContainer = new ButtonsContainer();
            this.setLeftComponent(this.statusBar);
            this.setRightComponent(this.buttonsContainer);
        }

        private final class ButtonsContainer
                extends AbstractButtonsContainer<ACOPreviewButtonType> {

            private ButtonsContainer() {
                super(
                        PreviewPanel.this.controller::getHeaderButtonActionListener,
                        PreviewPanel.Header.ACOPreviewButtonType.values());
            }

            @Override
            protected void updateButtons() {
                super.removeAllElements();
                switch (PreviewPanel.this.controller.getCurrentState()) {
                    case PAUSED -> super.addX(ACOPreviewButtonType.START);
                    case STARTED -> super.addX(ACOPreviewButtonType.PAUSE);
                }
                super.addX(ACOPreviewButtonType.OPEN_SETTINGS);
            }
        }

        private final class StatusBar extends AbstractStatusBar {

            private void updateDisplay() {
                // TODO: Complete function implementation
            }
        }

        private enum ACOPreviewButtonType implements AbstractButtonType {
            START,
            PAUSE,
            OPEN_SETTINGS;

            @Override
            public String buttonString() {
                return switch (this) {
                    case START -> "Start";
                    case PAUSE -> "Pause";
                    case OPEN_SETTINGS -> "Settings";
                };
            }

            @Override
            public boolean getDefaultEnabledValue() {
                return true;
            }
        }
    }

    private enum ACOPreviewState implements AbstractState {
        PAUSED,
        STARTED;

        @Override
        public String tooltipText() {
            return null;
        }

        @Override
        public ACOPreviewState next() {
            return switch (this) {
                case PAUSED -> STARTED;
                case STARTED -> PAUSED;
            };
        }

        @Override
        public ACOPreviewState previous() {
            return this.next();
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public boolean hasPrevious() {
            return true;
        }

        @Override
        public int index() {
            return switch (this) {
                case STARTED -> 1;
                case PAUSED -> 0;
            };
        }

        public static ACOPreviewState fromIndex(int index) {
            return Arrays.stream(ACOPreviewState.values())
                    .filter(s -> s.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
