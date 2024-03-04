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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

abstract class AbstractMapPanel extends JSplitPane {
    protected static final double HEADER_DEFAULT_VERTICAL_RATIO = 1 / 10.;
    protected final Settings settings;
    protected final Dimension size;

    protected AbstractMapPanel(Dimension size, Settings settings) {
        this.settings = settings;
        this.size = size;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setOrientation(VERTICAL_SPLIT);
    }

    protected abstract class AbstractHeader extends JSplitPane {
        protected static final double HEADER_STATUS_BAR_DEFAULT_HORIZONTAL_RATIO = 7. / 10;
        protected final Dimension size;

        protected AbstractHeader() {
            this.size =
                    new Dimension(
                            AbstractMapPanel.this.size.width,
                            (int)
                                    Math.ceil(
                                            AbstractMapPanel.this.size.height
                                                    * AbstractMapPanel
                                                            .HEADER_DEFAULT_VERTICAL_RATIO));
            this.setMinimumSize(this.size);
            this.setPreferredSize(this.size);
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setOrientation(HORIZONTAL_SPLIT);
        }

        protected abstract class AbstractStatusBar extends JPanel {
            private final GridBagConstraints gbc;

            protected AbstractStatusBar() {
                this.gbc = new GridBagConstraints();
                this.setLayout(new GridBagLayout());
                Dimension size =
                        new Dimension(
                                (int)
                                        Math.ceil(
                                                AbstractHeader.this.size.width
                                                        * HEADER_STATUS_BAR_DEFAULT_HORIZONTAL_RATIO),
                                AbstractHeader.this.size.height);
                this.setMinimumSize(size);
                this.setPreferredSize(size);
                this.gbc.gridx = 0;
                this.gbc.gridy = 0;
            }

            /**
             * Method adds component horizontally to the grid bag constraints.
             *
             * @param component JComponent that we want to add into the grid bag constraints.
             */
            protected void addX(JComponent component) {
                this.add(component, this.gbc);
                this.gbc.gridx++;
            }

            /**
             * Method adds component vertically to the grid bag constraints.
             *
             * @param component JComponent that we want to add into the grid bag constraints.
             */
            protected void addY(JComponent component) {
                this.add(component, gbc);
                this.gbc.gridy++;
            }
        }

        protected abstract class AbstractButtonsContainer<T extends Enum<T> & AbstractButtonType>
                extends JPanel {
            protected final Map<T, JButton> buttons;
            private final GridBagConstraints gbc;

            protected AbstractButtonsContainer(
                    Function<T, ActionListener> getButtonActionListener, T[] availableButtonTypes) {
                this.gbc = new GridBagConstraints();
                this.buttons = new HashMap<>();
                Dimension size =
                        new Dimension(
                                (int)
                                        Math.ceil(
                                                AbstractHeader.this.size.width
                                                        * (1
                                                                - AbstractHeader
                                                                        .HEADER_STATUS_BAR_DEFAULT_HORIZONTAL_RATIO)),
                                AbstractHeader.this.size.height);
                this.setMinimumSize(size);
                this.setPreferredSize(size);
                this.setLayout(new GridBagLayout());
                for (T bt : availableButtonTypes) {
                    this.buttons.put(bt, this.buttonBuilder(bt, getButtonActionListener.apply(bt)));
                }
                updateButtons();
            }

            protected abstract void updateButtons();

            /** Removes all children and re-initialise the GridBagContraints */
            protected void removeAllElements() {
                this.removeAll();
                this.gbc.gridx = 0;
                this.gbc.gridy = 0;
            }

            /**
             * Adds the given button type on the X axis
             *
             * @param buttonType The type of the button to add
             */
            protected void addX(T buttonType) {
                this.addX(this.buttons.get(buttonType));
            }

            /**
             * Adds the given button type on the Y axis
             *
             * @param buttonType The type of the button to add
             */
            protected void addY(T buttonType) {
                this.addY(this.buttons.get(buttonType));
            }

            /**
             * Adds the given component on the Y axis
             *
             * @param component The component to add
             */
            private void addY(JComponent component) {
                this.add(component, this.gbc);
                this.gbc.gridy++;
            }

            /**
             * Adds the given component on the X axis
             *
             * @param component The component to add
             */
            private void addX(JComponent component) {
                this.add(component, this.gbc);
                this.gbc.gridx++;
            }
            /**
             * Create JButton instance for the given button type and with the given action listener
             * generator
             *
             * @param buttonType The type of the button to generate
             * @param actionListener The action listener to add to the generated button
             * @return The JButton instance with for the give button type and the given generated
             *     listener
             */
            private JButton buttonBuilder(T buttonType, ActionListener actionListener) {
                JButton button = new JButton();
                button.setText(buttonType.buttonString());
                button.setEnabled(buttonType.getDefaultEnabledValue());
                button.addActionListener(actionListener);
                return button;
            }
        }

        protected interface AbstractButtonType {
            String buttonString();

            boolean getDefaultEnabledValue();
        }
    }

