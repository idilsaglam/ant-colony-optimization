/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.Board;
import aco.core.Settings;
import aco.gui.AbstractMapPanel.AbstractController.AbstractStatefulController;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

class CreateMapPanel extends AbstractMapPanel implements VisitableMapPanel {

    private CreateMapController controller;
    private final Header header;
    private final MapBuilderPanel builderPanel;
    private final Consumer<Board.Builder> startCallback;

    CreateMapPanel(
            Dimension size,
            Consumer<Board.Builder> startCallback,
            Settings settings,
            Board.Builder boardBuilder) {
        super(size, settings);
        this.startCallback = startCallback;
        this.controller = new CreateMapController(boardBuilder);

        this.header = new Header();
        this.builderPanel = new MapBuilderPanel();
        super.setTopComponent(this.header);
        super.setBottomComponent(this.builderPanel);
    }

    public void accept(IVisitor visitor) {
        visitor.visit(
                this.controller.boardBuilder,
                this.controller::nextState,
                this.controller::updateBoardBuilder,
                CreateMapPanel.this.builderPanel.boardBuilder::setSourcePoint,
                CreateMapPanel.this.builderPanel.boardBuilder::setDestinationPoint,
                CreateMapPanel.this.builderPanel.boardBuilder::setObstacles);
    }

    private class CreateMapController extends AbstractStatefulController<MapBuilderStatus> {
        private final Board.Builder boardBuilder;
        // Singleton for the mapBuilderMouseAdapter
        private final MapBuilderMouseAdapter mapBuilderMouseAdapter;

        private CreateMapController(Board.Builder boardBuilder) {
            super(MapBuilderStatus.CREATE_BOARD);
            this.boardBuilder = boardBuilder;
            this.mapBuilderMouseAdapter = new MapBuilderMouseAdapter();
        }

        @Override
        protected boolean previousState() {
            return super.previousState(super.getCurrentState()::previous);
        }

        @Override
        protected boolean nextState() {
            return super.nextState(super.getCurrentState()::next);
        }

        /** Method updates all related child elements for the current controller instance. */
        protected void updateChildren() {
            CreateMapPanel.this.header.statusBar.setStep();
            CreateMapPanel.this.header.buttonsContainer.updateButtons();
            CreateMapPanel.this.header.statusBar.revalidate();
            CreateMapPanel.this.header.statusBar.repaint();
            CreateMapPanel.this.header.buttonsContainer.revalidate();
            CreateMapPanel.this.header.buttonsContainer.repaint();
            CreateMapPanel.this.builderPanel.setStep();
            CreateMapPanel.this.builderPanel.revalidate();
            CreateMapPanel.this.builderPanel.repaint();
        }

