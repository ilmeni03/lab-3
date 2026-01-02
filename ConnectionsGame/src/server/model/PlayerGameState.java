package server.model;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rappresenta lo stato di un giocatore in una specifica partita.
 * Thread-safe per accessi concorrenti.
 */
public class PlayerGameState {
    private final String username;
    private final int gameId;
    
    // Gruppi già individuati correttamente (indici 0-3)
    private final Set<Integer> foundGroups;
    
    // Contatori atomici
    private final AtomicInteger correctProposals;  // Proposte corrette (0-3)
    private final AtomicInteger wrongProposals;    // Proposte sbagliate (max 4)
    private final AtomicInteger currentScore;      // Punteggio corrente
    
    // Stato della partita per questo giocatore
    private volatile boolean finished;  // Ha terminato (vittoria/sconfitta)
    private volatile boolean won;       // Ha vinto
    
    /**
     * Costruttore
     */
    public PlayerGameState(String username, int gameId) {
        this.username = username;
        this.gameId = gameId;
        
        this.foundGroups = new HashSet<>();
        this.correctProposals = new AtomicInteger(0);
        this.wrongProposals = new AtomicInteger(0);
        this.currentScore = new AtomicInteger(0);
        
        this.finished = false;
        this.won = false;
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getGameId() {
        return gameId;
    }
    
    public int getCorrectProposals() {
        return correctProposals.get();
    }
    
    public int getWrongProposals() {
        return wrongProposals.get();
    }
    
    public int getCurrentScore() {
        return currentScore.get();
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public boolean hasWon() {
        return won;
    }
    
    /**
     * Verifica se ha già trovato un certo gruppo
     */
    public synchronized boolean hasFoundGroup(int groupIndex) {
        return foundGroups.contains(groupIndex);
    }
    
    /**
     * Registra una proposta corretta
     * @param groupIndex indice del gruppo trovato (0-3)
     * @return il nuovo punteggio
     */
    public synchronized int registerCorrectProposal(int groupIndex) {
        if (finished) {
            return currentScore.get();  // Non modificare se già finito
        }
        
        // Aggiungi il gruppo ai trovati
        if (!foundGroups.add(groupIndex)) {
            // Gruppo già trovato in precedenza - non dovrebbe succedere
            return currentScore.get();
        }
        
        int correct = correctProposals.incrementAndGet();
        
        // Calcola bonus in base al numero di proposte corrette
        int bonus = 0;
        if (correct == 1) {
            bonus = 6;
        } else if (correct == 2) {
            bonus = 12;
        } else if (correct == 3) {
            bonus = 18;
            // Ha vinto! (3 gruppi trovati, il 4° è implicito)
            finished = true;
            won = true;
        }
        
        currentScore.addAndGet(bonus);
        
        return currentScore.get();
    }
    
    /**
     * Registra una proposta sbagliata
     * @return il nuovo punteggio
     */
    public synchronized int registerWrongProposal() {
        if (finished) {
            return currentScore.get();  // Non modificare se già finito
        }
        
        int wrong = wrongProposals.incrementAndGet();
        
        // Penalità di -4 per ogni errore
        currentScore.addAndGet(-4);
        
        // Se ha fatto 4 errori, ha perso
        if (wrong >= 4) {
            finished = true;
            won = false;
        }
        
        return currentScore.get();
    }
    
    /**
     * Segna la partita come terminata per scadenza tempo
     */
    public synchronized void markTimeExpired() {
        if (!finished) {
            finished = true;
            won = false;
        }
    }
    
    /**
     * Calcola il punteggio finale secondo le regole:
     * - 0 se non ha inviato proposte
     * - +6/+12/+18 per 1/2/3 proposte corrette
     * - -4 per ogni proposta sbagliata
     */
    public int calculateFinalScore() {
        return currentScore.get();
    }
    
    /**
     * Verifica se può ancora giocare (non ha finito e non ha fatto 4 errori)
     */
    public boolean canPlay() {
        return !finished && wrongProposals.get() < 4;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerGameState[user=%s, game=%d, correct=%d, wrong=%d, score=%d, finished=%b, won=%b]",
            username, gameId, correctProposals.get(), wrongProposals.get(), 
            currentScore.get(), finished, won);
    }
}
