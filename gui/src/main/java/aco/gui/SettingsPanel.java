/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.MessageResolver;
import aco.core.Settings.SettingsBuilder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

class SettingsPanel extends JPanel {

    private static final Dimension COLOR_PREVIEW_DEFAULT_SIZE = new Dimension(20, 20);

    private final SettingsSelectorsContainer settingsSelectorsContainer;
    private final SettingsButtonsContainer buttonsContainer;

    private final SettingsPanelController controller;
    private final Consumer<SettingsBuilder> onApply;
    private final EmptyCallback onCancel;

    SettingsPanel(
            Consumer<SettingsBuilder> onApply,
            EmptyCallback onCancel,
            SettingsBuilder settingsBuilder) {
        this.controller = new SettingsPanelController(settingsBuilder);
        this.settingsSelectorsContainer = new SettingsSelectorsContainer();
        this.buttonsContainer = new SettingsButtonsContainer();

        this.onApply = onApply;
        this.onCancel = onCancel;

        GridBagConstraints gbc = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(this.settingsSelectorsContainer, gbc);
        gbc.gridy++;
        this.add(this.buttonsContainer, gbc);
    }

    private void rebuild() {
        this.settingsSelectorsContainer.rebuild();
        this.settingsSelectorsContainer.revalidate();
        this.settingsSelectorsContainer.repaint();
    }

    private class SettingsPanelController implements SettingsBuilder.Subscriber {

        public Map<
                        Event,
                        SettingsPanel.SettingsSelectorsContainer.SettingsContainer
                                .SettingsContainerRow>
                eventHandlers;
        private SettingsBuilder settingsBuilder;

        public SettingsPanelController(SettingsBuilder settingsBuilder) {
            this.settingsBuilder = settingsBuilder;
            this.settingsBuilder.subscribe(this);
            this.eventHandlers = new ConcurrentHashMap<>();
        }

        public void setStatusBarCircleNextColor(Color color) {
            this.settingsBuilder.setStatusBarCircleNextColor(color);
        }

        public void setStatusBarCircleCurrentColor(Color color) {
            this.settingsBuilder.setStatusBarCircleCurrentColor(color);
        }

        public void setStatusBarCircleCompletedColor(Color color) {
            this.settingsBuilder.setStatusBarCircleCompletedColor(color);
        }

        public void setSourcePointColor(Color color) {
            this.settingsBuilder.setSourcePointColor(color);
        }

        public void setDestinationPointColor(Color color) {
            this.settingsBuilder.setDestinationPointColor(color);
        }

        public void setObstacleColor(Color color) {
            this.settingsBuilder.setObstacleColor(color);
        }

        public void setBoardBorderColor(Color color) {
            this.settingsBuilder.setBoardBorderColor(color);
        }

        public void setMapBackgroundColor(Color color) {
            this.settingsBuilder.setMapBackgroundColor(color);
        }

        public void setMapFrameColor(Color color) {
            this.settingsBuilder.setMapFrameColor(color);
        }

        public void setBoardBuilderDraftBorderColor(Color color) {
            this.settingsBuilder.setBoardBuilderDraftBorderColor(color);
        }

        public void setMapBuilderDraftBackgroundColor(Color color) {
            this.settingsBuilder.setMapBuilderDraftBackgroundColor(color);
        }

        public void setMapBorderThickness(Integer thickness) {
            this.settingsBuilder.setMapBorderThickness(thickness);
        }

        public void setSourcePointRadius(Integer radius) {
            this.settingsBuilder.setSourcePointRadius(radius);
        }

        public void setDestinationPointRadius(Integer radius) {
            this.settingsBuilder.setDestinationPointRadius(radius);
        }

        public void setAntColor(Color color) {
            this.settingsBuilder.setAntColor(color);
        }

