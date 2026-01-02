package server.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rappresenta un utente registrato nel sistema.
 * Contiene credenziali e statistiche personali.
 */
public class User {
    private String username;
    private String password;
    
    // Statistiche del giocatore
    private final AtomicInteger totalScore;           // Punteggio totale accumulato
    private final AtomicInteger puzzlesCompleted;     // Totale partite giocate
    private final AtomicInteger puzzlesWon;           // Partite vinte
    private final AtomicInteger puzzlesLost;          // Partite perse
    private final AtomicInteger currentStreak;        // Streak corrente di vittorie consecutive
    private final AtomicInteger maxStreak;            // Streak massimo mai raggiunto
    private final AtomicInteger perfectPuzzles;       // Partite vinte senza errori
    
    // Histogram degli errori: quante partite finite con 0,1,2,3,4 errori e partite non finite
    private final int[] mistakeHistogram;  // [0]=0 errori, [1]=1 errore, ..., [4]=4 errori, [5]=non finite
    
    // Stato corrente
    private transient boolean loggedIn;
    private transient int currentGameId;  // ID della partita a cui sta partecipando (-1 se nessuna)
    
    /**
     * Costruttore per nuovo utente
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        
        this.totalScore = new AtomicInteger(0);
        this.puzzlesCompleted = new AtomicInteger(0);
        this.puzzlesWon = new AtomicInteger(0);
        this.puzzlesLost = new AtomicInteger(0);
        this.currentStreak = new AtomicInteger(0);
        this.maxStreak = new AtomicInteger(0);
        this.perfectPuzzles = new AtomicInteger(0);
        
        this.mistakeHistogram = new int[6];  // Inizializzato a 0
        
        this.loggedIn = false;
        this.currentGameId = -1;
    }
    
    // Getters e Setters
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
    
    public int getCurrentGameId() {
        return currentGameId;
    }
    
    public void setCurrentGameId(int gameId) {
        this.currentGameId = gameId;
    }
    
    // Metodi per le statistiche
    
    public int getTotalScore() {
        return totalScore.get();
    }
    
    public void addScore(int points) {
        totalScore.addAndGet(points);
    }
    
    public int getPuzzlesCompleted() {
        return puzzlesCompleted.get();
    }
    
    public int getPuzzlesWon() {
        return puzzlesWon.get();
    }
    
    public int getPuzzlesLost() {
        return puzzlesLost.get();
    }
    
    public int getCurrentStreak() {
        return currentStreak.get();
    }
    
    public int getMaxStreak() {
        return maxStreak.get();
    }
    
    public int getPerfectPuzzles() {
        return perfectPuzzles.get();
    }
    
    public int[] getMistakeHistogram() {
        return mistakeHistogram.clone();  // Restituisce una copia per sicurezza
    }
    
    /**
     * Calcola la percentuale di vittorie
     */
    public double getWinRate() {
        int completed = puzzlesCompleted.get();
        if (completed == 0) return 0.0;
        return (puzzlesWon.get() * 100.0) / completed;
    }
    
    /**
     * Calcola la percentuale di sconfitte
     */
    public double getLossRate() {
        int completed = puzzlesCompleted.get();
        if (completed == 0) return 0.0;
        return (puzzlesLost.get() * 100.0) / completed;
    }
    
    /**
     * Aggiorna le statistiche al termine di una partita
     * @param won true se ha vinto
     * @param mistakes numero di errori fatti (0-4)
     * @param finished true se ha completato la partita (non scaduto il tempo)
     */
    public synchronized void updateStats(boolean won, int mistakes, boolean finished) {
        puzzlesCompleted.incrementAndGet();
        
        if (won) {
            puzzlesWon.incrementAndGet();
            currentStreak.incrementAndGet();
            
            // Aggiorna max streak se necessario
            if (currentStreak.get() > maxStreak.get()) {
                maxStreak.set(currentStreak.get());
            }
            
            // Partita perfetta (0 errori)
            if (mistakes == 0) {
                perfectPuzzles.incrementAndGet();
            }
            
            // Aggiorna histogram
            if (mistakes >= 0 && mistakes <= 4) {
                mistakeHistogram[mistakes]++;
            }
            
        } else if (!finished) {
            // Partita non finita in tempo
            currentStreak.set(0);
            mistakeHistogram[5]++;
            
        } else {
            // Partita persa (4 errori)
            puzzlesLost.incrementAndGet();
            currentStreak.set(0);
            mistakeHistogram[4]++;
        }
    }
    
    @Override
    public String toString() {
        return String.format("User[%s, score=%d, games=%d, wins=%d]", 
            username, totalScore.get(), puzzlesCompleted.get(), puzzlesWon.get());
    }
}
