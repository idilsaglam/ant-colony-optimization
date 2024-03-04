/*
22015094 - SAGLAM Idil
*/
package aco.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MessageResolver {

    private static final Properties properties;

    static {
        try {
            properties = MessageResolver.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the properties from properties file
     *
     * @return The initialized property file
     * @throws IOException If the properties file is not found
     */
    private static Properties initialize() throws IOException {
        final Properties properties = new Properties();
        final String fileName = "messages_en.properties";
        final InputStream inputStream =
                MessageResolver.class.getResourceAsStream("/%s".formatted(fileName));
        if (inputStream != null) {
            properties.load(inputStream);
        } else {
            throw new FileNotFoundException("property file %s is not found".formatted(fileName));
        }
        return properties;
    }

    /**
     * Return the given message for the given key
     *
     * @param key The key of the message to get
     * @return The message related to the given key
     */
    public static String getMessage(String key) {
        return MessageResolver.properties.getProperty(key);
    }

    // TODO: Add constructor for language code
    // TODO: Read properties file for the given language
    // TODO: Load file content to a properties instance
    // TODO: Add methods to return text with selector

}