        public void setNumberOfAnts(int nbAnts) {
            this.settingsBuilder.setNumberOfAnts(nbAnts);
            ((SettingsPanel.SettingsSelectorsContainer.PreviewSettingsContainer)
                            SettingsPanel.this.settingsSelectorsContainer.previewSettings)
                    .antsPerSecondRow.setMaxValue(nbAnts / 60);
        }

        public void setNumberOfAntsPerSecond(int antsPerSecond) {
            System.out.printf("Number of ants per second %d\n", antsPerSecond);
            this.settingsBuilder.setAntsPerSecond(antsPerSecond);
        }

        public void setPheromoneColor(Color color) {
            System.out.println("Pheromone color changed");
            this.settingsBuilder.setPheromoneColor(color);
        }

        public void setPheromoneIntensityTimeoutMs(Integer timeoutMs) {
            this.settingsBuilder.setPheromoneEvaporationTimeout(timeoutMs);
        }

        public void setThreadpoolSize(Integer threadpoolSize) {
            this.settingsBuilder.setThreadpoolSize(threadpoolSize);
        }

        public void setNumberOfClusters(Integer nbClusters) {
            this.settingsBuilder.setNumberOfClusters(nbClusters);
        }

        public ActionListener getSettingsButtonActionListener(
                SettingsButtonsContainer.SettingsButtonTypes buttonType) {
            return switch (buttonType) {
                case SAVE -> this::saveSettings;
                case LOAD -> this::loadSettings;
                case CANCEL -> this::cancelSettings;
                case APPLY -> this::applySettings;
                case DEFAULT -> this::resetSettings;
            };
        }

        @Override
        public void on(Event event) {
            switch (event) {
                case SUBSCRIBED -> System.out.println("Subscribed");
                case UNSUBSCRIBED -> System.out.println("Unsubscribed");
                default -> {
                    System.out.printf("Received new event %s\n", event);
                    final SettingsPanel.SettingsSelectorsContainer.SettingsContainer
                                    .SettingsContainerRow
                            subject = this.eventHandlers.get(event);
                    if (subject == null) {
                        return;
                    }
                    subject.rebuild();
                }
            }
        }

        private void saveSettings(ActionEvent ignore) {
            final JSONFileChooser jsonFileChooser = new JSONFileChooser(SettingsPanel.this);
            jsonFileChooser.setDialogTitle(
                    MessageResolver.getMessage(
                            "gui.settings.save.settings.file.chooser.dialog.title"));
            jsonFileChooser.setApprouveButtonText(
                    MessageResolver.getMessage(
                            "gui.settings.save.settings.file.chooser.approve.button.text"));
            try {
                final File file = jsonFileChooser.get();
                if (file == null) {
                    // If the selected file is null (user dismissed the dialog) do nothing
                    return;
                }
                this.settingsBuilder.save(file);
                SettingsPanel.this.onApply.accept(this.settingsBuilder);
            } catch (IOException ioe) {
                System.err.println("Something related to the file or saving happened");
            } catch (IllegalArgumentException iae) {
                System.err.println("Settings contains illegal arguments");
            }
        }

        private void loadSettings(ActionEvent ignore) {
            final JSONFileChooser jsonFileChooser = new JSONFileChooser(SettingsPanel.this);
            jsonFileChooser.setDialogTitle(
                    MessageResolver.getMessage(
                            "gui.settings.load.settings.file.chooser.dialog.title"));
            jsonFileChooser.setApprouveButtonText(
                    MessageResolver.getMessage(
                            "gui.settings.load.settings.file.chooser.approve.button.text"));
            try {
                final File fileToLoad = jsonFileChooser.get();
                if (fileToLoad == null) {
                    // If nothing selected, do nothing
                    return;
                }
                this.settingsBuilder.load(fileToLoad);
                // Repaint the whole page to update
                SettingsPanel.this.rebuild();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Something related to IO operations happened");
            }
        }

        private void cancelSettings(ActionEvent ignore) {
            // Just call the onCancel callback
            SettingsPanel.this.onCancel.call();
        }

