package server;

import server.handler.CommandHandler;
import server.manager.ConfigManager;
import server.manager.GameManager;
import server.manager.UserManager;
import server.nio.ClientAttachment;
import server.util.JsonGameLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server NIO con Selector per il gioco Connections.
 * Usa multiplexing per gestire molti client con pochi thread.
 */
public class NIOServerMain {
    private static volatile boolean running = true;
    private static Selector selector;
    private static ExecutorService workerPool;
    private static UserManager userManager;
    private static GameManager gameManager;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    CONNECTIONS SERVER - NIO Version    ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        ServerSocketChannel serverChannel = null;
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
            System.out.println("   Thread Pool: " + threadPoolSize + " worker threads");
            System.out.println("   Durata partita: " + gameDurationMinutes + " minuti\n");
            
            // 2. Inizializza i manager
            System.out.println("2. Inizializzazione manager...");
            userManager = new UserManager();
            
            gameLoader = new JsonGameLoader(gamesFile);
            gameLoader.initialize();
            
            gameManager = new GameManager(gameLoader, userManager, gameDurationMs);
            System.out.println();
            
            // 3. Avvia la prima partita
            System.out.println("3. Avvio prima partita...");
            gameManager.startNewGame();
            System.out.println();
            
            // 4. Crea il Thread Pool per i worker
            System.out.println("4. Creazione Worker Thread Pool...");
            workerPool = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("   ✓ Worker Pool creato\n");
            
