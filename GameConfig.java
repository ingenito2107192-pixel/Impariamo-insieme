/**
 * GameConfig
 * ----------
 * Singleton con tutti i parametri configurabili dal docente.
 * Letto da GameState, BubbleMode e TeacherPanel.
 */
public class GameConfig {

    private static volatile GameConfig instance;
    public static GameConfig get() {
        if (instance == null) {
            synchronized (GameConfig.class) {
                if (instance == null) instance = new GameConfig();
            }
        }
        return instance;
    }
    private GameConfig() { reset(); }

    // ── Parametri configurabili ───────────────────────────────────

    /** 0=addizione, 1=sottrazione, 2=moltiplicazione, 3=divisione */
    private int operation = 0;

    /** 0=facile, 1=medio, 2=difficile */
    private int difficulty = 1;

    /** Durata sessione in secondi */
    private int sessionDuration = 60;

    /** Numero di vite iniziali */
    private int initialLives = 3;

    /** Mostra filastrocca prima del gioco */
    private boolean showFilastrocca = true;

    /** Numero di oggetti sullo schermo */
    private int numOggetti = 10;

    /** Numero massimo di domande (0 = illimitato) */
    private int numDomande = 10;

    /** ID modalità di gioco attiva */
    private String gameModeId = "bubble";

    // ── Getters / Setters ─────────────────────────────────────────

    public int     getOperation()            { return operation; }
    public void    setOperation(int v)       { operation = v; }

    public int     getDifficulty()           { return difficulty; }
    public void    setDifficulty(int v)      { difficulty = Math.max(0, Math.min(2, v)); }

    public int     getSessionDuration()      { return sessionDuration; }
    public void    setSessionDuration(int v) { sessionDuration = Math.max(10, Math.min(600, v)); }

    public int     getInitialLives()         { return initialLives; }
    public void    setInitialLives(int v)    { initialLives = Math.max(1, Math.min(10, v)); }

    public boolean isShowFilastrocca()           { return showFilastrocca; }
    public void    setShowFilastrocca(boolean v) { showFilastrocca = v; }

    public int     getNumOggetti()           { return numOggetti; }
    public void    setNumOggetti(int v)      { numOggetti = Math.max(2, Math.min(30, v)); }

    public int     getNumDomande()           { return numDomande; }
    public void    setNumDomande(int v)      { numDomande = Math.max(1, Math.min(100, v)); }

    public String  getGameModeId()           { return gameModeId; }
    public void    setGameModeId(String v)   { gameModeId = v; }

    // ── Pool oggetti in base alla difficoltà ──────────────────────

    public ObjectType[] getAvailableTypes() {
        switch (difficulty) {
            case 0:  return ObjectType.EASY;
            case 1:  return merge(ObjectType.EASY, ObjectType.MEDIUM);
            default: return merge(ObjectType.EASY, ObjectType.MEDIUM, ObjectType.HARD);
        }
    }

    private ObjectType[] merge(ObjectType[]... arrays) {
        int total = 0;
        for (ObjectType[] a : arrays) total += a.length;
        ObjectType[] result = new ObjectType[total];
        int i = 0;
        for (ObjectType[] a : arrays)
            for (ObjectType t : a) result[i++] = t;
        return result;
    }

    // ── Reset ─────────────────────────────────────────────────────

    public void reset() {
        operation       = 0;
        difficulty      = 1;
        sessionDuration = 60;
        initialLives    = 3;
        numOggetti      = 10;
        numDomande      = 10;
        showFilastrocca = true;
        gameModeId      = "bubble";
    }

    // ── Costanti leggibili ────────────────────────────────────────

    public static final String[] OP_NOMI    = { "ADDIZIONE", "SOTTRAZIONE", "MOLTIPLICAZIONE", "DIVISIONE" };
    public static final String[] OP_SIMBOLI = { "+", "-", "x", "÷" };
    public static final String[] DIFF_NOMI  = { "Facile", "Medio", "Difficile" };
}
