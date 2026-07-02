import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

/**
 * GameMode
 * --------
 * Interfaccia che ogni modalita' di gioco deve implementare.
 *
 * Come aggiungere una NUOVA modalita' di gioco:
 *   1. Crea una nuova classe, es. CardMode.java, che implementa GameMode
 *   2. Implementa tutti i metodi qui sotto
 *   3. Registrala in GamePanel.buildModeRegistry() con un ID univoco
 *   4. Aggiungila come opzione in TeacherPanel
 *
 * Non serve toccare nessun altro file esistente.
 */
public interface GameMode {

    /**
     * Identificatore univoco della modalita'.
     * Deve corrispondere a GameConfig.getGameModeId().
     * Esempio: "bubble", "card", "quiz"
     */
    String getModeId();

    /**
     * Nome leggibile mostrato nel pannello docente.
     * Esempio: "Bolle rimbalzanti", "Carte", "Quiz a scelta"
     */
    String getDisplayName();

    /**
     * Chiamato una sola volta quando la partita inizia.
     * Usa GameConfig.get() per leggere i parametri configurati.
     * @param state lo stato della partita appena creato
     */
    void init(GameState state);

    /**
     * Chiamato ~60 volte al secondo dal game loop.
     * Aggiorna la logica di gioco (posizioni, collisioni, ecc.)
     */
    void update();

    /**
     * Chiamato ~60 volte al secondo dopo update().
     * Disegna tutto il contenuto della modalita' sul pannello.
     * @param g contesto grafico del pannello (800x600)
     */
    void draw(Graphics2D g);

    /**
     * Chiamato quando l'utente clicca sul pannello di gioco.
     * La modalita' decide cosa fare con il click.
     */
    void onMouseClick(MouseEvent e);

    /**
     * Restituisce il testo della domanda corrente (per il monitor del docente).
     * Ritorna stringa vuota se non applicabile.
     */
    default String getCurrentQuestion() { return ""; }

    /**
     * Chiamato quando la partita termina (tempo scaduto, vite finite, o uscita volontaria).
     * Usato per fermare timer interni o animazioni pendenti.
     */
    default void onGameEnd() {}
}
