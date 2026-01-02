package server.manager;

import server.model.Game;
import server.model.PlayerGameState;
import server.model.User;
import server.model.WordGroup;
import server.util.JsonGameLoader;

import java.util.*;
import java.util.concurrent.*;

/**
 * Gestisce il ciclo di vita delle partite.
 * Mantiene lo stato della partita corrente e storico delle partite passate.
 */
public class GameManager {
    private final JsonGameLoader gameLoader;
    private final UserManager userManager;
    private final long gameDuration;  // Durata in millisecondi
    
    // Partita corrente (volatile per visibilità tra thread)
    private volatile Game currentGame;
    
    // Stati dei giocatori nella partita corrente (thread-safe)
    private final ConcurrentHashMap<String, PlayerGameState> playerStates;
    
    // Storico partite (thread-safe)
    private final ConcurrentHashMap<Integer, Game> gameHistory;
    
    // Timer per gestire la scadenza della partita
    private ScheduledExecutorService gameTimer;
    private ScheduledFuture<?> currentGameTask;
    
    /**
     * Costruttore
     */
    public GameManager(JsonGameLoader gameLoader, UserManager userManager, long gameDuration) {
        this.gameLoader = gameLoader;
        this.userManager = userManager;
        this.gameDuration = gameDuration;
        
        this.playerStates = new ConcurrentHashMap<>();
        this.gameHistory = new ConcurrentHashMap<>();
        
        // Timer con un singolo thread per gestire la scadenza
        this.gameTimer = Executors.newSingleThreadScheduledExecutor();
        
        System.out.println("[OK] GameManager inizializzato (durata partita: " + 
                         (gameDuration/1000) + " secondi)");
    }
    
