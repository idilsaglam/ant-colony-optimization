/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.*;

class ConfigurationPanel extends JPanel {

    public ConfigurationPanel(Consumer<ButtonType> onClick) {
        GridBagConstraints gbc = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        gbc.gridy = 0;
        gbc.gridx = 0;
        JButton button;
        // Add buttons for each button type
        for (ButtonType t : ButtonType.values()) {
            button = new JButton();
            button.setText(t.getButtonText());
            button.addActionListener((ActionEvent ignore) -> onClick.accept(t));
            this.add(button, gbc);
            gbc.gridy++;
        }
    }

    public static enum ButtonType {
        CREATE,
        LOAD,
        SETTINGS;

        /**
         * Get the text displayed on the buttons
         *
         * @return text displayed on the button
         */
        public String getButtonText() {
            return switch (this) {
                case LOAD -> "Load a map";
                case CREATE -> "Create a map";
                case SETTINGS -> "Settings";
            };
        }
    }
}
