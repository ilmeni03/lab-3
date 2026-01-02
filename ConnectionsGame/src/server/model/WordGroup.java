package server.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rappresenta un gruppo di 4 parole con un tema comune.
 */
public class WordGroup {
    private final String theme;
    private final String[] words;  // Sempre 4 parole
    private final Set<String> wordSet;  // Per ricerca veloce
    
    /**
     * Costruttore
     * @param theme il tema che accomuna le parole
     * @param words array di 4 parole
     */
    public WordGroup(String theme, String[] words) {
        if (words == null || words.length != 4) {
            throw new IllegalArgumentException("Un gruppo deve contenere esattamente 4 parole");
        }
        
        this.theme = theme;
        this.words = words;
        this.wordSet = new HashSet<>(Arrays.asList(words));
    }
    
    /**
     * Costruttore alternativo con List
     */
    public WordGroup(String theme, List<String> wordsList) {
        this(theme, wordsList.toArray(new String[0]));
    }
    
    public String getTheme() {
        return theme;
    }
    
    public String[] getWords() {
        return words.clone();  // Restituisce copia per immutabilità
    }
    
    public List<String> getWordsList() {
        return Arrays.asList(words);
    }
    
    /**
     * Verifica se questo gruppo contiene tutte le parole specificate
     * @param proposedWords le parole da verificare
     * @return true se le 4 parole corrispondono esattamente a questo gruppo
     */
    public boolean matches(String[] proposedWords) {
        if (proposedWords == null || proposedWords.length != 4) {
            return false;
        }
        
        Set<String> proposedSet = new HashSet<>(Arrays.asList(proposedWords));
        return wordSet.equals(proposedSet);
    }
    
    /**
     * Verifica se questo gruppo contiene tutte le parole specificate (con List)
     */
    public boolean matches(List<String> proposedWords) {
        if (proposedWords == null || proposedWords.size() != 4) {
            return false;
        }
        
        Set<String> proposedSet = new HashSet<>(proposedWords);
        return wordSet.equals(proposedSet);
    }
    
    /**
     * Verifica se una parola appartiene a questo gruppo
     */
    public boolean containsWord(String word) {
        return wordSet.contains(word);
    }
    
    @Override
    public String toString() {
        return String.format("WordGroup[theme=%s, words=%s]", theme, Arrays.toString(words));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WordGroup other = (WordGroup) obj;
        return theme.equals(other.theme) && wordSet.equals(other.wordSet);
    }
    
    @Override
    public int hashCode() {
        return 31 * theme.hashCode() + wordSet.hashCode();
    }
}
