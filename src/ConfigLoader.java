import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties nicht gefunden!");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden der Konfiguration: " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Konfigurationswert nicht gefunden: " + key);
        }
        return value;
    }
}
