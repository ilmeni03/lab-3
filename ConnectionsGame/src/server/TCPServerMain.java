package server;

import server.handler.ClientHandler;
import server.manager.ConfigManager;
import server.manager.GameManager;
import server.manager.UserManager;
import server.util.JsonGameLoader;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server TCP principale per il gioco Connections.
 * Versione con Thread Pool (non NIO).
 */
public class TCPServerMain {
    private static volatile boolean running = true;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    CONNECTIONS SERVER - TCP Version    ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        ServerSocket serverSocket = null;
        ExecutorService threadPool = null;
        GameManager gameManager = null;
        JsonGameLoader gameLoader = null;
        
        try {
            // 1. Carica configurazione
            System.out.println("1. Caricamento configurazione...");
            ConfigManager.initialize("config/server.properties");
            ConfigManager config = ConfigManager.getInstance();
            
            int tcpPort = config.getTcpPort();
            int threadPoolSize = config.getThreadPoolSize();
            String gamesFile = config.getProperty("game.file.path");
            int gameDurationMinutes = config.getIntProperty("game.duration.minutes");
            long gameDurationMs = gameDurationMinutes * 60 * 1000L;
            
            System.out.println("   Porta TCP: " + tcpPort);
            System.out.println("   Thread Pool: " + threadPoolSize + " threads");
            System.out.println("   Durata partita: " + gameDurationMinutes + " minuti\n");
            
            // 2. Inizializza i manager
            System.out.println("2. Inizializzazione manager...");
            UserManager userManager = new UserManager();
            
            gameLoader = new JsonGameLoader(gamesFile);
            gameLoader.initialize();
            
            gameManager = new GameManager(gameLoader, userManager, gameDurationMs);
            System.out.println();
            
            // 3. Avvia la prima partita
            System.out.println("3. Avvio prima partita...");
            gameManager.startNewGame();
            System.out.println();
            
            // 4. Crea il Thread Pool
            System.out.println("4. Creazione Thread Pool...");
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("   [OK] Thread Pool creato\n");
            
            // 5. Avvia il server TCP
            System.out.println("5. Avvio server TCP sulla porta " + tcpPort + "...");
            serverSocket = new ServerSocket(tcpPort);
            System.out.println("   [OK] Server TCP in ascolto\n");
            
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║  SERVER PRONTO - In attesa di client   ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            // 6. Loop principale: accetta connessioni
            final ServerSocket finalServerSocket = serverSocket;
            final ExecutorService finalThreadPool = threadPool;
            final UserManager finalUserManager = userManager;
            final GameManager finalGameManager = gameManager;
            
            // Shutdown hook per chiusura pulita
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\n [WARNING] Ricevuto segnale di terminazione...");
                running = false;
                
                try {
                    if (finalServerSocket != null && !finalServerSocket.isClosed()) {
                        finalServerSocket.close();
                    }
                } catch (IOException e) {
                    // Ignora
                }
                
                if (finalThreadPool != null) {
                    finalThreadPool.shutdown();
                }
                
                if (finalGameManager != null) {
                    finalGameManager.shutdown();
                }
                
                System.out.println("[OK] Server terminato correttamente");
            }));
            
            // Loop di accettazione connessioni
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Assegna la gestione del client al thread pool
                    ClientHandler handler = new ClientHandler(
                        clientSocket, 
                        finalUserManager, 
                        finalGameManager
                    );
                    
                    finalThreadPool.execute(handler);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[ERR] Errore nell'accettare connessione: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("\n[ERR] ERRORE FATALE:");
            e.printStackTrace();
            
        } finally {
            // Cleanup finale
            System.out.println("\nChiusura risorse...");
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignora
                }
            }
            
            if (threadPool != null) {
                threadPool.shutdown();
            }
            
            if (gameManager != null) {
                gameManager.shutdown();
            }
            
            if (gameLoader != null) {
                try {
                    gameLoader.close();
                } catch (IOException e) {
                    // Ignora
                }
            }
            
            System.out.println("[OK] Risorse rilasciate");
        }
    }
}
