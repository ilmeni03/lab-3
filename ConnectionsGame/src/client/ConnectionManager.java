package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Gestisce la connessione TCP con il server.
 * Invia richieste JSON e riceve risposte JSON.
 */
public class ConnectionManager {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson;
    private boolean connected;
    
    public ConnectionManager() {
        this.gson = new Gson();
        this.connected = false;
    }
    
    /**
     * Connette al server.
     */
    public boolean connect(String host, int port) {
        try {
            System.out.println("Connessione a " + host + ":" + port + "...");
            
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);  // auto-flush
            
            connected = true;
            System.out.println("✓ Connesso al server!\n");
            return true;
            
        } catch (IOException e) {
            System.err.println("✗ Errore di connessione: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Invia una richiesta JSON al server e restituisce la risposta.
     */
    public JsonObject sendRequest(JsonObject request) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Non connesso al server");
        }
        
        String requestStr = gson.toJson(request);
        
        // Invia la richiesta
        out.println(requestStr);
        
        // Leggi la risposta
        String responseStr = in.readLine();
        
        if (responseStr == null) {
            throw new IOException("Server ha chiuso la connessione");
        }
        
        // Parse della risposta
        return gson.fromJson(responseStr, JsonObject.class);
    }
    
    /**
     * Chiude la connessione.
     */
    public void disconnect() {
        connected = false;
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("✓ Disconnesso dal server");
        } catch (IOException e) {
            System.err.println("Errore durante disconnessione: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se è connesso.
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
