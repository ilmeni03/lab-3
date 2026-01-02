package server.handler;

import com.google.gson.Gson;
import server.manager.GameManager;
import server.manager.UserManager;
import server.model.*;
import server.protocol.JsonRequest;
import server.protocol.JsonResponse;

import java.util.*;

/**
 * Gestisce i comandi ricevuti dai client.
 * Coordina UserManager e GameManager per eseguire le operazioni.
 */
public class CommandHandler {
    private final UserManager userManager;
    private final GameManager gameManager;
    private final Gson gson;
    
    // Traccia quale utente è loggato su ogni connessione
    private String loggedUsername;
    
    public CommandHandler(UserManager userManager, GameManager gameManager) {
        this.userManager = userManager;
        this.gameManager = gameManager;
        this.gson = new Gson();
        this.loggedUsername = null;
    }
    
    /**
     * Processa una richiesta JSON e restituisce la risposta JSON.
     */
    public String handleCommand(String jsonRequest) {
        try {
            // Parse della richiesta
            JsonRequest request = gson.fromJson(jsonRequest, JsonRequest.class);
            
            if (request.getOperation() == null) {
                return toJson(JsonResponse.error("Campo 'operation' mancante"));
            }
            
            // Dispatch al metodo appropriato
            JsonResponse response;
            switch (request.getOperation()) {
                case "register":
                    response = handleRegister(request);
                    break;
                case "updateCredentials":
                    response = handleUpdateCredentials(request);
                    break;
                case "login":
                    response = handleLogin(request);
                    break;
                case "logout":
                    response = handleLogout(request);
                    break;
                case "submitProposal":
                    response = handleSubmitProposal(request);
                    break;
                case "requestGameInfo":
                    response = handleRequestGameInfo(request);
                    break;
                case "requestGameStats":
                    response = handleRequestGameStats(request);
                    break;
                case "requestLeaderboard":
                    response = handleRequestLeaderboard(request);
                    break;
                case "requestPlayerStats":
                    response = handleRequestPlayerStats(request);
                    break;
                default:
                    response = JsonResponse.error("Operazione sconosciuta: " + request.getOperation());
            }
            
            return toJson(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return toJson(JsonResponse.error("Errore nel processare la richiesta: " + e.getMessage()));
        }
    }
    
    /**
     * REGISTER: Registra un nuovo utente
     */
    private JsonResponse handleRegister(JsonRequest req) {
        if (req.getName() == null || req.getPsw() == null) {
            return JsonResponse.error("Campi 'name' e 'psw' obbligatori");
        }
        
        boolean success = userManager.register(req.getName(), req.getPsw());
        
        if (success) {
            return JsonResponse.success("Registrazione completata con successo");
        } else {
            return JsonResponse.error("Username già esistente");
        }
    }
    
    /**
     * UPDATE_CREDENTIALS: Aggiorna username e/o password
     */
    private JsonResponse handleUpdateCredentials(JsonRequest req) {
        if (req.getOldName() == null || req.getOldPsw() == null) {
            return JsonResponse.error("Campi 'oldName' e 'oldPsw' obbligatori");
        }
        
        boolean success = userManager.updateCredentials(
            req.getOldName(), 
            req.getNewName(), 
            req.getOldPsw(), 
            req.getNewPsw()
        );
        
        if (success) {
            return JsonResponse.success("Credenziali aggiornate con successo");
        } else {
            return JsonResponse.error("Aggiornamento fallito: verifica le credenziali");
        }
    }
    
    /**
     * LOGIN: Effettua il login
     */
    private JsonResponse handleLogin(JsonRequest req) {
        if (req.getUsername() == null || req.getPsw() == null) {
            return JsonResponse.error("Campi 'username' e 'psw' obbligatori");
        }
        
        if (loggedUsername != null) {
            return JsonResponse.error("Sei già loggato come: " + loggedUsername);
        }
        
        boolean success = userManager.login(req.getUsername(), req.getPsw());
        
        if (success) {
            loggedUsername = req.getUsername();
            
            // Fa entrare automaticamente nella partita corrente
            PlayerGameState state = gameManager.joinGame(loggedUsername);
            Game currentGame = gameManager.getCurrentGame();
            
            if (currentGame != null && state != null) {
                // Prepara i dati della partita
                Map<String, Object> gameData = new HashMap<>();
                gameData.put("gameId", currentGame.getGameId());
                gameData.put("words", currentGame.getAllWords());
                gameData.put("remainingTime", currentGame.getRemainingTime());
                gameData.put("correctProposals", state.getCorrectProposals());
                gameData.put("wrongProposals", state.getWrongProposals());
                gameData.put("currentScore", state.getCurrentScore());
                
                return JsonResponse.success("Login effettuato", gameData);
            } else {
                return JsonResponse.success("Login effettuato (nessuna partita attiva)");
            }
        } else {
            return JsonResponse.error("Credenziali errate");
        }
    }
    
    /**
     * LOGOUT: Effettua il logout
     */
    private JsonResponse handleLogout(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Non sei loggato");
        }
        
        userManager.logout(loggedUsername);
        loggedUsername = null;
        
        return JsonResponse.success("Logout effettuato");
    }
    