        private void applySettings(ActionEvent ignore) {
            try {
                SettingsPanel.this.onApply.accept(this.settingsBuilder);
            } catch (IllegalArgumentException e) {
                System.err.println("Something is not normal in the current settings builder");
            }
        }

        private void resetSettings(ActionEvent ignore) {
            // Create a new SettingsBuilder instance which will contain default values
            SettingsPanel.this.controller.settingsBuilder.reset();
            SettingsPanel.this.rebuild();
        }
    }

    private class SettingsButtonsContainer extends JPanel {

        private SettingsButtonsContainer() {

            final Map<SettingsButtonTypes, JButton> buttons = new HashMap<>();

            GridBagConstraints gbc = new GridBagConstraints();
            this.setLayout(new GridBagLayout());
            gbc.gridy = 0;
            gbc.gridx = 0;
            JButton button;
            for (SettingsButtonTypes sbt : SettingsButtonTypes.values()) {
                button = new JButton(sbt.toString());
                button.addActionListener(
                        SettingsPanel.this.controller.getSettingsButtonActionListener(sbt));
                buttons.put(sbt, button);
                this.add(button, gbc);
                gbc.gridx++;
            }
        }

        private enum SettingsButtonTypes {
            LOAD,
            SAVE,
            APPLY,
            CANCEL,
            DEFAULT;

            @Override
            public String toString() {
                return MessageResolver.getMessage(
                        "gui.settings.%s.button.title"
                                .formatted(
                                        switch (this) {
                                            case LOAD -> "load";
                                            case SAVE -> "save";
                                            case APPLY -> "apply";
                                            case CANCEL -> "cancel";
                                            case DEFAULT -> "reset";
                                        }));
            }
        }
    }

    private class SettingsSelectorsContainer extends JPanel {

        private final SettingsContainer commonSettings, builderSettings, previewSettings;

        private SettingsSelectorsContainer() {
            this.commonSettings = new CommonSettingsContainer();
            this.builderSettings = new BuilderSettingsContainer();
            this.previewSettings = new PreviewSettingsContainer();

            GridBagConstraints gbc = new GridBagConstraints();
            this.setLayout(new GridBagLayout());
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            this.add(this.commonSettings, gbc);
            gbc.gridx++;
            gbc.gridheight = 1;
            this.add(this.builderSettings, gbc);
            gbc.gridy++;
            this.add(this.previewSettings, gbc);
        }

        private void rebuild() {
            this.commonSettings.rebuild();
            this.commonSettings.revalidate();
            this.commonSettings.repaint();
            this.builderSettings.rebuild();
            this.builderSettings.revalidate();
            this.builderSettings.repaint();
            this.previewSettings.rebuild();
            this.previewSettings.revalidate();
            this.previewSettings.repaint();
        }

