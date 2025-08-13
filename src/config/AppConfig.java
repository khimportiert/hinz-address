package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Konfigurationsdatei konnte nicht geladen werden", e);
        }
    }
    
    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port"));
    }

    public static int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("socket.timeout"));
    }

    public static boolean getPhotonApiAllowed() {
        return Boolean.parseBoolean(properties.getProperty("photon.api.allowed"));
    }
    
    public static String getPhotonBaseUrl() {
        return properties.getProperty("photon.base.url");
    }

    public static String getPhotonLayer() {
        return properties.getProperty("photon.layer");
    }

    public static int getPhotonResultLimit() {
        return Integer.parseInt(properties.getProperty("photon.result.limit"));
    }

    public static boolean getGoogleApiAllowed() {
        return Boolean.parseBoolean(properties.getProperty("google.api.allowed"));
    }
    
    public static String getGoogleApiKey() {
        return properties.getProperty("google.api.key");
    }
    
    public static String getGoogleGeocodingUrl() {
        return properties.getProperty("google.geocoding.url");
    }

    public static boolean getGoogleUseCache() {
        return Boolean.parseBoolean(properties.getProperty("google.use.cache"));
    }

    public static String getGoogleCacheUrl() {
        return properties.getProperty("google.cache.url");
    }

    public static String getOrsBaseUrl() {
        return properties.getProperty("ors.base.url");
    }
}