/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.io.FilenameUtils;

// Same as the JFileChooser but filters only JSON files
class JSONFileChooser extends JFileChooser {
    private final Component parent;
    private String approuveButtonText;
    private static final String FILE_EXTENSION = "json";

    /** Creates a new JSONFileChooser instance. */
    JSONFileChooser(String title, Component parent, String approuveButtonText) {
        this(parent);
        super.setDialogTitle(title);
        this.approuveButtonText = approuveButtonText;
    }

    JSONFileChooser(Component parent) {
        // Create the JFileChooser from $HOME
        super(FileSystemView.getFileSystemView().getHomeDirectory());
        // Restrict users to select directories
        super.setFileSelectionMode(JSONFileChooser.FILES_ONLY);
        super.addChoosableFileFilter(
                new FileNameExtensionFilter("JSON files", JSONFileChooser.FILE_EXTENSION));
        super.setAcceptAllFileFilterUsed(false);
        this.parent = parent;
    }

    /**
     * Updates the approuve button text with the given text
     *
     * @param text The new text for the approuve button
     */
    void setApprouveButtonText(String text) {
        this.approuveButtonText = text;
    }

    /**
     * Show the dialog and get the selected file if any
     *
     * @return The selected file if any
     */
    // TODO: Use Optional<File> instead
    File get() {
        final int returnValue = super.showDialog(this.parent, this.approuveButtonText);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            // If the JFileChooser approved call the onFileSelected method with the selected file
            File file = super.getSelectedFile();
            if (!FilenameUtils.getExtension(file.getName())
                    .toLowerCase()
                    .equals(JSONFileChooser.FILE_EXTENSION)) {
                // If the file extension is not json add it
                file =
                        new File(
                                String.format(
                                        "%s.%s", file.getPath(), JSONFileChooser.FILE_EXTENSION));
            }
            return file;
        }
        return null;
    }
}
