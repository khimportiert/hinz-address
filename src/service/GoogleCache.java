package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.AddressMatch;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GoogleCache {
    private Map<String, AddressMatch> cache = new HashMap<>();
    private final Path cacheFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public GoogleCache(String filePath) {
        this.cacheFile = Paths.get(filePath);
        loadCache();
    }

    private synchronized void loadCache() {
        if (Files.exists(cacheFile)) {
            try (BufferedReader reader = Files.newBufferedReader(cacheFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Jede Zeile ist ein JSON-Objekt mit key und value
                    Entry entry = mapper.readValue(line, Entry.class);
                    cache.put(entry.key.toLowerCase(Locale.ROOT), entry.value);
                }
                System.out.println("Cache geladen: " + cache.size() + " Einträge in " + cacheFile.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Fehler beim Laden des Cache: " + e);
                cache = null;
            }
        } else {
            try {
                Files.createFile(cacheFile);
                System.out.println("Neuer Cache wird erstellt unter " + cacheFile.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Fehler beim Erstellen des Cache: " + e);
                cache = null;
            }
        }
    }

    public synchronized void save(String inputAddress, AddressMatch match) {
        if (inputAddress == null || match == null || cache == null) return;

        String key = inputAddress.toLowerCase(Locale.ROOT);
        cache.put(key, match);

        try (BufferedWriter writer = Files.newBufferedWriter(cacheFile, StandardOpenOption.APPEND)) {
            Entry entry = new Entry(key, match);
            writer.write(mapper.writeValueAsString(entry));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern des Cache: " + e);
        }
    }

    public synchronized AddressMatch find(String inputAddress) {
        if (inputAddress == null ||cache == null) return null;
        return cache.get(inputAddress.toLowerCase(Locale.ROOT));
    }

    // Hilfsklasse für die Zeilenrepräsentation
    private static class Entry {
        public String key;
        public AddressMatch value;

        public Entry() {}  // Für Jackson

        public Entry(String key, AddressMatch value) {
            this.key = key;
            this.value = value;
        }
    }
}