        private class CommonSettingsContainer extends SettingsContainer {
            private CommonSettingsContainer() {
                super(SettingsContainerType.COMMON_SETTINGS);
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.source.point.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.source.point.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getSourcePointColor,
                                MessageResolver.getMessage(
                                        "gui.settings.source.point.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setSourcePointColor,
                                SettingsBuilder.Subscriber.Event.SOURCE_POINT_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.destination.point.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.destination.point.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getDestinationPointColor,
                                MessageResolver.getMessage(
                                        "gui.settings.destination.point.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setDestinationPointColor,
                                SettingsBuilder.Subscriber.Event.DESTINATION_POINT_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.obstacle.color.title"),
                                MessageResolver.getMessage("gui.settings.obstacle.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getObstacleColor,
                                MessageResolver.getMessage(
                                        "gui.settings.obstacle.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setObstacleColor,
                                SettingsBuilder.Subscriber.Event.OBSTACLE_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.board.border.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.board.border.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getBoardBorderColor,
                                MessageResolver.getMessage(
                                        "gui.settings.board.border.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setBoardBorderColor,
                                SettingsBuilder.Subscriber.Event.BOARD_BORDER_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.map.background.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.map.background.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getMapBackgroundColor,
                                MessageResolver.getMessage(
                                        "gui.settings.map.background.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setMapBackgroundColor,
                                SettingsBuilder.Subscriber.Event.MAP_BACKGROUND_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.map.frame.color.title"),
                                MessageResolver.getMessage("gui.settings.map.frame.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getMapFrameColor,
                                MessageResolver.getMessage(
                                        "gui.settings.map.frame.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setMapFrameColor,
                                SettingsBuilder.Subscriber.Event.MAP_FRAME_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerSliderRow(
                                MessageResolver.getMessage(
                                        "gui.settings.map.border.thickness.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.map.border.thickness.tooltip"),
                                SettingsPanel.this.controller::setMapBorderThickness,
                                SettingsPanel.this.controller.settingsBuilder
                                        .mapBorderThicknessSliderConfig()));
                super.addRow(
                        new SettingsContainerSliderRow(
                                MessageResolver.getMessage(
                                        "gui.settings.source.point.radius.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.source.point.radius.tooltip"),
                                SettingsPanel.this.controller::setSourcePointRadius,
                                SettingsPanel.this.controller.settingsBuilder
                                        .sourcePointRadiusSliderConfig()));
                super.addRow(
                        new SettingsContainerSliderRow(
                                MessageResolver.getMessage(
                                        "gui.settings.destination.point.radius.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.destination.point.radius.tooltip"),
                                SettingsPanel.this.controller::setDestinationPointRadius,
                                SettingsPanel.this.controller.settingsBuilder
                                        .destinationPointRadiusSliderConfig()));
            }
        }

        private class BuilderSettingsContainer extends SettingsContainer {

            private BuilderSettingsContainer() {
                super(SettingsContainerType.BUILDER_SETTINGS);

                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.completed.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.completed.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getStatusBarCircleCompletedColor,
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.completed.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setStatusBarCircleCompletedColor,
                                SettingsBuilder.Subscriber.Event
                                        .STATUS_BAR_CIRCLE_COMPLETED_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.current.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.current.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getStatusBarCircleCurrentColor,
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.current.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setStatusBarCircleCurrentColor,
                                SettingsBuilder.Subscriber.Event
                                        .STATUS_BAR_CIRCLE_CURRENT_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.next.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.next.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getStatusBarCircleNextColor,
                                MessageResolver.getMessage(
                                        "gui.settings.status.bar.circle.next.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setStatusBarCircleNextColor,
                                SettingsBuilder.Subscriber.Event
                                        .STATUS_BAR_CIRCLE_NEXT_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.board.selector.border.color.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.board.selector.border.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getBoardBuilderDraftBorderColor,
                                MessageResolver.getMessage(
                                        "gui.settings.board.selector.border.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setBoardBuilderDraftBorderColor,
                                SettingsBuilder.Subscriber.Event.SELECTOR_BORDER_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.selector.background.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.selector.background.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        ::getMapBuilderDraftBackgroundColor,
                                MessageResolver.getMessage(
                                        "gui.settings.selector.background.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setMapBuilderDraftBackgroundColor,
                                SettingsBuilder.Subscriber.Event
                                        .SELECTOR_BACKGROUND_COLOR_CHANGED));
            }
        }

        private class PreviewSettingsContainer extends SettingsContainer {
            private final SettingsContainerNumberSpinnerRow antsPerSecondRow;

            private PreviewSettingsContainer() {
                super(SettingsContainerType.PREVIEW_SETTINGS);
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.ants.color.title"),
                                MessageResolver.getMessage("gui.settings.ants.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getAntColor,
                                MessageResolver.getMessage(
                                        "gui.settings.ants.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setAntColor,
                                SettingsBuilder.Subscriber.Event.ANT_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerNumberSpinnerRow(
                                MessageResolver.getMessage("gui.settings.number.of.ants.title"),
                                MessageResolver.getMessage("gui.settings.number.of.ants.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        .numberOfAntsSpinnerConfig(),
                                SettingsPanel.this.controller::setNumberOfAnts));
                this.antsPerSecondRow =
                        new SettingsContainerNumberSpinnerRow(
                                MessageResolver.getMessage("gui.settings.ants.per.second.title"),
                                MessageResolver.getMessage("gui.settings.ants.per.second.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        .antsPerSecondSpinnerConfig(),
                                SettingsPanel.this.controller::setNumberOfAntsPerSecond);
                super.addRow(this.antsPerSecondRow);
                super.addRow(
                        new SettingsContainerColorPickerRow(
                                MessageResolver.getMessage("gui.settings.pheromone.color.title"),
                                MessageResolver.getMessage("gui.settings.pheromone.color.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder::getPheromoneColor,
                                MessageResolver.getMessage(
                                        "gui.settings.pheromone.color.color.picker.dialog.text"),
                                SettingsPanel.this.controller::setPheromoneColor,
                                SettingsBuilder.Subscriber.Event.PHEROMONE_COLOR_CHANGED));
                super.addRow(
                        new SettingsContainerNumberSpinnerRow(
                                MessageResolver.getMessage(
                                        "gui.settings.pheromone.intensity.title"),
                                MessageResolver.getMessage(
                                        "gui.settings.pheromone.intensity.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        .pheromoneEvaporationTimeoutSpinnerConfig(),
                                SettingsPanel.this.controller::setPheromoneIntensityTimeoutMs));
                super.addRow(
                        new SettingsContainerNumberSpinnerRow(
                                MessageResolver.getMessage("gui.settings.threadpool.size.title"),
                                MessageResolver.getMessage("gui.settings.threadpool.size.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        .threadPoolSizeConfig(),
                                SettingsPanel.this.controller::setThreadpoolSize));
                super.addRow(
                        new SettingsContainerNumberSpinnerRow(
                                MessageResolver.getMessage("gui.settings.nb.clusters.title"),
                                MessageResolver.getMessage("gui.settings.nb.clusters.tooltip"),
                                SettingsPanel.this.controller.settingsBuilder
                                        .numberOfClustersConfig(),
                                SettingsPanel.this.controller::setNumberOfClusters));
            }
        }

        private class SettingsContainer extends JPanel {

            private final GridBagConstraints gbc;
            private final Set<SettingsContainerRow> rows;

            private SettingsContainer(SettingsContainerType containerType) {
                TitledBorder border =
                        BorderFactory.createTitledBorder(
                                BorderFactory.createLineBorder(Color.BLACK),
                                containerType.getTitle());
                border.setTitleJustification(TitledBorder.CENTER);
                this.setBorder(border);
                this.gbc = new GridBagConstraints();
                this.gbc.gridx = 0;
                this.gbc.gridy = 0;
                this.setLayout(new GridBagLayout());
                this.rows = new HashSet<>();
            }

            private void addRow(SettingsContainerRow row) {
                this.rows.add(row);
                this.add(row, this.gbc);
                SettingsPanel.this.controller.eventHandlers.put(row.type, row);
                this.gbc.gridy++;
            }

            private void rebuild() {
                this.rows.parallelStream().forEach(SettingsContainerRow::rebuild);
            }

            protected class SettingsContainerNumberSpinnerRow extends SettingsContainerRow {
                private final JSpinner spinner;
                private final SpinnerNumberModel numberModel;
                private final Supplier<Integer> defaultValueSupplier;

                private SettingsContainerNumberSpinnerRow(
                        String title,
                        String toolTipText,
                        SettingsBuilder.NumberSpinnerConfig config,
                        Consumer<Integer> onChanged) {
                    this(
                            title,
                            toolTipText,
                            config.supplier(),
                            config.min(),
                            config.max(),
                            config.step(),
                            onChanged,
                            config.event());
                }

                private SettingsContainerNumberSpinnerRow(
                        String title,
                        String tooltipText,
                        Supplier<Integer> currentValue,
                        int min,
                        int max,
                        int step,
                        Consumer<Integer> onChanged,
                        SettingsBuilder.Subscriber.Event event) {
                    super(title, tooltipText, event);
                    this.defaultValueSupplier = currentValue;
                    System.out.printf(
                            "Step size %d Max value %d Min value %d Default value: %d\n",
                            step, max, min, this.defaultValueSupplier.get());
                    this.numberModel =
                            new SpinnerNumberModel(
                                    this.defaultValueSupplier.get().intValue(), min, max, step);

                    this.spinner = new JSpinner(this.numberModel);
                    this.spinner.addChangeListener(
                            (ChangeEvent ignore) -> {
                                System.out.printf(
                                        "Number spinner changed. New value %d\n",
                                        this.numberModel.getNumber().intValue());
                                onChanged.accept(this.numberModel.getNumber().intValue());
                            });
                    super.addElement(this.spinner);
                }

                private void setMaxValue(Integer maxValue) {
                    this.numberModel.setMaximum(maxValue);
                    if ((Integer) this.numberModel.getMaximum()
                            < (Integer) this.numberModel.getValue()) {
                        this.numberModel.setValue(this.numberModel.getMaximum());
                    }
                    this.spinner.revalidate();
                    this.spinner.repaint();
                }

                private Integer getMaxValue() {
                    return (Integer) this.numberModel.getMaximum();
                }

                @Override
                protected void rebuild() {
                    this.numberModel.setValue(this.defaultValueSupplier.get());
                    this.spinner.revalidate();
                    this.spinner.repaint();
                }
            }

            protected class SettingsContainerColorPickerRow extends SettingsContainerRow {

                private final JPanel colorPreview;
                private final JButton openColorPickerButton;
                private final Supplier<Color> defaultColorSupplier;
                private final String colorPickerDialogText;
                private final Consumer<Color> onColorSelected;

                private SettingsContainerColorPickerRow(
                        String title,
                        String tooltipText,
                        Dimension colorPreviewSize,
                        Supplier<Color> defaultColor,
                        String buttonText,
                        String colorPickerDialogText,
                        Consumer<Color> onColorSelected,
                        SettingsBuilder.Subscriber.Event event) {
                    super(title, tooltipText, event);
                    this.defaultColorSupplier = defaultColor;
                    this.colorPreview = new JPanel();
                    this.colorPreview.setSize(colorPreviewSize);
                    this.colorPickerDialogText = colorPickerDialogText;
                    this.setPreviewColor();
                    this.onColorSelected = onColorSelected;
                    super.addElement(this.colorPreview);

                    this.openColorPickerButton = new JButton();
                    this.openColorPickerButton.setText(buttonText);
                    this.openColorPickerButton.addActionListener(this::onColorPickerConfirmed);
                    super.addElement(this.openColorPickerButton);
                }

                private SettingsContainerColorPickerRow(
                        String title,
                        String tooltipText,
                        Supplier<Color> defaultColor,
                        String colorPickerDialogText,
                        Consumer<Color> onColorSelected,
                        SettingsBuilder.Subscriber.Event event) {
                    this(
                            title,
                            tooltipText,
                            SettingsPanel.COLOR_PREVIEW_DEFAULT_SIZE,
                            defaultColor,
                            MessageResolver.getMessage("gui.settings.change.color.button.text"),
                            colorPickerDialogText,
                            onColorSelected,
                            event);
                }

                @Override
                protected void rebuild() {
                    this.setPreviewColor();
                    this.colorPreview.revalidate();
                    this.colorPreview.repaint();
                }

                /** Set the color in the color preview panel using the default color supplier */
                private void setPreviewColor() {
                    System.out.printf(
                            "Default color for %s from supplier %s\n",
                            this.colorPickerDialogText, this.defaultColorSupplier.get());
                    this.colorPreview.setBackground(this.defaultColorSupplier.get());
                }

                private void onColorPickerConfirmed(ActionEvent ignore) {
                    Color color =
                            JColorChooser.showDialog(
                                    this, colorPickerDialogText, this.defaultColorSupplier.get());
                    if (color == null) {
                        return;
                    }
                    onColorSelected.accept(color);
                }
            }

            protected class SettingsContainerSliderRow extends SettingsContainerRow {

                private final JSlider slider;
                private final Supplier<Integer> defaultValueSupplier;

                private SettingsContainerSliderRow(
                        String title,
                        String tooltipText,
                        Consumer<Integer> changeListener,
                        SettingsBuilder.SliderConfig config) {
                    this(
                            title,
                            tooltipText,
                            config.minValue(),
                            config.maxValue(),
                            config.supplier(),
                            changeListener,
                            config.majorTickSpacing(),
                            config.minorTickSpacing(),
                            config.paintTicks(),
                            config.paintLabels(),
                            config.event());
                }

                private SettingsContainerSliderRow(
                        String title,
                        String tooltipText,
                        int minValue,
                        int maxValue,
                        Supplier<Integer> defaultValue,
                        Consumer<Integer> changeListener,
                        int majorTickSpacing,
                        int minorTickSpacing,
                        boolean paintTicks,
                        boolean paintLabels,
                        SettingsBuilder.Subscriber.Event event) {
                    super(title, tooltipText, event);
                    this.defaultValueSupplier = defaultValue;
                    this.slider =
                            new JSlider(
                                    JSlider.HORIZONTAL,
                                    minValue,
                                    maxValue,
                                    this.defaultValueSupplier.get());
                    this.slider.addChangeListener(
                            (ChangeEvent ignore) -> {
                                changeListener.accept(this.slider.getValue());
                            });
                    this.slider.setMajorTickSpacing(majorTickSpacing);
                    this.slider.setMinorTickSpacing(minorTickSpacing);
                    this.slider.setPaintTicks(paintTicks);
                    this.slider.setPaintLabels(paintLabels);
                    super.addElement(this.slider);
                }

                @Override
                protected void rebuild() {
                    this.slider.setValue(this.defaultValueSupplier.get());
                    this.slider.revalidate();
                    this.slider.repaint();
                }
            }

            private abstract class SettingsContainerRow extends JPanel {

                private final GridBagConstraints gbc;
                private final JLabel label;
                private final SettingsBuilder.Subscriber.Event type;

                private SettingsContainerRow(
                        String title, String tooltipText, SettingsBuilder.Subscriber.Event type) {
                    this.gbc = new GridBagConstraints();
                    this.gbc.gridx = 0;
                    this.gbc.gridy = 0;
                    this.setLayout(new GridBagLayout());
                    this.label = new JLabel();
                    label.setText(title);
                    this.add(label, gbc);
                    this.setToolTipText(tooltipText);
                    this.type = type;
                }

                private void setTitle(String title) {
                    this.label.setText(title);
                }

                private void addElement(JComponent component) {
                    this.gbc.gridx++;
                    this.add(component, this.gbc);
                }

                protected abstract void rebuild();
            }
        }

        private enum SettingsContainerType {
            COMMON_SETTINGS,
            BUILDER_SETTINGS,
            PREVIEW_SETTINGS;

            /**
             * Return the title for the setting container type
             *
             * @return The string containing the title of the settings container type
             */
            private String getTitle() {
                return MessageResolver.getMessage(
                        "gui.settings.%s.settings.title"
                                .formatted(
                                        switch (this) {
                                            case COMMON_SETTINGS -> "common";
                                            case BUILDER_SETTINGS -> "builder";
                                            case PREVIEW_SETTINGS -> "preview";
                                        }));
            }
        }
    }
}
