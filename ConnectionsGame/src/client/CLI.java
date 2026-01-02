package client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Scanner;

/**
 * Interfaccia a linea di comando per il client.
 */
public class CLI {
    private final ConnectionManager connection;
    private final Scanner scanner;
    private boolean loggedIn;
    private String currentUsername;

    public CLI(ConnectionManager connection) {
        this.connection = connection;
        this.scanner = new Scanner(System.in);
        this.loggedIn = false;
        this.currentUsername = null;
    }

    /**
     * Avvia l'interfaccia CLI.
     */
    public void start() {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║     CONNECTIONS - Client Interattivo      ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        boolean running = true;

        while (running && connection.isConnected()) {
            try {
                if (!loggedIn) {
                    showPreLoginMenu();
                    int choice = readInt("Scelta: ");
                    running = handlePreLoginChoice(choice);
                } else {
                    showMainMenu();
                    int choice = readInt("Scelta: ");
                    running = handleMainMenuChoice(choice);
                }

            } catch (Exception e) {
                System.err.println("✗ Errore: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\nArrivederci!");
    }

    /**
     * Menu pre-login.
     */
    private void showPreLoginMenu() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║               MENU PRINCIPALE              ║");
        System.out.println("╠═══════════════════════════════════════════╣");
        System.out.println("║  1. Registrazione                          ║");
        System.out.println("║  2. Login                                  ║");
        System.out.println("║  3. Aggiorna credenziali                   ║");
        System.out.println("║  0. Esci                                   ║");
        System.out.println("╚═══════════════════════════════════════════╝");
    }

    /**
     * Menu post-login.
     */
    private void showMainMenu() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║         MENU GIOCO [" + currentUsername + "]" +
                spaces(Math.max(0, 24 - currentUsername.length())) + "║");
        System.out.println("╠═══════════════════════════════════════════╣");
        System.out.println("║  1. Invia proposta                         ║");
        System.out.println("║  2. Statistiche partita corrente           ║");
        System.out.println("║  3. Visualizza classifica                  ║");
        System.out.println("║  4. Le mie statistiche                     ║");
        System.out.println("║  5. Logout                                 ║");
        System.out.println("║  0. Esci                                   ║");
        System.out.println("╚═══════════════════════════════════════════╝");
    }

    /**
     * Gestisce scelte pre-login.
     */
    private boolean handlePreLoginChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                register();
                break;
            case 2:
                login();
                break;
            case 3:
                updateCredentials();
                break;
            case 0:
                return false;
            default:
                System.out.println("Scelta non valida");
        }
        return true;
    }

    /**
     * Gestisce scelte post-login.
     */
    private boolean handleMainMenuChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                submitProposal();
                break;
            case 2:
                requestGameStats();
                break;
            case 3:
                requestLeaderboard();
                break;
            case 4:
                requestPlayerStats();
                break;
            case 5:
                logout();
                break;
            case 0:
                logout();
                return false;
            default:
                System.out.println("Scelta non valida");
        }
        return true;
    }

    // ==================== OPERAZIONI ====================

    /**
     * REGISTRAZIONE
     */
    private void register() throws IOException {
        System.out.println("\n=== REGISTRAZIONE ===");
        String username = readString("Username: ");
        String password = readString("Password: ");

        JsonObject request = new JsonObject();
        request.addProperty("operation", "register");
        request.addProperty("name", username);
        request.addProperty("psw", password);

        JsonObject response = connection.sendRequest(request);
        printResponse(response);
    }

    /**
     * LOGIN
     */
    private void login() throws IOException {
        System.out.println("\n=== LOGIN ===");
        String username = readString("Username: ");
        String password = readString("Password: ");

        JsonObject request = new JsonObject();
        request.addProperty("operation", "login");
        request.addProperty("username", username);
        request.addProperty("psw", password);

        JsonObject response = connection.sendRequest(request);

        if (response.get("success").getAsBoolean()) {
            loggedIn = true;
            currentUsername = username;

            System.out.println("\n✓ " + response.get("message").getAsString());

            // Mostra info partita se presenti
            if (response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                System.out.println("\n╔═══════════════════════════════════════════╗");
                System.out.println("║           PARTITA IN CORSO                 ║");
                System.out.println("╠═══════════════════════════════════════════╣");
                System.out.println("  Game ID: " + data.get("gameId").getAsInt());
                System.out.println("  Tempo rimanente: " +
                        (data.get("remainingTime").getAsLong() / 1000) + " secondi");
                System.out.println("  Punteggio attuale: " + data.get("currentScore").getAsInt());
                System.out.println("  Proposte corrette: " + data.get("correctProposals").getAsInt());
                System.out.println("  Errori: " + data.get("wrongProposals").getAsInt() + "/4");

                System.out.println("\n  Parole disponibili:");
                JsonArray words = data.getAsJsonArray("words");
                System.out.print("  ");
                for (int i = 0; i < words.size(); i++) {
                    System.out.print(words.get(i).getAsString());
                    if (i < words.size() - 1) System.out.print(", ");
                    if ((i + 1) % 4 == 0 && i < words.size() - 1) System.out.print("\n  ");
                }
                System.out.println("\n╚═══════════════════════════════════════════╝");
            }
        } else {
            System.out.println("\n✗ " + response.get("message").getAsString());
        }
    }

    /**
     * AGGIORNA CREDENZIALI
     */
    private void updateCredentials() throws IOException {
        System.out.println("\n=== AGGIORNA CREDENZIALI ===");
        String oldUsername = readString("Username attuale: ");
        String oldPassword = readString("Password attuale: ");

        System.out.println("\nLascia vuoto per non modificare");
        String newUsername = readString("Nuovo username (vuoto = non cambiare): ");
        String newPassword = readString("Nuova password (vuoto = non cambiare): ");

        JsonObject request = new JsonObject();
        request.addProperty("operation", "updateCredentials");
        request.addProperty("oldName", oldUsername);
        request.addProperty("oldPsw", oldPassword);

        if (!newUsername.isEmpty()) {
            request.addProperty("newName", newUsername);
        }
        if (!newPassword.isEmpty()) {
            request.addProperty("newPsw", newPassword);
        }

        JsonObject response = connection.sendRequest(request);
        printResponse(response);
    }

    /**
     * LOGOUT
     */
    private void logout() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("operation", "logout");

        JsonObject response = connection.sendRequest(request);
        printResponse(response);

        if (response.get("success").getAsBoolean()) {
            loggedIn = false;
            currentUsername = null;
        }
    }

    /**
     * INVIA PROPOSTA
     */
    private void submitProposal() throws IOException {
        System.out.println("\n=== INVIA PROPOSTA ===");
        System.out.println("Inserisci 4 parole separate da virgola:");
        String input = readString("Parole: ");

        String[] words = input.split(",");
        if (words.length != 4) {
            System.out.println("✗ Devi inserire esattamente 4 parole!");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("operation", "submitProposal");

        JsonArray wordsArray = new JsonArray();
        for (String word : words) {
            wordsArray.add(word.trim());
        }
        request.add("words", wordsArray);

        JsonObject response = connection.sendRequest(request);

        System.out.println("\n" + (response.get("success").getAsBoolean() ? "✓" : "✗") +
                " " + response.get("message").getAsString());

        if (response.has("data")) {
            JsonObject data = response.getAsJsonObject("data");
            System.out.println("  Nuovo punteggio: " + data.get("newScore").getAsInt());

            if (data.has("theme")) {
                System.out.println("  Tema trovato: " + data.get("theme").getAsString());
            }
        }
    }

    /**
     * STATISTICHE PARTITA
     */
    private void requestGameStats() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("operation", "requestGameStats");
        request.addProperty("gameId", -1);  // Partita corrente

        JsonObject response = connection.sendRequest(request);

        if (response.get("success").getAsBoolean() && response.has("data")) {
            JsonObject data = response.getAsJsonObject("data");

            System.out.println("\n╔═══════════════════════════════════════════╗");
            System.out.println("║        STATISTICHE PARTITA                 ║");
            System.out.println("╠═══════════════════════════════════════════╣");
            System.out.println("  Game ID: " + data.get("gameId").getAsInt());
            System.out.println("  Attiva: " + (data.get("active").getAsBoolean() ? "Sì" : "No"));
            System.out.println("  Tempo rimanente: " +
                    (data.get("remainingTime").getAsLong() / 1000) + " secondi");
            System.out.println("  Giocatori totali: " + data.get("totalPlayers").getAsInt());
            System.out.println("  Giocatori che hanno finito: " + data.get("finishedPlayers").getAsInt());
            System.out.println("  Vincitori: " + data.get("winners").getAsInt());
            System.out.println("╚═══════════════════════════════════════════╝");
        } else {
            System.out.println("\n✗ " + response.get("message").getAsString());
        }
    }

    /**
     * CLASSIFICA
     */
    private void requestLeaderboard() throws IOException {
        System.out.println("\n=== CLASSIFICA ===");
        System.out.println("1. Classifica completa");
        System.out.println("2. Top K giocatori");
        System.out.println("3. Posizione di un giocatore");

        int choice = readInt("Scelta: ");

        JsonObject request = new JsonObject();
        request.addProperty("operation", "requestLeaderboard");

        if (choice == 2) {
            int k = readInt("Quanti giocatori? ");
            request.addProperty("topPlayers", k);
        } else if (choice == 3) {
            String player = readString("Nome giocatore: ");
            request.addProperty("playerName", player);
        }

        JsonObject response = connection.sendRequest(request);

        if (response.get("success").getAsBoolean() && response.has("data")) {
            if (choice == 3) {
                // Posizione singola
                JsonObject data = response.getAsJsonObject("data");
                System.out.println("\n  Posizione: #" + data.get("rank").getAsInt());
                System.out.println("  Username: " + data.get("username").getAsString());
                System.out.println("  Punteggio: " + data.get("score").getAsInt());
            } else {
                // Lista giocatori
                JsonArray leaderboard = response.getAsJsonArray("data");

                System.out.println("\n╔═══════════════════════════════════════════╗");
                System.out.println("║             CLASSIFICA                     ║");
                System.out.println("╠═══════════════════════════════════════════╣");

                for (JsonElement elem : leaderboard) {
                    JsonObject player = elem.getAsJsonObject();
                    System.out.printf("  %2d. %-15s %6d punti%n",
                            player.get("rank").getAsInt(),
                            player.get("username").getAsString(),
                            player.get("score").getAsInt()
                    );
                }
                System.out.println("╚═══════════════════════════════════════════╝");
            }
        } else {
            System.out.println("\n✗ " + response.get("message").getAsString());
        }
    }

    /**
     * STATISTICHE PERSONALI
     */
    private void requestPlayerStats() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("operation", "requestPlayerStats");

        JsonObject response = connection.sendRequest(request);

        if (response.get("success").getAsBoolean() && response.has("data")) {
            JsonObject data = response.getAsJsonObject("data");

            System.out.println("\n╔═══════════════════════════════════════════╗");
            System.out.println("║         LE MIE STATISTICHE                 ║");
            System.out.println("╠═══════════════════════════════════════════╣");
            System.out.println("  Partite completate: " + data.get("puzzlesCompleted").getAsInt());
            System.out.printf("  Win Rate: %.1f%%%n", data.get("winRate").getAsDouble());
            System.out.printf("  Loss Rate: %.1f%%%n", data.get("lossRate").getAsDouble());
            System.out.println("  Current Streak: " + data.get("currentStreak").getAsInt());
            System.out.println("  Max Streak: " + data.get("maxStreak").getAsInt());
            System.out.println("  Perfect Puzzles: " + data.get("perfectPuzzles").getAsInt());

            System.out.println("\n  Histogram errori:");
            JsonArray histogram = data.getAsJsonArray("mistakeHistogram");
            for (int i = 0; i < histogram.size(); i++) {
                String label = (i < 5) ? (i + " errori") : "Non finite";
                System.out.printf("    %-12s: %d%n", label, histogram.get(i).getAsInt());
            }
            System.out.println("╚═══════════════════════════════════════════╝");
        } else {
            System.out.println("\n✗ " + response.get("message").getAsString());
        }
    }

    // ==================== UTILITY ====================

    private void printResponse(JsonObject response) {
        boolean success = response.get("success").getAsBoolean();
        String message = response.get("message").getAsString();

        System.out.println("\n" + (success ? "✓" : "✗") + " " + message);
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Inserisci un numero valido!");
            }
        }
    }

    /**
     * Utility compatibile con Java 8 per generare spazi.
     */
    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
