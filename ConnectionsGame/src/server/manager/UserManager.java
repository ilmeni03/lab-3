package server.manager;

import server.model.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce tutti gli utenti registrati nel sistema.
 * Thread-safe: usa ConcurrentHashMap per accessi concorrenti.
 */
public class UserManager {
    // Mappa username -> User (thread-safe)
    private final ConcurrentHashMap<String, User> users;

    // Set degli utenti attualmente loggati (thread-safe)
    private final Set<String> loggedInUsers;

    /**
     * Costruttore
     */
    public UserManager() {
        this.users = new ConcurrentHashMap<>();
        this.loggedInUsers = Collections.synchronizedSet(new HashSet<>());

        System.out.println("[OK] UserManager inizializzato");
    }

    /**
     * Registra un nuovo utente.
     * @return true se registrato con successo, false se username già esistente
     */
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        User newUser = new User(username, password);
        User existing = users.putIfAbsent(username, newUser);

        if (existing == null) {
            System.out.println("[OK] Nuovo utente registrato: " + username);
            return true;
        } else {
            System.out.println("[ERR] Username gia esistente: " + username);
            return false;
        }
    }

    /**
     * Verifica le credenziali e effettua il login.
     * @return true se login riuscito, false altrimenti
     */
    public boolean login(String username, String password) {
        User user = users.get(username);

        if (user == null) {
            System.out.println("[ERR] Login fallito: utente non esistente - " + username);
            return false;
        }

        if (!user.getPassword().equals(password)) {
            System.out.println("[ERR] Login fallito: password errata - " + username);
            return false;
        }

        user.setLoggedIn(true);
        loggedInUsers.add(username);

        System.out.println("[OK] Login effettuato: " + username);
        return true;
    }

    /**
     * Effettua il logout di un utente.
     */
    public void logout(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            loggedInUsers.remove(username);
            System.out.println("[OK] Logout effettuato: " + username);
        }
    }

    /**
     * Verifica se un utente è attualmente loggato.
     */
    public boolean isLoggedIn(String username) {
        return loggedInUsers.contains(username);
    }

    /**
     * Aggiorna le credenziali di un utente.
     */
    public synchronized boolean updateCredentials(String oldUsername, String newUsername,
                                                  String oldPassword, String newPassword) {
        User user = users.get(oldUsername);

        if (user == null) {
            System.out.println("[ERR] Update fallito: utente non esistente - " + oldUsername);
            return false;
        }

        if (!user.getPassword().equals(oldPassword)) {
            System.out.println("[ERR] Update fallito: password errata - " + oldUsername);
            return false;
        }

        // Cambia username
        if (newUsername != null && !newUsername.equals(oldUsername)) {
            if (users.containsKey(newUsername)) {
                System.out.println("[ERR] Update fallito: nuovo username gia esistente - " + newUsername);
                return false;
            }

            users.remove(oldUsername);
            user.setUsername(newUsername);
            users.put(newUsername, user);

            if (loggedInUsers.contains(oldUsername)) {
                loggedInUsers.remove(oldUsername);
                loggedInUsers.add(newUsername);
            }

            System.out.println("[OK] Username aggiornato: " + oldUsername + " -> " + newUsername);
        }

        // Cambia password
        if (newPassword != null && !newPassword.equals(oldPassword)) {
            user.setPassword(newPassword);
            System.out.println("[OK] Password aggiornata per: " + user.getUsername());
        }

        return true;
    }

    /**
     * Restituisce un utente dato il suo username.
     */
    public User getUser(String username) {
        return users.get(username);
    }

    /**
     * Verifica se un username esiste.
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Restituisce il numero di utenti registrati.
     */
    public int getTotalUsers() {
        return users.size();
    }

    /**
     * Restituisce il numero di utenti attualmente loggati.
     */
    public int getLoggedInCount() {
        return loggedInUsers.size();
    }

    /**
     * Restituisce la lista di tutti gli utenti loggati.
     */
    public List<String> getLoggedInUsers() {
        return new ArrayList<>(loggedInUsers);
    }

    /**
     * Genera la classifica globale ordinata per punteggio totale.
     */
    public List<User> getLeaderboard() {
        List<User> leaderboard = new ArrayList<>(users.values());
        leaderboard.sort((u1, u2) ->
                Integer.compare(u2.getTotalScore(), u1.getTotalScore())
        );
        return leaderboard;
    }

    /**
     * Restituisce i top K utenti della classifica.
     */
    public List<User> getTopKUsers(int k) {
        List<User> leaderboard = getLeaderboard();
        return leaderboard.subList(0, Math.min(k, leaderboard.size()));
    }

    /**
     * Trova la posizione in classifica di un utente.
     */
    public int getUserRank(String username) {
        List<User> leaderboard = getLeaderboard();

        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getUsername().equals(username)) {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * Restituisce tutti gli utenti (copia per sicurezza).
     */
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Carica gli utenti da una mappa (per persistenza).
     */
    public void loadUsers(Map<String, User> loadedUsers) {
        users.clear();
        users.putAll(loadedUsers);

        for (User user : users.values()) {
            user.setLoggedIn(false);
            user.setCurrentGameId(-1);
        }
        loggedInUsers.clear();

        System.out.println("[OK] Caricati " + users.size() + " utenti dalla persistenza");
    }

    /**
     * Stampa statistiche generali degli utenti (per debug).
     */
    public void printStats() {
        System.out.println("\n=== STATISTICHE UTENTI ===");
        System.out.println("Utenti registrati: " + getTotalUsers());
        System.out.println("Utenti loggati: " + getLoggedInCount());
        System.out.println("==========================\n");
    }
}