        /**
         * Get action listener for the current header button type
         *
         * @return An ActionListener for the current HeaderButtonType
         */
        private ActionListener getHeaderButtonActionListener(
                Header.ButtonsContainer.HeaderButtonType buttonType) {
            // TODO: Put the switch/case statement on top of the return statement
            return (ActionEvent ignore) -> {
                switch (buttonType) {
                    case SAVE -> {
                        final JSONFileChooser jsonFileChooser =
                                new JSONFileChooser(
                                        "Save the current map", CreateMapPanel.this, "Save");
                        final File selectedFile = jsonFileChooser.get();
                        // TODO: Save the current board to the selected file
                        try {
                            this.boardBuilder.save(selectedFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    case NEXT -> this.nextState(); // FIXME: Get result and handle
                    case START -> CreateMapPanel.this.startCallback.accept(
                            CreateMapPanel.this.controller.boardBuilder);
                    case PREVIOUS -> this.previousState();
                }
                ;
            };
        }

        /**
         * Update visibility of the buttons on the header following the necessary conditions for
         * each map builder step
         */
        protected void updateButtonsVisibility() {
            boolean isDisabled =
                    switch (CreateMapPanel.this.controller.getCurrentState()) {
                        case CREATE_BOARD -> ((CreateMapPanel.this.builderPanel.boardBuilder
                                                .getWidth()
                                        == 0)
                                && (CreateMapPanel.this.builderPanel.boardBuilder.getHeight()
                                        == 0));
                        case PLACE_SOURCE -> CreateMapPanel.this.controller.boardBuilder
                                        .getSourcePoint()
                                == null;
                        case PLACE_DESTINATION -> CreateMapPanel.this.controller.boardBuilder
                                        .getDestinationPoint()
                                == null;
                        case PLACE_OBSTACLES -> false;
                    };
            Arrays.stream(CreateMapPanel.Header.ButtonsContainer.HeaderButtonType.values())
                    .filter(bt -> !bt.getDefaultEnabledValue())
                    .forEach(
                            bt ->
                                    CreateMapPanel.this.header.buttonsContainer.setButtonEnabled(
                                            bt, !isDisabled));
        }

        /**
         * Add or update the source point on the given location. The source point will be updated or
         * added if the location does not collude with other elements and is inside of the area
         * occupied by the board builder
         *
         * @param location The location to add or update the source point
         */
        private void updateSourcePoint(Point location) {
            try {
                // We try to update the source point in the map builder model
                this.boardBuilder.setSourcePoint(
                        location, CreateMapPanel.this.settings.sourcePointRadius());
                // Check if the source point is available on the view
                if (CreateMapPanel.this.builderPanel.boardBuilder.sourcePoint == null) {
                    // If the source point successfully added to the board builder model, add to the
                    // view
                    CreateMapPanel.this.builderPanel.boardBuilder.setSourcePoint(location);
                    CreateMapPanel.this.controller.updateButtonsVisibility();
                } else {
                    // If there's an existing source point present on the board, we just
                    // need to update its location
                    CreateMapPanel.this.builderPanel.boardBuilder.sourcePoint.setLocation(location);
                }
                CreateMapPanel.this.builderPanel.boardBuilder.sourcePoint.revalidate();
                CreateMapPanel.this.builderPanel.boardBuilder.sourcePoint.repaint();
            } catch (Board.ElementOutOfBoundsException ignore) {
                System.err.println("Source point out of bounds");
            } catch (Board.ElementColludesWithOtherElementsOfBoardException ignore) {
                System.err.println("Source point colludes with other elements");
            }
        }

        /**
         * Add or update the destination point on the given location
         *
         * @param location The location to add or update the source point
         */
        private void updateDestinationPoint(Point location) {
            try {
                // First, we try to update the destination point on the board builder model
                this.boardBuilder.setDestinationPoint(
                        location, CreateMapPanel.this.settings.destinationPointRadius());
                // Check if the destination point is set on the view
                if (CreateMapPanel.this.builderPanel.boardBuilder.destinationPoint == null) {
                    // There's no destination point exists on the view
                    CreateMapPanel.this.builderPanel.boardBuilder.setDestinationPoint(location);
                    CreateMapPanel.this.controller.updateButtonsVisibility();
                } else {
                    // If there's already a destination point on the view, we just need to update
                    // its location
                    CreateMapPanel.this.builderPanel.boardBuilder.destinationPoint.setLocation(
                            location);
                }
                CreateMapPanel.this.builderPanel.boardBuilder.destinationPoint.revalidate();
                CreateMapPanel.this.builderPanel.boardBuilder.destinationPoint.repaint();
            } catch (Board.ElementOutOfBoundsException
                    | Board.ElementColludesWithOtherElementsOfBoardException ignore) {
            }
        }

        /**
         * Add obstacle to the given location
         *
         * @param location The top left corner of the obstacle to add
         */
        private void addObstacle(Point location) {
            try {
                this.boardBuilder.createObstacle(location);
                // If we can add the obstacle to the board builder model, we will add to the view
                CreateMapPanel.this.builderPanel.boardBuilder.setCurrentObstacle(location);
            } catch (Board.ElementOutOfBoundsException
                    | Board.ElementColludesWithOtherElementsOfBoardException ignore) {
                // If the obstacle cannot be added to the given point, nothing happens
            }
        }

        /**
         * Update the board builder rectangle area dimensions
         *
         * @param point The top left point of the board builder area
         */
        private void updateBoardBuilderDimension(Point point) {
            this.boardBuilder.resetEnclosingRectangle(point);
            this.updateBoardBuilder();
        }

        private void updateBoardBuilder() {
            CreateMapPanel.this.builderPanel.boardBuilder.setBounds(this.boardBuilder.getBounds());
            CreateMapPanel.this.builderPanel.revalidate();
            CreateMapPanel.this.builderPanel.repaint();
        }

        /**
         * Resize the board builder panel with the current point
         *
         * @param point The point on which we resize the board builder panel
         */
        private void resizeBoardBuilderPanel(Point point) {
            this.boardBuilder.setBounds(point);
            CreateMapPanel.this.builderPanel.boardBuilder.setBounds(this.boardBuilder.getBounds());
            CreateMapPanel.this.builderPanel.boardBuilder.updateLabelText();
            CreateMapPanel.this.builderPanel.revalidate();
            CreateMapPanel.this.builderPanel.repaint();
        }

        /**
         * Resize the last added obstacle following the given point
         *
         * @param point The point to resize the obstacle with
         */
        private void resizeLastObstacle(Point point) {
            try {
                this.boardBuilder.resizeCurrentObstacle(point);
                // FIXME: Setting bounds when moving to the left not working
                CreateMapPanel.this.builderPanel.boardBuilder.currentObstacle.setBounds(
                        Math.min(
                                CreateMapPanel.this.builderPanel.boardBuilder.currentObstacle
                                        .getX(),
                                point.x),
                        Math.min(
                                CreateMapPanel.this.builderPanel.boardBuilder.currentObstacle
                                        .getY(),
                                point.y),
                        Math.abs(
                                point.x
                                        - CreateMapPanel.this.builderPanel.boardBuilder
                                                .currentObstacle.getX()),
                        Math.abs(
                                point.y
                                        - CreateMapPanel.this.builderPanel.boardBuilder
                                                .currentObstacle.getY()));
                CreateMapPanel.this.builderPanel.boardBuilder.currentObstacle.revalidate();
                CreateMapPanel.this.builderPanel.boardBuilder.currentObstacle.repaint();
                CreateMapPanel.this.builderPanel.boardBuilder.revalidate();
                CreateMapPanel.this.builderPanel.boardBuilder.repaint();
            } catch (Board.ElementOutOfBoundsException ignore) {
                System.err.println("Obstacle out of bounds");
            } catch (Board.ElementColludesWithOtherElementsOfBoardException ignore) {
                System.err.println("Obstacle colludes with other elements of the board");
            }
        }

        private class MapBuilderMouseAdapter implements MouseMotionListener, MouseListener {

            /**
             * Invoked when the mouse button has been clicked (pressed and released) on a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                Point newLocation;
                switch (CreateMapPanel.this.controller.getCurrentState()) {
                    case PLACE_SOURCE -> {
                        newLocation =
                                new Point(
                                        e.getX() - CreateMapPanel.this.settings.sourcePointRadius(),
                                        e.getY()
                                                - CreateMapPanel.this.settings.sourcePointRadius());
                        CreateMapPanel.this.controller.updateSourcePoint(newLocation);
                        CreateMapPanel.this.builderPanel.boardBuilder.revalidate();
                        CreateMapPanel.this.builderPanel.boardBuilder.repaint();
                    }
                    case PLACE_DESTINATION -> {
                        newLocation =
                                new Point(
                                        e.getX()
                                                - CreateMapPanel.this.settings
                                                        .destinationPointRadius(),
                                        e.getY()
                                                - CreateMapPanel.this.settings
                                                        .destinationPointRadius());
                        CreateMapPanel.this.controller.updateDestinationPoint(newLocation);
                        CreateMapPanel.this.builderPanel.boardBuilder.revalidate();
                        CreateMapPanel.this.builderPanel.boardBuilder.repaint();
                    }
                }
            }

            /**
             * Invoked when a mouse button has been pressed on a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mousePressed(MouseEvent e) {
                switch (CreateMapPanel.this.controller.getCurrentState()) {
                    case CREATE_BOARD -> CreateMapPanel.this.controller.updateBoardBuilderDimension(
                            e.getPoint());
                    case PLACE_OBSTACLES -> CreateMapPanel.this.controller.addObstacle(
                            e.getPoint());
                }
            }

            /**
             * Invoked when a mouse button has been released on a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                if (CreateMapPanel.this.controller.getCurrentState()
                        == CreateMapPanel.MapBuilderStatus.PLACE_OBSTACLES) {
                    CreateMapPanel.this.controller.boardBuilder.addCurrentObstacle();
                    CreateMapPanel.this.builderPanel.boardBuilder.addCurrentObstacle();
                }
                CreateMapPanel.this.controller.updateButtonsVisibility();
                CreateMapPanel.this.header.buttonsContainer.revalidate();
                CreateMapPanel.this.header.buttonsContainer.repaint();
            }

            /**
             * Invoked when the mouse enters a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseEntered(MouseEvent e) {}

            /**
             * Invoked when the mouse exits a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseExited(MouseEvent e) {}

            /**
             * Invoked when a mouse button is pressed on a component and then dragged. {@code
             * MOUSE_DRAGGED} events will continue to be delivered to the component where the drag
             * originated until the mouse button is released (regardless of whether the mouse
             * position is within the bounds of the component).
             *
             * <p>Due to platform-dependent Drag&amp;Drop implementations, {@code MOUSE_DRAGGED}
             * events may not be delivered during a native Drag&amp;Drop operation.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseDragged(MouseEvent e) {
                switch (CreateMapPanel.this.controller.getCurrentState()) {
                    case CREATE_BOARD -> CreateMapPanel.this.controller.resizeBoardBuilderPanel(
                            e.getPoint());
                    case PLACE_OBSTACLES -> CreateMapPanel.this.controller.resizeLastObstacle(
                            e.getPoint());
                }
            }

            /**
             * Invoked when the mouse cursor has been moved onto a component but no buttons have
             * been pushed.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseMoved(MouseEvent e) {}
        }
    }

    private class Header extends AbstractHeader {
        private final StatusBar statusBar;
        private final ButtonsContainer buttonsContainer;

        private Header() {
            super();
            this.statusBar = new StatusBar();
            this.buttonsContainer = new ButtonsContainer();
            this.setLeftComponent(this.statusBar);
            this.setRightComponent(this.buttonsContainer);
        }

        private final class ButtonsContainer
                extends AbstractButtonsContainer<
                        CreateMapPanel.Header.ButtonsContainer.HeaderButtonType> {

            private ButtonsContainer() {
                super(
                        CreateMapPanel.this.controller::getHeaderButtonActionListener,
                        CreateMapPanel.Header.ButtonsContainer.HeaderButtonType.values());
            }

            /** Update buttons for the current status */
            protected void updateButtons() {
                // Remove all existing buttons
                super.removeAllElements();
                // Re-add necessary buttons
                switch (CreateMapPanel.this.controller.getCurrentState()) {
                    case CREATE_BOARD, PLACE_SOURCE -> super.addX(HeaderButtonType.NEXT);
                    case PLACE_DESTINATION -> {
                        super.addX(HeaderButtonType.PREVIOUS);
                        super.addX(HeaderButtonType.NEXT);
                        super.addX(HeaderButtonType.SAVE);
                        super.addX(HeaderButtonType.START);
                    }
                    case PLACE_OBSTACLES -> {
                        super.addX(HeaderButtonType.PREVIOUS);
                        super.addX(HeaderButtonType.SAVE);
                        super.addX(HeaderButtonType.START);
                    }
                }
            }

            /**
             * Set a button from the buttons' container enabled
             *
             * @param buttonType The type of the button to set
             * @param enabled Boolean value indicating that the button will be enabled or not
             */
            private void setButtonEnabled(HeaderButtonType buttonType, boolean enabled) {
                this.buttons.get(buttonType).setEnabled(enabled);
            }

            private enum HeaderButtonType implements AbstractButtonType {
                NEXT,
                START,
                SAVE,
                PREVIOUS;

                /**
                 * Get the string representation of the button types
                 *
                 * @return The string representation of the current HeaderButtonTypes instance
                 */
                public String buttonString() {
                    return switch (this) {
                        case NEXT -> "Next";
                        case SAVE -> "Save";
                        case START -> "Start";
                        case PREVIOUS -> "Previous";
                    };
                }

                /**
                 * Get the default enabled value for the current button type. All buttons should be
                 * disabled by default except for the previous button
                 *
                 * @return True if the button is enabled by default, false if not
                 */
                public boolean getDefaultEnabledValue() {
                    return this == PREVIOUS;
                }
            }
        }

        private final class StatusBar extends AbstractStatusBar {
            private final StatusBarCircle[] circles;

            private StatusBar() {
                this.circles = new StatusBarCircle[MapBuilderStatus.values().length];
                for (int i = 0; i < this.circles.length; i++) {
                    this.circles[i] =
                            new StatusBarCircle(
                                    Header.this.size.height / 2.,
                                    this.computeCircleColor(
                                            CreateMapPanel.this.controller.getCurrentState(), i),
                                    i,
                                    i == (this.circles.length - 1));
                    this.addX(this.circles[i]);
                }
            }

            /** Update the status bar steps with the controller data */
            private void setStep() {
                for (int i = 0; i < this.circles.length; i++) {
                    this.circles[i].setColor(
                            this.computeCircleColor(
                                    CreateMapPanel.this.controller.getCurrentState(), i));
                }
            }

            /**
             * Compute the color of the circle with current status and the index of the circle
             *
             * @param circleIndex The index of the circle to compute the index
             * @return The color of the circle
             */
            private Color computeCircleColor(MapBuilderStatus currentStatus, int circleIndex) {
                if (circleIndex < currentStatus.index()) {
                    return CreateMapPanel.this.settings.statusBarCircleCompletedColor();
                }
                if (circleIndex == currentStatus.index()) {
                    return CreateMapPanel.this.settings.statusBarCircleCurentColor();
                }
                return CreateMapPanel.this.settings.statusBarCircleNextColor();
            }

            private final class StatusBarCircle extends JPanel {

                private final double x, y, r;
                private Color color;
                private final boolean isLast;
                private final Dimension size;
                private final int index;

                private StatusBarCircle(double h, Color color, int index, boolean isLast) {
                    System.out.printf("Status bar circle constructor called with h %f\n", h);
                    this.r = h / 2.;
                    this.x = this.r;
                    this.y = this.r;
                    this.isLast = isLast;
                    System.out.printf(
                            "Status bar circle (%f,%f) and r = %f\n", this.x, this.y, this.r);
                    this.color = color;
                    this.index = index;
                    int w = (int) Math.ceil((this.isLast ? 1 : 2) * h);
                    this.size = new Dimension(w, (int) Math.ceil(h));
                    this.setSize(this.size);
                    this.setPreferredSize(this.size);
                    this.setToolTipText(MapBuilderStatus.fromIndex(index).tooltipText());
                }

                /**
                 * Update the color of the status bar circle
                 *
                 * @param c The new color to update
                 */
                void setColor(Color c) {
                    this.color = c;
                    this.revalidate();
                    this.repaint();
                }

                @Override
                public void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setPaint(this.color);
                    g2.fill(new Board.Circle(this.x, this.y, this.r));
                    String indexString = Integer.toString(this.index + 1);
                    Color c = g2.getColor();
                    // TODO: Check if the color is light, show a dark text color
                    g2.setColor(Color.WHITE);
                    Rectangle2D stringRectangle =
                            g2.getFont().getStringBounds(indexString, g2.getFontRenderContext());
                    g2.drawString(
                            indexString,
                            (int) Math.ceil(this.x - stringRectangle.getWidth() / 2),
                            (int) (this.y + stringRectangle.getHeight() / 4));
                    g2.setColor(c);
                    if (this.isLast) {
                        return;
                    }

                    final double rectangleHeight = 10;

                    g2.fill(
                            new Rectangle2D.Double(
                                    this.size.width / 2.,
                                    this.size.height / 2. - rectangleHeight / 2,
                                    this.size.width / 2.,
                                    rectangleHeight));
                }
            }
        }
    }

    private class MapBuilderPanel extends JPanel {

        private final BoardBuilderPanel boardBuilder;

        private MapBuilderPanel() {
            this.setMinimumSize(
                    new Dimension(
                            CreateMapPanel.this.size.width,
                            (int)
                                    Math.ceil(
                                            CreateMapPanel.this.size.height
                                                    * (1
                                                            - AbstractMapPanel
                                                                    .HEADER_DEFAULT_VERTICAL_RATIO))));
            this.setPreferredSize(
                    new Dimension(
                            CreateMapPanel.this.size.width,
                            9 * CreateMapPanel.this.size.height / 10));
            this.addMouseListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
            this.addMouseMotionListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
            this.boardBuilder = new BoardBuilderPanel();
            this.setLayout(null);
            this.add(this.boardBuilder);
        }

        /** Update the map builder and child components with current step */
        private void setStep() {
            if (CreateMapPanel.this.controller.getCurrentState() != MapBuilderStatus.CREATE_BOARD) {
                this.lock();
                this.boardBuilder.unlock();
            }
        }

        /** Lock the MapBuilderPanel and make it not clickable */
        private void lock() {
            this.removeMouseListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
            this.removeMouseMotionListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
            this.setBackground(CreateMapPanel.this.settings.mapFrameColor());
        }

        private final class BoardBuilderPanel extends AbstractBoardPanel {
            private final JLabel label;
            private RoundBoardElement sourcePoint, destinationPoint;
            private final Set<RectangularBoardElement> obstacles;
            private RectangularBoardElement currentObstacle;

            private BoardBuilderPanel() {
                this.obstacles = new HashSet<>();
                this.setBorder(
                        BorderFactory.createDashedBorder(
                                CreateMapPanel.this.settings.boardBuilderDraftBorderColor(),
                                3f,
                                2f));
                this.setBackground(new Color(0, 0, 0, 0));
                GridBagConstraints gbc = new GridBagConstraints();
                this.setLayout(new GridBagLayout());
                this.label = new JLabel();
                this.add(this.label, gbc);
            }

            /**
             * Sets the source point on the board builder panel
             *
             * @param location The location of the element to add
             */
            private void setSourcePoint(Point location) {
                this.sourcePoint =
                        new RoundBoardElement(
                                CreateMapPanel.this.settings.sourcePointColor(),
                                CreateMapPanel.this.settings.sourcePointRadius(),
                                location,
                                "The source point of ants");
                this.add(this.sourcePoint);
            }

            private void setSourcePoint() {
                this.setSourcePoint(
                        CreateMapPanel.this.controller.boardBuilder.getSourcePointLocation());
            }

            private void setDestinationPoint(Point location) {
                this.destinationPoint =
                        new RoundBoardElement(
                                CreateMapPanel.this.settings.destinationPointColor(),
                                CreateMapPanel.this.settings.destinationPointRadius(),
                                location,
                                "The destination point of ants");
                this.add(this.destinationPoint);
            }

            private void setDestinationPoint() {
                this.setDestinationPoint(
                        CreateMapPanel.this.controller.boardBuilder.getDestinationPointLocation());
            }

            /** Update the text of the label with the current size of the JPanel */
            private void updateLabelText() {
                this.label.setText(String.format("%dx%d", this.getWidth(), this.getHeight()));
            }

            /** Unlock the current board builder panel and make it clickable */
            private void unlock() {
                this.addMouseListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
                this.addMouseMotionListener(CreateMapPanel.this.controller.mapBuilderMouseAdapter);
                this.setBorder(
                        BorderFactory.createLineBorder(
                                CreateMapPanel.this.settings.boardBorderColor(), 3, false));
                this.label.setVisible(false);
                this.setBackground(CreateMapPanel.this.settings.mapBackgroundColor());
                this.setLayout(null);
                this.revalidate();
                this.repaint();
            }

            private void setObstacles() {
                CreateMapPanel.this.controller.boardBuilder.getObstacleBounds().parallelStream()
                        .forEach(
                                (Rectangle rectangle) -> {
                                    this.currentObstacle =
                                            new RectangularBoardElement(
                                                    CreateMapPanel.this.settings.obstacleColor(),
                                                    rectangle);
                                    this.add(this.currentObstacle);
                                    this.addCurrentObstacle();
                                });
            }

            /** Add an obstacle to the Set of Obstacles */
            private void addCurrentObstacle() {
                this.obstacles.add(this.currentObstacle);
                this.currentObstacle = null;
            }

            /**
             * Set the current obstacle to the given location
             *
             * @param location The location to add te obstacle
             */
            private void setCurrentObstacle(Point location) {
                this.currentObstacle =
                        new RectangularBoardElement(
                                CreateMapPanel.this.settings.obstacleColor(), location);
                this.add(this.currentObstacle);
            }
        }
    }

    private enum MapBuilderStatus implements AbstractState {
        CREATE_BOARD,
        PLACE_SOURCE,
        PLACE_DESTINATION,
        PLACE_OBSTACLES;

        /**
         * Get the tooltip text related to the current map builder status
         *
         * @return The tooltip text for the current map builder status
         */
        public String tooltipText() {
            return switch (this) {
                case CREATE_BOARD -> "Create board enclosing rectangle";
                case PLACE_SOURCE -> "Place source of ants on the board";
                case PLACE_DESTINATION -> "Place destination point on the board";
                case PLACE_OBSTACLES -> "Place obstacles on the board (Optional)";
            };
        }

        /**
         * Send the last status from the current map builder status
         *
         * @return The next map builder status from the current one. If the current status is
         *     already the last status, status will not change
         */
        public MapBuilderStatus next() {
            return switch (this) {
                case CREATE_BOARD -> PLACE_SOURCE;
                case PLACE_SOURCE -> PLACE_DESTINATION;
                default -> PLACE_OBSTACLES;
            };
        }

        /**
         * Verify if the current map builder status is the last one or not
         *
         * @return True if the current map builder status is not the last one, false if not
         */
        public boolean hasNext() {
            return this != PLACE_OBSTACLES;
        }

        /**
         * Get the index related to the current map builder status
         *
         * @return The index of the current map builder status
         */
        public int index() {
            return switch (this) {
                case CREATE_BOARD -> 0;
                case PLACE_SOURCE -> 1;
                case PLACE_DESTINATION -> 2;
                case PLACE_OBSTACLES -> 3;
            };
        }

        /**
         * Get the MapBuilderStatus related to given index
         *
         * @param index The index for the MapBuilderStatus
         * @return The MapBuilderStatus related to the given index
         */
        public static MapBuilderStatus fromIndex(int index) {
            return Arrays.stream(MapBuilderStatus.values())
                    .filter(s -> s.index() == index)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Verify if the current MapBuilderStatus has a previous value
         *
         * @return True if the current MapBuilderStatus is not the first one
         */
        public boolean hasPrevious() {
            return this != CREATE_BOARD;
        }

        /**
         * Get the previous value from the current map builder status
         *
         * @return The previous map builder status from the current status
         */
        public MapBuilderStatus previous() {
            return switch (this) {
                case PLACE_OBSTACLES -> PLACE_DESTINATION;
                case CREATE_BOARD, PLACE_SOURCE -> CREATE_BOARD;
                case PLACE_DESTINATION -> PLACE_SOURCE;
            };
        }
    }
}
