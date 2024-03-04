/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.Board;
import aco.core.Settings;
import aco.core.Settings.SettingsBuilder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

public class MainFrame extends JFrame {

    private Dimension size;
    private Point position;
    private Rectangle defaultSize;
    private Settings settings;
    private final ConfigurationPanel configurationPanel;
    private Board board;

    public MainFrame() {
        this.configurationPanel = new ConfigurationPanel(this::onButtonClicked);
        this.init();
        super.setVisible(true);
        this.setSettings((new Settings.SettingsBuilder()).build());
        this.board = (new Board.Builder()).build(this.settings);
    }

    private void setSettings(Settings settings) {
        this.settings = settings;
        // TODO: Handle exception and error
    }

    /** Initialize the main frame's properties */
    private void init() {
        super.setTitle("Algorithme de Colonies de Fourmis");
        super.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.initSizes();
        this.initContentPane();
    }

    /** Initialise the content pane with the configuration panel */
    private void initContentPane() {
        this.setContentPane(this.configurationPanel);
    }

    /** Initialize the position and the size of the current frame */
    private void initSizes() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // Initialise the dimension of the screen
        // FIXME(style): Change default size with a smaller one
        this.size = new Dimension(screenSize.width, screenSize.height);
        // The frame should not be smaller than this size
        super.setMinimumSize(this.size);
        // Set the position attribute of the frame
        // FIXME(style): Change the top left corner position when the size clarified
        this.position = new Point(0, 0);
        // the maximum size of the screen should not be bigger than the screen size
        super.setMaximumSize(screenSize);
        // Set the JFrame bound with the given position and the given size
        defaultSize =
                new Rectangle(this.position.x, this.position.y, this.size.width, this.size.height);
        super.setBounds(defaultSize);
        super.revalidate();
        super.repaint();
    }

    /**
     * Returns the usable screen size of the main frame
     *
     * @return Dimensions of the usable screen size
     */
    private Dimension getSafeAreaSize() {
        return new Dimension(
                this.getWidth() - this.getInsets().right - this.getInsets().left,
                this.getHeight() - this.getInsets().top - this.getInsets().bottom);
    }

    /**
     * Handle configuration pane button click action.
     *
     * @param t the type of the clicked button
     */
    private void onButtonClicked(ConfigurationPanel.ButtonType t) {

        this.setContentPane(
                switch (t) {
                    case SETTINGS -> new SettingsPanel(
                            (SettingsBuilder settingsBuilder) -> {
                                // Update the settings with the settings coming from the
                                // SettingsPanel
                                this.setSettings(settingsBuilder.build());
                                // If settings panel is dismissed (canceled) do not update settings
                                // TODO: Verify builder manually and show an error dialog if
                                // necessary
                                this.initContentPane();
                            },
                            this::initContentPane,
                            this.settings.builder());
                    case CREATE -> new CreateMapPanel(
                            this.getSafeAreaSize(),
                            this::startVisualisation,
                            this.settings,
                            this.board.builder());
                    case LOAD -> {
                        final JSONFileChooser jsonFileChooser =
                                new JSONFileChooser("Load map", this, "Load");
                        final File selectedFile = jsonFileChooser.get();
                        if (selectedFile == null) {
                            // If nothing is selected
                            yield this.getContentPane();
                        }
                        Board.Builder boardBuilder = null;
                        try {
                            boardBuilder = Board.Builder.from(selectedFile);
                            final CreateMapPanel createMapPanel =
                                    new CreateMapPanel(
                                            this.getSafeAreaSize(),
                                            this::startVisualisation,
                                            this.settings,
                                            boardBuilder);
                            createMapPanel.accept(new MapPanelCurrentStateVisitor());
                            yield createMapPanel;
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException();
                        }
                    }
                });
        this.revalidate();
        this.repaint();
    }

    /**
     * Start the visualisation of the algorithm with the given board
     *
     * @param boardBuilder The board builder used for te visualisation of the algorithm
     */
    private void startVisualisation(Board.Builder boardBuilder) {
        // TODO: Handle exception and display error message if necessary
        this.board = boardBuilder.build(this.settings);
        // TODO: Resize the main frame if necessary
        System.out.println("Create map panel submitted");
        this.setContentPane(new PreviewPanel(this.getSafeAreaSize(), this.board, this.settings));
        this.revalidate();
        this.repaint();
    }

    public static void main(String[] args) {
        MainFrame mainFrame = new MainFrame();
    }
}
