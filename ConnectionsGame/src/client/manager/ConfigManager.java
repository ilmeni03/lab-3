package client.manager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestisce la lettura dei parametri di configurazione dal file properties.
 * Questa classe segue il pattern Singleton per garantire una singola istanza.
 */
public class ConfigManager {
    private Properties properties;
    private static ConfigManager instance;

    /**
     * Costruttore privato per il pattern Singleton.
     * @param configFilePath percorso del file di configurazione
     * @throws IOException se il file non può essere letto
     */
    private ConfigManager(String configFilePath) throws IOException {
        properties = new Properties();
        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            throw new IOException("File di configurazione non trovato: " + configFilePath);
        }

        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
            System.out.println("[OK] Configurazione caricata da: " + configFilePath);
        }
    }

    /**
     * Inizializza il ConfigManager con il file specificato.
     * Deve essere chiamato una sola volta all'avvio dell'applicazione.
     */
    public static void initialize(String configFilePath) throws IOException {
        if (instance == null) {
            instance = new ConfigManager(configFilePath);
        }
    }

    /**
     * Restituisce l'istanza singleton del ConfigManager.
     * @throws IllegalStateException se initialize() non è stata chiamata
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager non inizializzato. Chiamare initialize() prima.");
        }
        return instance;
    }

    // Metodi per recuperare i parametri di configurazione

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Proprieta non trovata: " + key);
        }
        return Integer.parseInt(value);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Valore non valido per " + key + ", uso default: " + defaultValue);
            return defaultValue;
        }
    }

    public long getLongProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Proprieta non trovata: " + key);
        }
        return Long.parseLong(value);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    // Metodi di utilità per ottenere configurazioni comuni

    public int getTcpPort() {
        return getIntProperty("tcp.port");
    }

    public int getUdpPort() {
        return getIntProperty("udp.port");
    }

    public String getServerHost() {
        return getProperty("server.host", "localhost");
    }

    public int getThreadPoolSize() {
        return getIntProperty("thread.pool.size", 20);
    }

    /**
     * Stampa tutte le proprietà caricate (utile per debug)
     */
    public void printAllProperties() {
        System.out.println("\n=== Configurazione caricata ===");
        properties.forEach((key, value) ->
                System.out.println(key + " = " + value)
        );
        System.out.println("================================\n");
    }
}
