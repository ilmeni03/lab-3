package server.protocol;

import java.util.List;

/**
 * Rappresenta una richiesta JSON dal client.
 * Contiene tutti i campi possibili per qualsiasi operazione.
 */
public class JsonRequest {
    // Campo obbligatorio: tipo di operazione
    private String operation;
    
    // Campi per register
    private String name;
    private String psw;
    
    // Campi per updateCredentials
    private String oldName;
    private String newName;
    private String oldPsw;
    private String newPsw;
    
    // Campi per login/logout
    private String username;
    
    // Campi per submitProposal
    private List<String> words;
    
    // Campi per requestGameInfo/requestGameStats
    private Integer gameId;  // null = partita corrente
    
    // Campi per requestLeaderboard
    private String playerName;  // null = intera classifica
    private Integer topPlayers;  // null = tutti i giocatori
    
    // Getters e Setters
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPsw() {
        return psw;
    }
    
    public void setPsw(String psw) {
        this.psw = psw;
    }
    
    public String getOldName() {
        return oldName;
    }
    
    public void setOldName(String oldName) {
        this.oldName = oldName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getOldPsw() {
        return oldPsw;
    }
    
    public void setOldPsw(String oldPsw) {
        this.oldPsw = oldPsw;
    }
    
    public String getNewPsw() {
        return newPsw;
    }
    
    public void setNewPsw(String newPsw) {
        this.newPsw = newPsw;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public List<String> getWords() {
        return words;
    }
    
    public void setWords(List<String> words) {
        this.words = words;
    }
    
    public Integer getGameId() {
        return gameId;
    }
    
    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public Integer getTopPlayers() {
        return topPlayers;
    }
    
    public void setTopPlayers(Integer topPlayers) {
        this.topPlayers = topPlayers;
    }
}
