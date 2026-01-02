package server;

import server.manager.ConfigManager;
import server.manager.GameManager;
import server.manager.UserManager;
import server.model.*;
import server.util.JsonGameLoader;

import java.util.Arrays;
import java.util.List;

/**
 * Test per UserManager e GameManager.
 * Simula una partita completa con 2 giocatori.
 */
public class TestManagers {

    public static void main(String[] args) {
        System.out.println("=== TEST: UserManager & GameManager ===\n");

        try {
            // 1. Inizializza configurazione
            ConfigManager.initialize("config/server.properties");
            ConfigManager config = ConfigManager.getInstance();

            // 2. Crea UserManager
            System.out.println("1. Creazione UserManager...");
            UserManager userManager = new UserManager();
            System.out.println();

            // 3. Registra alcuni utenti
            System.out.println("2. Registrazione utenti...");
            userManager.register("alice", "pass123");
            userManager.register("bob", "pass456");
            userManager.register("charlie", "pass789");
            System.out.println();

            // 4. Prova login
            System.out.println("3. Login utenti...");
            userManager.login("alice", "pass123");
            userManager.login("bob", "pass456");
            System.out.println();

            // 5. Inizializza GameManager
            System.out.println("4. Inizializzazione GameManager...");
            String jsonPath = config.getProperty("game.file.path");
            long gameDuration = 60000;  // 60 secondi per il test

            JsonGameLoader loader = new JsonGameLoader(jsonPath);
            loader.initialize();

            GameManager gameManager = new GameManager(loader, userManager, gameDuration);
            System.out.println();

            // 6. Avvia una partita
            System.out.println("5. Avvio partita...");
            gameManager.startNewGame();
            System.out.println();

            // 7. I giocatori entrano nella partita
            System.out.println("6. I giocatori entrano nella partita...");
            PlayerGameState aliceState = gameManager.joinGame("alice");
            PlayerGameState bobState = gameManager.joinGame("bob");
            System.out.println();

            // 8. Mostra le parole della partita
            Game currentGame = gameManager.getCurrentGame();
            System.out.println("7. Parole della partita:");
            System.out.println("   " + currentGame.getAllWords());
            System.out.println();

            System.out.println("8. Gruppi corretti (per il test):");
            List<WordGroup> groups = currentGame.getGroups();
            for (int i = 0; i < groups.size(); i++) {
                WordGroup g = groups.get(i);
                System.out.println("   Gruppo " + (i + 1) + " (" + g.getTheme() + "): "
                        + Arrays.toString(g.getWords()));
            }
            System.out.println();

            // 9. Alice prova una proposta corretta
            System.out.println("9. Alice prova una proposta corretta...");
            WordGroup firstGroup = groups.get(0);
            List<String> aliceProposal = Arrays.asList(firstGroup.getWords());

            GameManager.ProposalResult result1 = gameManager.submitProposal("alice", aliceProposal);
            System.out.println("   Risultato: " + result1.message);
            System.out.println("   Punteggio Alice: " + result1.newScore);
            System.out.println();

            // 10. Bob prova una proposta sbagliata
            System.out.println("10. Bob prova una proposta sbagliata...");
            List<String> allWords = currentGame.getAllWords();
            List<String> bobWrongProposal = Arrays.asList(
                    allWords.get(0), allWords.get(1), allWords.get(2), allWords.get(3)
            );

            GameManager.ProposalResult result2 = gameManager.submitProposal("bob", bobWrongProposal);
            System.out.println("   Risultato: " + result2.message);
            System.out.println("   Punteggio Bob: " + result2.newScore);
            System.out.println();

            // 11. Alice prova un'altra proposta corretta
            System.out.println("11. Alice trova un altro gruppo...");
            WordGroup secondGroup = groups.get(1);
            List<String> aliceProposal2 = Arrays.asList(secondGroup.getWords());

            GameManager.ProposalResult result3 = gameManager.submitProposal("alice", aliceProposal2);
            System.out.println("   Risultato: " + result3.message);
            System.out.println("   Punteggio Alice: " + result3.newScore);
            System.out.println();

            // 11b. Alice trova il terzo gruppo e vince
            System.out.println("11b. Alice trova il terzo gruppo (VITTORIA)...");
            WordGroup thirdGroup = groups.get(2);
            List<String> aliceProposal3 = Arrays.asList(thirdGroup.getWords());

            GameManager.ProposalResult result4 = gameManager.submitProposal("alice", aliceProposal3);
            System.out.println("   Risultato: " + result4.message);
            System.out.println("   Punteggio Alice: " + result4.newScore);
            System.out.println();

            // 11c. Bob fa altri errori e perde
            System.out.println("11c. Bob fa altri errori e perde...");
            for (int i = 1; i < 4; i++) {
                List<String> bobWrong = Arrays.asList(
                        allWords.get(i * 4), allWords.get(i * 4 + 1),
                        allWords.get(i * 4 + 2), allWords.get(i * 4 + 3)
                );
                GameManager.ProposalResult bobResult = gameManager.submitProposal("bob", bobWrong);
                System.out.println("   Tentativo " + (i + 1) + ": " + bobResult.message
                        + " (score: " + bobResult.newScore + ")");
            }
            System.out.println();

            // 12. Statistiche aggiornate
            System.out.println("12. Statistiche della partita (dopo le partite finite):");
            GameManager.GameStats stats = gameManager.getCurrentGameStats();
            System.out.println("   Game ID: " + stats.gameId);
            System.out.println("   Attiva: " + stats.active);
            System.out.println("   Tempo rimanente: " + (stats.remainingTime / 1000) + "s");
            System.out.println("   Giocatori totali: " + stats.totalPlayers);
            System.out.println("   Giocatori finiti: " + stats.finishedPlayers);
            System.out.println("   Vincitori: " + stats.winners);
            System.out.println();

            // 13. Classifica aggiornata
            System.out.println("13. Classifica globale (dopo le partite finite):");
            List<User> leaderboard = userManager.getLeaderboard();
            for (int i = 0; i < leaderboard.size(); i++) {
                User u = leaderboard.get(i);
                System.out.printf("   %d. %-10s - %3d punti | Partite: %d | Vinte: %d | Win Rate: %.1f%%%n",
                        (i + 1), u.getUsername(), u.getTotalScore(),
                        u.getPuzzlesCompleted(), u.getPuzzlesWon(), u.getWinRate());
            }
            System.out.println();

            // 13b. Statistiche dettagliate di Alice
            System.out.println("13b. Statistiche dettagliate di Alice:");
            User alice = userManager.getUser("alice");
            System.out.println("   Punteggio totale: " + alice.getTotalScore());
            System.out.println("   Partite completate: " + alice.getPuzzlesCompleted());
            System.out.println("   Partite vinte: " + alice.getPuzzlesWon());
            System.out.println("   Win Rate: " + String.format("%.1f%%", alice.getWinRate()));
            System.out.println("   Current Streak: " + alice.getCurrentStreak());
            System.out.println("   Max Streak: " + alice.getMaxStreak());
            System.out.println("   Perfect Puzzles: " + alice.getPerfectPuzzles());
            System.out.println();

            // 14. Cleanup
            System.out.println("14. Pulizia risorse...");
            gameManager.shutdown();
            loader.close();

            System.out.println("\nTEST COMPLETATO CON SUCCESSO!");
            System.out.println("Il GameManager continuera a far partire nuove partite");
            System.out.println("finche non viene chiuso manualmente.");

        } catch (Exception e) {
            System.err.println("\nERRORE durante il test:");
            e.printStackTrace();
        }
    }
}
