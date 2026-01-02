package server.nio;

import server.handler.CommandHandler;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mantiene lo stato di una connessione client nel server NIO.
 * Viene associato come "attachment" alla SelectionKey.
 */
public class ClientAttachment {
    private final SocketChannel channel;
    private final CommandHandler commandHandler;
    
    // Buffer per lettura
    private final ByteBuffer readBuffer;
    
    // Buffer per scrittura
    private final ByteBuffer writeBuffer;
    
    // Coda di messaggi da inviare
    private final Queue<String> messageQueue;
    
    // Accumula dati letti fino a trovare un messaggio completo
    private final StringBuilder partialMessage;
    
    // Indirizzo del client (per logging)
    private final String clientAddress;
    
    public ClientAttachment(SocketChannel channel, CommandHandler commandHandler) {
        this.channel = channel;
        this.commandHandler = commandHandler;
        
        // Buffer di lettura (8KB)
        this.readBuffer = ByteBuffer.allocate(8192);
        
        // Buffer di scrittura (8KB)
        this.writeBuffer = ByteBuffer.allocate(8192);
        
        // Coda messaggi
        this.messageQueue = new LinkedList<>();
        
        // Messaggio parziale
        this.partialMessage = new StringBuilder();
        
        // Indirizzo client
        try {
            this.clientAddress = channel.getRemoteAddress().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public SocketChannel getChannel() {
        return channel;
    }
    
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
    
    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }
    
    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }
    
    public String getClientAddress() {
        return clientAddress;
    }
    
    /**
     * Aggiunge dati letti al messaggio parziale.
     * Restituisce messaggi completi (terminati da newline).
     */
    public Queue<String> extractMessages(String data) {
        Queue<String> completeMessages = new LinkedList<>();
        partialMessage.append(data);
        
        // Cerca messaggi completi (terminati da \n)
        String accumulated = partialMessage.toString();
        String[] lines = accumulated.split("\n", -1);
        
        // Tutti tranne l'ultimo sono messaggi completi
        for (int i = 0; i < lines.length - 1; i++) {
            String msg = lines[i].trim();
            if (!msg.isEmpty()) {
                completeMessages.add(msg);
            }
        }
        
        // L'ultimo è il messaggio parziale rimanente
        partialMessage.setLength(0);
        partialMessage.append(lines[lines.length - 1]);
        
        return completeMessages;
    }
    
    /**
     * Accoda un messaggio da inviare al client.
     */
    public synchronized void queueMessage(String message) {
        messageQueue.offer(message);
    }
    
    /**
     * Preleva il prossimo messaggio da inviare.
     */
    public synchronized String pollMessage() {
        return messageQueue.poll();
    }
    
    /**
     * Verifica se ci sono messaggi in coda.
     */
    public synchronized boolean hasMessages() {
        return !messageQueue.isEmpty();
    }
    
    /**
     * Chiude la connessione.
     */
    public void close() {
        try {
            // Logout automatico se loggato
            String loggedUser = commandHandler.getLoggedUsername();
            if (loggedUser != null) {
                System.out.println("  Auto-logout: " + loggedUser);
            }
            
            channel.close();
            System.out.println("✗ Connessione chiusa: " + clientAddress);
            
        } catch (Exception e) {
            System.err.println("Errore durante chiusura: " + e.getMessage());
        }
    }
}