    /**
     * SUBMIT_PROPOSAL: Invia una proposta
     */
    private JsonResponse handleSubmitProposal(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Devi effettuare il login");
        }
        
        if (req.getWords() == null || req.getWords().size() != 4) {
            return JsonResponse.error("Devi proporre esattamente 4 parole");
        }
        
        GameManager.ProposalResult result = gameManager.submitProposal(
            loggedUsername, 
            req.getWords()
        );
        
        Map<String, Object> data = new HashMap<>();
        data.put("correct", result.correct);
        data.put("newScore", result.newScore);
        
        if (result.correct && result.foundGroup != null) {
            data.put("theme", result.foundGroup.getTheme());
        }
        
        return new JsonResponse(result.correct, result.message, data);
    }
    
    /**
     * REQUEST_GAME_INFO: Richiede info su una partita
     */
    private JsonResponse handleRequestGameInfo(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Devi effettuare il login");
        }
        
        // TODO: Implementare richiesta info partita specifica
        return JsonResponse.error("Funzionalità non ancora implementata");
    }
    
    /**
     * REQUEST_GAME_STATS: Richiede statistiche partita
     */
    private JsonResponse handleRequestGameStats(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Devi effettuare il login");
        }
        
        GameManager.GameStats stats = gameManager.getCurrentGameStats();
        
        if (stats == null) {
            return JsonResponse.error("Nessuna partita in corso");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("gameId", stats.gameId);
        data.put("active", stats.active);
        data.put("remainingTime", stats.remainingTime);
        data.put("totalPlayers", stats.totalPlayers);
        data.put("finishedPlayers", stats.finishedPlayers);
        data.put("winners", stats.winners);
        
        return JsonResponse.success("Statistiche partita", data);
    }
    
    /**
     * REQUEST_LEADERBOARD: Richiede la classifica
     */
    private JsonResponse handleRequestLeaderboard(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Devi effettuare il login");
        }
        
        List<Map<String, Object>> leaderboardData = new ArrayList<>();
        
        if (req.getPlayerName() != null) {
            // Posizione di un giocatore specifico
            int rank = userManager.getUserRank(req.getPlayerName());
            if (rank == -1) {
                return JsonResponse.error("Giocatore non trovato");
            }
            
            User user = userManager.getUser(req.getPlayerName());
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("rank", rank);
            playerData.put("username", user.getUsername());
            playerData.put("score", user.getTotalScore());
            
            return JsonResponse.success("Posizione in classifica", playerData);
            
        } else if (req.getTopPlayers() != null) {
            // Top K giocatori
            List<User> topUsers = userManager.getTopKUsers(req.getTopPlayers());
            
            for (int i = 0; i < topUsers.size(); i++) {
                User u = topUsers.get(i);
                Map<String, Object> userData = new HashMap<>();
                userData.put("rank", i + 1);
                userData.put("username", u.getUsername());
                userData.put("score", u.getTotalScore());
                leaderboardData.add(userData);
            }
            
        } else {
            // Intera classifica
            List<User> allUsers = userManager.getLeaderboard();
            
            for (int i = 0; i < allUsers.size(); i++) {
                User u = allUsers.get(i);
                Map<String, Object> userData = new HashMap<>();
                userData.put("rank", i + 1);
                userData.put("username", u.getUsername());
                userData.put("score", u.getTotalScore());
                leaderboardData.add(userData);
            }
        }
        
        return JsonResponse.success("Classifica", leaderboardData);
    }
    
    /**
     * REQUEST_PLAYER_STATS: Richiede statistiche personali
     */
    private JsonResponse handleRequestPlayerStats(JsonRequest req) {
        if (loggedUsername == null) {
            return JsonResponse.error("Devi effettuare il login");
        }
        
        User user = userManager.getUser(loggedUsername);
        
        if (user == null) {
            return JsonResponse.error("Utente non trovato");
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("puzzlesCompleted", user.getPuzzlesCompleted());
        stats.put("winRate", user.getWinRate());
        stats.put("lossRate", user.getLossRate());
        stats.put("currentStreak", user.getCurrentStreak());
        stats.put("maxStreak", user.getMaxStreak());
        stats.put("perfectPuzzles", user.getPerfectPuzzles());
        stats.put("mistakeHistogram", user.getMistakeHistogram());
        
        return JsonResponse.success("Statistiche personali", stats);
    }
    
    /**
     * Converte un oggetto in JSON
     */
    private String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Restituisce l'username loggato (per debug)
     */
    public String getLoggedUsername() {
        return loggedUsername;
    }
}