    /**
     * Avvia una nuova partita.
     * Carica dal JSON e avvia il timer.
     */
    public synchronized boolean startNewGame() {
        try {
            // Cancella il timer della partita precedente se esiste
            if (currentGameTask != null && !currentGameTask.isDone()) {
                currentGameTask.cancel(false);
            }
            
            // Carica la prossima partita dal JSON
            Game newGame = gameLoader.loadNextGame(gameDuration);
            
            if (newGame == null) {
                System.err.println("[ERR] Nessuna partita disponibile nel JSON");
                return false;
            }
            
            // Archivia la partita precedente se esiste
            if (currentGame != null) {
                gameHistory.put(currentGame.getGameId(), currentGame);
                System.out.println("  Partita #" + currentGame.getGameId() + 
                                 " archiviata nello storico");
            }
            
            // Imposta la nuova partita
            currentGame = newGame;
            playerStates.clear();  // Reset stati giocatori
            
            System.out.println("[OK] Nuova partita avviata: #" + currentGame.getGameId());
            System.out.println("  Scadenza tra: " + (gameDuration/1000) + " secondi");
            
            // Avvia il timer per la scadenza
            startGameTimer();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[ERR] Errore nell'avvio della partita: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Avvia il timer che gestisce la scadenza della partita.
     */
    private void startGameTimer() {
        currentGameTask = gameTimer.schedule(() -> {
            handleGameTimeout();
        }, gameDuration, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gestisce la scadenza del tempo della partita.
     */
    private void handleGameTimeout() {
        System.out.println("\n-- TEMPO SCADUTO per partita #" + currentGame.getGameId());
        
        // Marca tutti i giocatori non finiti come "tempo scaduto"
        for (PlayerGameState state : playerStates.values()) {
            if (!state.isFinished()) {
                state.markTimeExpired();
                
                // Aggiorna statistiche utente
                User user = userManager.getUser(state.getUsername());
                if (user != null) {
                    user.addScore(state.getCurrentScore());
                    user.updateStats(false, state.getWrongProposals(), false);
                }
            }
        }
        
        System.out.println("[OK] Stati finali calcolati per tutti i giocatori");
        
        // TODO: Inviare notifica UDP a tutti i giocatori loggati
        
        // Dopo un breve delay, avvia automaticamente la prossima partita
        gameTimer.schedule(() -> {
            System.out.println("\n -- Avvio automatico prossima partita...\n");
            startNewGame();
        }, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Fa entrare un giocatore nella partita corrente.
     * Crea il suo PlayerGameState se non esiste.
     */
    public synchronized PlayerGameState joinGame(String username) {
        if (currentGame == null) {
            return null;
        }
        
        // Se già ha uno stato per questa partita, restituiscilo
        PlayerGameState state = playerStates.get(username);
        if (state != null) {
            System.out.println("  " + username + " si è riconnesso alla partita #" + 
                             currentGame.getGameId());
            return state;
        }
        
        // Crea nuovo stato per questo giocatore
        state = new PlayerGameState(username, currentGame.getGameId());
        playerStates.put(username, state);
        
        // Aggiorna lo stato dell'utente
        User user = userManager.getUser(username);
        if (user != null) {
            user.setCurrentGameId(currentGame.getGameId());
        }
        
        System.out.println("[OK] " + username + " è entrato nella partita #" + 
                         currentGame.getGameId());
        
        return state;
    }
    
    /**
     * Valida una proposta di un giocatore.
     * @return risultato della validazione
     */
    public ProposalResult submitProposal(String username, List<String> proposedWords) {
        // Verifica che ci sia una partita in corso
        if (currentGame == null) {
            return new ProposalResult(false, "Nessuna partita in corso", null, 0);
        }
        
        // Verifica che il giocatore abbia uno stato
        PlayerGameState state = playerStates.get(username);
        if (state == null) {
            return new ProposalResult(false, "Non sei nella partita", null, 0);
        }
        
        // Verifica che possa ancora giocare
        if (!state.canPlay()) {
            return new ProposalResult(false, "Hai già terminato la partita", null, 0);
        }
        
        // Verifica che la partita sia ancora attiva
        if (!currentGame.isActive()) {
            return new ProposalResult(false, "Tempo scaduto", null, 0);
        }
        
        // Valida la proposta (4 parole, tutte diverse, tutte nel gioco)
        if (proposedWords == null || proposedWords.size() != 4) {
            return new ProposalResult(false, "Devi proporre esattamente 4 parole", null, 0);
        }
        
        // Verifica parole duplicate nella proposta
        Set<String> uniqueWords = new HashSet<>(proposedWords);
        if (uniqueWords.size() != 4) {
            return new ProposalResult(false, "Parole duplicate nella proposta", null, 0);
        }
        
        // Verifica che tutte le parole appartengano alla partita
        List<String> gameWords = currentGame.getAllWords();
        for (String word : proposedWords) {
            if (!gameWords.contains(word)) {
                return new ProposalResult(false, "Parola non valida: " + word, null, 0);
            }
        }
        
        // Verifica se la proposta corrisponde a un gruppo
        int groupIndex = currentGame.getGroupIndex(proposedWords);
        
        if (groupIndex != -1) {
            // PROPOSTA CORRETTA!
            
            // Verifica se ha già trovato questo gruppo
            if (state.hasFoundGroup(groupIndex)) {
                return new ProposalResult(false, "Gruppo già trovato in precedenza", null, 0);
            }
            
            // Registra la proposta corretta
            int newScore = state.registerCorrectProposal(groupIndex);
            WordGroup foundGroup = currentGame.getGroups().get(groupIndex);
            
            System.out.println("[OK[ " + username + " ha trovato: " + foundGroup.getTheme() + 
                             " (score: " + newScore + ")");
            
            // Se ha vinto, aggiorna le statistiche
            if (state.hasWon()) {
                User user = userManager.getUser(username);
                if (user != null) {
                    user.addScore(newScore);
                    user.updateStats(true, state.getWrongProposals(), true);
                }
                System.out.println("[WIN] " + username + " ha VINTO la partita!");
            }
            
            return new ProposalResult(true, "Gruppo corretto!", foundGroup, newScore);
            
        } else {
            // PROPOSTA SBAGLIATA
            
            int newScore = state.registerWrongProposal();
            
            System.out.println("[ERR] " + username + " ha sbagliato (errori: " + 
                             state.getWrongProposals() + "/4, score: " + newScore + ")");
            
            // Se ha perso, aggiorna le statistiche
            if (state.isFinished() && !state.hasWon()) {
                User user = userManager.getUser(username);
                if (user != null) {
                    user.addScore(newScore);
                    user.updateStats(false, 4, true);
                }
                System.out.println("[LOSE] " + username + " ha PERSO la partita (4 errori)");
            }
            
            return new ProposalResult(false, "Gruppo sbagliato", null, newScore);
        }
    }
    
    // Getters
    
    public Game getCurrentGame() {
        return currentGame;
    }
    
    public PlayerGameState getPlayerState(String username) {
        return playerStates.get(username);
    }
    
    public Game getGameById(int gameId) {
        if (currentGame != null && currentGame.getGameId() == gameId) {
            return currentGame;
        }
        return gameHistory.get(gameId);
    }
    
    public Map<String, PlayerGameState> getAllPlayerStates() {
        return new HashMap<>(playerStates);
    }
    
    /**
     * Statistiche della partita corrente.
     */
    public GameStats getCurrentGameStats() {
        if (currentGame == null) {
            return null;
        }
        
        int totalPlayers = playerStates.size();
        int finishedPlayers = 0;
        int winners = 0;
        
        for (PlayerGameState state : playerStates.values()) {
            if (state.isFinished()) {
                finishedPlayers++;
                if (state.hasWon()) {
                    winners++;
                }
            }
        }
        
        return new GameStats(
            currentGame.getGameId(),
            currentGame.isActive(),
            currentGame.getRemainingTime(),
            totalPlayers,
            finishedPlayers,
            winners
        );
    }
    
    /**
     * Chiude il GameManager e libera le risorse.
     */
    public void shutdown() {
        if (currentGameTask != null) {
            currentGameTask.cancel(false);
        }
        gameTimer.shutdown();
        System.out.println("[OK] GameManager chiuso");
    }
    
    // Classi helper per i risultati
    
    public static class ProposalResult {
        public final boolean correct;
        public final String message;
        public final WordGroup foundGroup;  // null se sbagliato
        public final int newScore;
        
        public ProposalResult(boolean correct, String message, WordGroup foundGroup, int newScore) {
            this.correct = correct;
            this.message = message;
            this.foundGroup = foundGroup;
            this.newScore = newScore;
        }
    }
    
    public static class GameStats {
        public final int gameId;
        public final boolean active;
        public final long remainingTime;
        public final int totalPlayers;
        public final int finishedPlayers;
        public final int winners;
        
        public GameStats(int gameId, boolean active, long remainingTime, 
                        int totalPlayers, int finishedPlayers, int winners) {
            this.gameId = gameId;
            this.active = active;
            this.remainingTime = remainingTime;
            this.totalPlayers = totalPlayers;
            this.finishedPlayers = finishedPlayers;
            this.winners = winners;
        }
    }
}