    protected abstract class AbstractBoardPanel extends JPanel {

        static final class EllipticalBoardElement extends JPanel {
            private final Color color;
            private final double semiMinor, semiMajor;
            private Point center;
            private final Ellipse2D ellipse;

            /**
             * Create an elliptical board element with given parameters
             *
             * @param color The color of the element
             * @param center The center of the ellipse
             * @param semiMajor The length of the semi major axis of the ellipse
             * @param semiMinor The length of the semi minor axis of the ellipse
             */
            EllipticalBoardElement(
                    Color color,
                    Point center,
                    double semiMajor,
                    double semiMinor,
                    String tooltipText) {
                this.color = color;
                this.semiMajor = semiMajor;
                this.semiMinor = semiMinor;
                this.center = center;
                this.setToolTipText(tooltipText);
                final int tlx = (int) Math.ceil(center.x - semiMajor),
                        tly = (int) Math.ceil(center.x - semiMinor),
                        w = (int) Math.ceil(2 * semiMajor),
                        h = (int) Math.ceil(2 * semiMinor);
                this.setBounds(tlx, tly, w, h);
                this.ellipse = new Ellipse2D.Double(0, 0, 2 * semiMajor, 2 * semiMinor);
            }

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(this.color);
                g2.fill(this.ellipse);
            }

            /**
             * Updates the location of the center point
             *
             * @param p The new center of the JPanel
             */
            public void setCenter(Point p) {
                this.center = p;
                this.setLocation(getLocation());
            }

            @Override
            public Point getLocation() {
                return new Point(
                        (int) Math.ceil(this.center.x - this.semiMajor),
                        (int) Math.ceil(this.center.y - this.semiMinor));
            }
        }

        static final class RectangularBoardElement extends JPanel {

            /**
             * Create a rectangular board element instance with given parameters
             *
             * @param color The color of the obstacle
             * @param tlCorner The coordinates of the top left corner of the rectangle
             */
            RectangularBoardElement(Color color, Point tlCorner) {
                this.setBackground(color);
                this.setLocation(tlCorner);
            }

            /**
             * Create a rectangular board element with color and bounds
             *
             * @param color The color of the element
             * @param bounds The bounds of the rectangle
             */
            RectangularBoardElement(Color color, Rectangle bounds) {
                this.setBackground(color);
                this.setBounds(bounds);
            }
        }

        static final class RoundBoardElement extends JPanel {

            private final Color color;
            private final int radius;
            private final Board.Circle circle;

            /**
             * Create a round board element with given parameters
             *
             * @param color The color of the element
             * @param radius The radius of the circle
             * @param topLeftCorner The top left corner of the JPanel
             * @param tooltipText The tooltip text
             */
            RoundBoardElement(Color color, int radius, Point topLeftCorner, String tooltipText) {
                this.color = color;
                this.radius = radius;
                this.setBounds(topLeftCorner.x, topLeftCorner.y, 2 * radius, 2 * radius);

                this.circle = new Board.Circle(this.radius, this.radius, this.radius);
                this.setToolTipText(tooltipText);
            }

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(this.color);
                g2.fill(this.circle);
            }
        }
    }

    protected abstract static class AbstractController {

        protected abstract static class AbstractStatefulController<
                T extends Enum<T> & AbstractState> {
            private T currentState;

            AbstractStatefulController(T currentState) {
                this.currentState = currentState;
            }

            /**
             * Get the current state of the controller
             *
             * @return The current state of the controller
             */
            protected T getCurrentState() {
                return this.currentState;
            }

            /**
             * Pass to the next state if possible
             *
             * @param nextStateSupplier The supplier for the next state from the current state
             * @return True if passed to the next state successfully
             */
            protected boolean nextState(Supplier<T> nextStateSupplier) {
                if (this.currentState.hasNext()) {
                    this.currentState = nextStateSupplier.get();
                    this.updateButtonsVisibility();
                    this.updateChildren();
                    return true;
                }
                return false;
            }

            /**
             * Pass to the previous state if possible
             *
             * @param previousStateSupplier The supplier for the previous state from the current
             *     state
             * @return True if passed to the previous state successfully
             */
            protected boolean previousState(Supplier<T> previousStateSupplier) {
                if (this.currentState.hasPrevious()) {
                    this.currentState = previousStateSupplier.get();
                    this.updateChildren();
                    return true;
                }
                return false;
            }

            /**
             * Verify if map creation is completed or not
             *
             * @return True if the map creation completed, false if not
             */
            protected boolean isCompleted() {
                return !this.currentState.hasNext();
            }

            protected abstract void updateButtonsVisibility();

            protected abstract boolean previousState();

            protected abstract boolean nextState();

            protected abstract void updateChildren();
        }
    }

    protected interface AbstractState {
        String tooltipText();

        AbstractState next();

        AbstractState previous();

        boolean hasNext();

        boolean hasPrevious();

        int index();

        static AbstractState fromIndex(int index) {
            throw new UnsupportedOperationException("Please implement fromIndex in your class");
        }
    }
}
