package server;

import server.manager.ConfigManager;
import server.model.Game;
import server.model.WordGroup;
import server.util.JsonGameLoader;

import java.util.List;

/**
 * Programma di test per verificare il caricamento delle partite dal JSON.
 *
 * COME USARLO:
 * 1. Assicurati che games.json sia in data/
 * 2. Assicurati che server.properties sia in config/
 * 3. Compila ed esegui questo programma
 */
public class TestGameLoader {

    public static void main(String[] args) {
        System.out.println("=== TEST: Caricamento Partite dal JSON ===\n");

        try {
            // 1. Carica la configurazione
            System.out.println("1. Caricamento configurazione...");
            ConfigManager.initialize("config/server.properties");
            ConfigManager config = ConfigManager.getInstance();

            String jsonPath = config.getProperty("game.file.path");
            int gameDurationMinutes = config.getIntProperty("game.duration.minutes");
            long gameDurationMs = gameDurationMinutes * 60 * 1000L;

            System.out.println("   File partite: " + jsonPath);
            System.out.println("   Durata partita: " + gameDurationMinutes + " minuti\n");

            // 2. Inizializza il loader
            System.out.println("2. Inizializzazione JsonGameLoader...");
            JsonGameLoader loader = new JsonGameLoader(jsonPath);
            loader.initialize();
            System.out.println();

            // 3. Carica le prime 3 partite come test
            System.out.println("3. Caricamento prime 3 partite...\n");

            for (int i = 0; i < 3 && loader.hasNext(); i++) {
                Game game = loader.loadNextGame(gameDurationMs);

                if (game != null) {
                    printGameInfo(game);
                    System.out.println();
                }
            }

            // 4. Chiudi il loader
            System.out.println("4. Chiusura loader...");
            loader.close();

            System.out.println("\nTEST COMPLETATO CON SUCCESSO!");

        } catch (Exception e) {
            System.err.println("\nERRORE durante il test:");
            e.printStackTrace();
        }
    }

    /**
     * Stampa le informazioni di una partita
     */
    private static void printGameInfo(Game game) {
        System.out.println("==============================================");
        System.out.println("  PARTITA #" + game.getGameId());
        System.out.println("==============================================");

        List<WordGroup> groups = game.getGroups();
        for (int i = 0; i < groups.size(); i++) {
            WordGroup group = groups.get(i);
            System.out.printf("  Gruppo %d: %s%n", (i + 1), group.getTheme());
            System.out.print("    Parole: ");

            String[] words = group.getWords();
            for (int j = 0; j < words.length; j++) {
                System.out.print(words[j]);
                if (j < words.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("  Parole mescolate inviate ai giocatori:");
        System.out.print("    ");

        List<String> shuffled = game.getAllWords();
        for (int i = 0; i < shuffled.size(); i++) {
            System.out.print(shuffled.get(i));
            if (i < shuffled.size() - 1) {
                System.out.print(", ");
            }
            if ((i + 1) % 4 == 0 && i < shuffled.size() - 1) {
                System.out.print("\n    ");
            }
        }
        System.out.println();

        System.out.println();
        System.out.printf("  Durata: %d minuti%n", game.getDuration() / 60000);
        System.out.printf("  Tempo rimanente: %d ms%n", game.getRemainingTime());
        System.out.printf("  Attiva: %b%n", game.isActive());
        System.out.println("==============================================");
    }
}
