package client;

import client.manager.ConfigManager;

/**
 * Main del client per il gioco Connections.
 */
public class ClientMain {
    
    public static void main(String[] args) {
        ConnectionManager connection = null;
        
        try {
            // 1. Carica configurazione
            ConfigManager.initialize("config/client.properties");
            ConfigManager config = ConfigManager.getInstance();
            
            String serverHost = config.getServerHost();
            int serverPort = config.getTcpPort();
            
            // 2. Connetti al server
            connection = new ConnectionManager();
            boolean connected = connection.connect(serverHost, serverPort);
            
            if (!connected) {
                System.err.println("✗ Impossibile connettersi al server");
                System.exit(1);
            }
            
            // 3. Avvia l'interfaccia CLI
            CLI cli = new CLI(connection);
            cli.start();
            
        } catch (Exception e) {
            System.err.println("✗ Errore: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            // Cleanup
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
