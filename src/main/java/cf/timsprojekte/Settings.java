package cf.timsprojekte;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Settings {
    private Properties properties;
    private Properties defaultProperties;

    public Settings() {
        properties = new Properties(defaultProperties);
        properties.setProperty("botname", "");
        properties.setProperty("bottoken", "");
        try {
            InputStream input = new FileInputStream("config.properties");
            properties.load(input);
        } catch (IOException e) {
            try {
                OutputStream outputStream = new FileOutputStream("config.properties");
                properties.store(outputStream, "RPGBot");
            } catch (IOException e1) {
                e.printStackTrace();
                System.err.println("SCHREIBFEHLER");
                System.exit(-1);
            }
        }
    }

    public String getBotname() {
        return properties.getProperty("botname");
    }

    public String getBottoken() {
        return properties.getProperty("bottoken");
    }

    public Properties getProperties() {
        return properties;
    }
}
