package server.model;

import java.util.*;

/**
 * Rappresenta una partita di Connections.
 * Contiene i 4 gruppi tematici (16 parole totali).
 */
public class Game {
    private final int gameId;
    private final List<WordGroup> groups;  // Sempre 4 gruppi
    private final List<String> allWords;   // Tutte le 16 parole in ordine casuale
    private final long startTime;
    private final long duration;  // Durata in millisecondi
    
    /**
     * Costruttore
     * @param gameId identificativo univoco della partita
     * @param groups lista di 4 WordGroup
     * @param duration durata della partita in millisecondi
     */
    public Game(int gameId, List<WordGroup> groups, long duration) {
        if (groups == null || groups.size() != 4) {
            throw new IllegalArgumentException("Una partita deve avere esattamente 4 gruppi");
        }
        
        this.gameId = gameId;
        this.groups = new ArrayList<>(groups);
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        
        // Raccoglie tutte le 16 parole e le mischia
        this.allWords = new ArrayList<>();
        for (WordGroup group : groups) {
            allWords.addAll(Arrays.asList(group.getWords()));
        }
        Collections.shuffle(allWords);  // Ordine casuale
    }
    
    public int getGameId() {
        return gameId;
    }
    
    public List<WordGroup> getGroups() {
        return new ArrayList<>(groups);  // Copia per immutabilità
    }
    
    public List<String> getAllWords() {
        return new ArrayList<>(allWords);  // Copia per immutabilità
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getDuration() {
        return duration;
    }
    
    /**
     * Calcola il tempo rimanente in millisecondi
     * @return millisecondi rimanenti, o 0 se scaduto
     */
    public long getRemainingTime() {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = duration - elapsed;
        return Math.max(0, remaining);
    }
    
    /**
     * Verifica se la partita è ancora in corso
     */
    public boolean isActive() {
        return getRemainingTime() > 0;
    }
    
    /**
     * Verifica se le parole proposte formano un gruppo corretto
     * @param proposedWords le 4 parole proposte
     * @return il gruppo corrispondente, o null se non è corretto
     */
    public WordGroup checkProposal(List<String> proposedWords) {
        if (proposedWords == null || proposedWords.size() != 4) {
            return null;
        }
        
        // Verifica contro ogni gruppo
        for (WordGroup group : groups) {
            if (group.matches(proposedWords)) {
                return group;
            }
        }
        
        return null;  // Nessun match
    }
    
    /**
     * Trova il gruppo che contiene una certa parola
     */
    public WordGroup findGroupForWord(String word) {
        for (WordGroup group : groups) {
            if (group.containsWord(word)) {
                return group;
            }
        }
        return null;
    }
    
    /**
     * Restituisce l'indice del gruppo (0-3) che matcha la proposta
     * @return indice 0-3, oppure -1 se nessun match
     */
    public int getGroupIndex(List<String> proposedWords) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).matches(proposedWords)) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public String toString() {
        return String.format("Game[id=%d, active=%b, remaining=%dms]", 
            gameId, isActive(), getRemainingTime());
    }
}
