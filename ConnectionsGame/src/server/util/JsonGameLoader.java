package server.util;

import com.google.gson.stream.JsonReader;
import server.model.Game;
import server.model.WordGroup;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Carica le partite dal file JSON usando GSON Streaming API.
 * NON carica tutto il file in memoria, ma legge una partita alla volta.
 *
 * Questo approccio è fondamentale per gestire file JSON di grandi dimensioni.
 */
public class JsonGameLoader {
    private final String jsonFilePath;
    private FileReader fileReader;
    private JsonReader jsonReader;
    private int currentGameIndex;
    private boolean hasMoreGames;

    /**
     * Costruttore
     * @param jsonFilePath percorso del file games.json
     */
    public JsonGameLoader(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
        this.currentGameIndex = 0;
        this.hasMoreGames = true;
    }

    /**
     * Inizializza il reader per iniziare a leggere il file
     */
    public void initialize() throws IOException {
        fileReader = new FileReader(jsonFilePath);
        jsonReader = new JsonReader(fileReader);

        // Il file JSON è un array di partite: [...]
        jsonReader.beginArray();

        System.out.println("[OK] JsonGameLoader inizializzato: " + jsonFilePath);
    }

    /**
     * Verifica se ci sono ancora partite da leggere
     */
    public boolean hasNext() throws IOException {
        if (jsonReader == null) {
            throw new IllegalStateException("JsonGameLoader non inizializzato. Chiamare initialize()");
        }

        hasMoreGames = jsonReader.hasNext();
        return hasMoreGames;
    }

    /**
     * Legge la prossima partita dal file JSON.
     * Usa la durata specificata (in millisecondi) per creare l'oggetto Game.
     *
     * @param gameDuration durata della partita in millisecondi
     * @return oggetto Game, o null se non ci sono più partite
     */
    public Game loadNextGame(long gameDuration) throws IOException {
        if (!hasNext()) {
            return null;
        }

        // Inizia a leggere un oggetto partita: {...}
        jsonReader.beginObject();

        int gameId = -1;
        List<WordGroup> groups = new ArrayList<>();

        // Legge i campi dell'oggetto partita
        while (jsonReader.hasNext()) {
            String fieldName = jsonReader.nextName();

            if (fieldName.equals("gameId")) {
                gameId = jsonReader.nextInt();

            } else if (fieldName.equals("groups")) {
                // Inizia a leggere l'array di gruppi: [...]
                jsonReader.beginArray();

                // Legge ogni gruppo
                while (jsonReader.hasNext()) {
                    WordGroup group = readWordGroup();
                    groups.add(group);
                }

                jsonReader.endArray();

            } else {
                // Campo sconosciuto, lo saltiamo
                jsonReader.skipValue();
            }
        }

        jsonReader.endObject();

        // Verifica che abbiamo letto tutti i dati necessari
        if (gameId == -1 || groups.size() != 4) {
            throw new IOException("Partita malformata nel JSON (gameId=" + gameId +
                    ", groups=" + groups.size() + ")");
        }

        currentGameIndex++;

        // Crea e restituisce l'oggetto Game
        Game game = new Game(gameId, groups, gameDuration);

        System.out.println("[OK] Caricata partita #" + gameId + " (indice " +
                (currentGameIndex - 1) + ")");

        return game;
    }

    /**
     * Legge un singolo WordGroup dal JSON
     */
    private WordGroup readWordGroup() throws IOException {
        jsonReader.beginObject();

        String theme = null;
        List<String> words = new ArrayList<>();

        while (jsonReader.hasNext()) {
            String fieldName = jsonReader.nextName();

            if (fieldName.equals("theme")) {
                theme = jsonReader.nextString();

            } else if (fieldName.equals("words")) {
                // Legge l'array di parole
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    words.add(jsonReader.nextString());
                }
                jsonReader.endArray();

            } else {
                jsonReader.skipValue();
            }
        }

        jsonReader.endObject();

        if (theme == null || words.size() != 4) {
            throw new IOException("Gruppo malformato nel JSON (theme=" + theme +
                    ", words=" + words.size() + ")");
        }

        return new WordGroup(theme, words);
    }

    /**
     * Chiude il reader e libera le risorse
     */
    public void close() throws IOException {
        if (jsonReader != null) {
            try {
                // Se siamo ancora dentro l'array, terminalo
                if (hasMoreGames) {
                    jsonReader.endArray();
                }
            } catch (Exception e) {
                // Ignora errori durante la chiusura
            }
            jsonReader.close();
        }

        if (fileReader != null) {
            fileReader.close();
        }

        System.out.println("[OK] JsonGameLoader chiuso (caricate " + currentGameIndex + " partite)");
    }

    /**
     * Resetta il loader per ricominciare dall'inizio del file
     */
    public void reset() throws IOException {
        close();
        currentGameIndex = 0;
        hasMoreGames = true;
        initialize();
    }

    /**
     * Restituisce il numero di partite caricate finora
     */
    public int getLoadedGamesCount() {
        return currentGameIndex;
    }
}
