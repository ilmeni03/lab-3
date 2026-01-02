package server.handler;

import server.manager.GameManager;
import server.manager.UserManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Gestisce la comunicazione con un singolo client.
 * Viene eseguito in un thread del pool.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final UserManager userManager;
    private final GameManager gameManager;
    private final CommandHandler commandHandler;

    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, UserManager userManager, GameManager gameManager) {
        this.clientSocket = socket;
        this.userManager = userManager;
        this.gameManager = gameManager;
        this.commandHandler = new CommandHandler(userManager, gameManager);
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress()
                + ":" + clientSocket.getPort();

        try {
            System.out.println("[OK] Nuova connessione da: " + clientAddress);

            // Setup stream I/O
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );
            out = new PrintWriter(clientSocket.getOutputStream(), true);  // auto-flush

            // Loop di comunicazione
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("[IN  " + clientAddress + "] " + request);

                // Processa il comando
                String response = commandHandler.handleCommand(request);

                // Invia la risposta
                out.println(response);
                System.out.println("[OUT " + clientAddress + "] " + response);
            }

        } catch (IOException e) {
            System.err.println("[ERR] Errore con client " + clientAddress + ": " + e.getMessage());

        } finally {
            cleanup(clientAddress);
        }
    }

    /**
     * Chiude le risorse e effettua logout se necessario
     */
    private void cleanup(String clientAddress) {
        // Logout automatico se era loggato
        String loggedUser = commandHandler.getLoggedUsername();
        if (loggedUser != null) {
            userManager.logout(loggedUser);
            System.out.println("Auto-logout: " + loggedUser);
        }

        // Chiudi stream
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Errore durante cleanup: " + e.getMessage());
        }

        System.out.println("[CLOSE] Connessione chiusa: " + clientAddress);
    }
}