            // 5. Inizializza NIO
            System.out.println("5. Inizializzazione NIO...");
            selector = Selector.open();
            
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(tcpPort));
            serverChannel.configureBlocking(false);  // NON-BLOCCANTE!
            
            // Registra il server channel per OP_ACCEPT
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("   ✓ Selector creato");
            System.out.println("   ✓ Server channel registrato\n");
            
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║  SERVER NIO PRONTO - Selector attivo   ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            // Shutdown hook
            final ServerSocketChannel finalServerChannel = serverChannel;
            final JsonGameLoader finalGameLoader = gameLoader;
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\n⚠ Ricevuto segnale di terminazione...");
                running = false;
                
                try {
                    if (selector != null) selector.wakeup();
                    if (finalServerChannel != null) finalServerChannel.close();
                } catch (IOException e) {
                    // Ignora
                }
                
                if (workerPool != null) workerPool.shutdown();
                if (gameManager != null) gameManager.shutdown();
                if (finalGameLoader != null) {
                    try {
                        finalGameLoader.close();
                    } catch (IOException e) {
                        // Ignora
                    }
                }
                
                System.out.println("✓ Server NIO terminato correttamente");
            }));
            
            // 6. Loop principale del Selector
            selectorLoop();
            
        } catch (Exception e) {
            System.err.println("\n✗ ERRORE FATALE:");
            e.printStackTrace();
            
        } finally {
            cleanup(serverChannel, gameLoader);
        }
    }
    
    /**
     * Loop principale del Selector.
     * Gestisce eventi di I/O su tutti i channel.
     */
    private static void selectorLoop() throws IOException {
        System.out.println("▶ Selector loop avviato\n");
        
        while (running) {
            // Attende eventi (bloccante, ma monitora tutti i channel!)
            int readyChannels = selector.select(1000);  // Timeout 1 secondo
            
            if (readyChannels == 0) {
                continue;  // Nessun evento, riprova
            }
            
            // Elabora tutti gli eventi pronti
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();  // IMPORTANTE: rimuovi dalla lista
                
                if (!key.isValid()) {
                    continue;  // Chiave non più valida
                }
                
                try {
                    if (key.isAcceptable()) {
                        handleAccept(key);
                        
                    } else if (key.isReadable()) {
                        handleRead(key);
                        
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                    
                } catch (IOException e) {
                    System.err.println("✗ Errore gestione evento: " + e.getMessage());
                    closeConnection(key);
                }
            }
        }
        
        System.out.println("✓ Selector loop terminato");
    }
    
    /**
     * Gestisce evento OP_ACCEPT: nuova connessione.
     */
    private static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel == null) {
            return;  // Non dovrebbe succedere con NIO
        }
        
        // Configura il client channel come non-bloccante
        clientChannel.configureBlocking(false);
        
        // Crea l'attachment per questo client
        CommandHandler commandHandler = new CommandHandler(userManager, gameManager);
        ClientAttachment attachment = new ClientAttachment(clientChannel, commandHandler);
        
        // Registra per OP_READ
        clientChannel.register(selector, SelectionKey.OP_READ, attachment);
        
        System.out.println("✓ Nuova connessione da: " + attachment.getClientAddress());
    }
    
    /**
     * Gestisce evento OP_READ: dati disponibili per lettura.
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientAttachment attachment = (ClientAttachment) key.attachment();
        
        ByteBuffer buffer = attachment.getReadBuffer();
        buffer.clear();
        
        int bytesRead = channel.read(buffer);
        
        if (bytesRead == -1) {
            // Client ha chiuso la connessione
            System.out.println("  Client ha chiuso la connessione: " + 
                             attachment.getClientAddress());
            closeConnection(key);
            return;
        }
        
        if (bytesRead == 0) {
            return;  // Nessun dato disponibile
        }
        
        // Converte i byte letti in stringa
        buffer.flip();
        String data = StandardCharsets.UTF_8.decode(buffer).toString();
        
        // Estrae messaggi completi (terminati da \n)
        Queue<String> messages = attachment.extractMessages(data);
        
        // Processa ogni messaggio in un worker thread
        for (String message : messages) {
            System.out.println("← [" + attachment.getClientAddress() + "] " + message);
            
            // Delega l'elaborazione al worker pool
            workerPool.execute(() -> {
                String response = attachment.getCommandHandler().handleCommand(message);
                
                System.out.println("→ [" + attachment.getClientAddress() + "] " + response);
                
                // Accoda la risposta per l'invio
                attachment.queueMessage(response + "\n");
                
                // Registra interesse per OP_WRITE
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();  // Sveglia il selector
            });
        }
    }
    
    /**
     * Gestisce evento OP_WRITE: pronto per scrivere.
     */
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientAttachment attachment = (ClientAttachment) key.attachment();
        
        // Prendi il prossimo messaggio dalla coda
        String message = attachment.pollMessage();
        
        if (message == null) {
            // Nessun messaggio da inviare, rimuovi interesse per OP_WRITE
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            return;
        }
        
        // Scrivi il messaggio sul channel
        ByteBuffer buffer = attachment.getWriteBuffer();
        buffer.clear();
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        
        // Se ci sono altri messaggi, mantieni interesse per OP_WRITE
        if (!attachment.hasMessages()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }
    
    /**
     * Chiude una connessione client.
     */
    private static void closeConnection(SelectionKey key) {
        ClientAttachment attachment = (ClientAttachment) key.attachment();
        
        if (attachment != null) {
            attachment.close();
        }
        
        try {
            key.channel().close();
        } catch (IOException e) {
            // Ignora
        }
        
        key.cancel();
    }
    
    /**
     * Cleanup finale delle risorse.
     */
    private static void cleanup(ServerSocketChannel serverChannel, JsonGameLoader gameLoader) {
        System.out.println("\nChiusura risorse...");
        
        if (selector != null && selector.isOpen()) {
            try {
                // Chiudi tutte le connessioni client
                for (SelectionKey key : selector.keys()) {
                    try {
                        key.channel().close();
                    } catch (IOException e) {
                        // Ignora
                    }
                }
                selector.close();
            } catch (IOException e) {
                // Ignora
            }
        }
        
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                // Ignora
            }
        }
        
        if (workerPool != null) {
            workerPool.shutdown();
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
        
        System.out.println("✓ Risorse rilasciate");
    }
}
