/**
 * GameState
 * ---------
 * Contiene lo stato mutabile di una singola partita.
 * I valori vengono letti da GameConfig al momento della creazione,
 * quindi GameConfig deve essere aggiornato PRIMA di creare GameState.
 */
public class GameState {

    public int     score    = 0;
    public int     lives;
    public int     timeLeft;
    public boolean gameOver = false;
    public int     operation;
    public int     difficulty;

    /** Domande già risposto correttamente */
    public int domandeRisposte = 0;

    /** Limite massimo di domande (0 = solo tempo) */
    public int maxDomande;

    /** Difficoltà dinamica: cresce durante la partita */
    public int dynamicDifficulty = 0;

    public GameState() {
        GameConfig cfg = GameConfig.get();
        this.lives             = cfg.getInitialLives();
        this.timeLeft          = cfg.getSessionDuration();
        this.operation         = cfg.getOperation();
        this.difficulty        = cfg.getDifficulty();
        this.maxDomande        = cfg.getNumDomande();
        this.dynamicDifficulty = cfg.getDifficulty();   // parte dal livello scelto dal docente

        System.out.println("[GameState] Creato:"
            + " tempo=" + timeLeft + "s"
            + " domande=" + maxDomande
            + " difficolta=" + difficulty
            + " operazione=" + operation);
    }

    // ── Azioni di gioco ────────────────────────────────────────────

    /** Risposta corretta */
    public void correctAnswer() {
        score += 5;
        domandeRisposte++;
        lives++;  // guadagna una vita per ogni risposta corretta
        if (maxDomande > 0 && domandeRisposte >= maxDomande) {
            gameOver = true;
            System.out.println("[GameState] Limite domande raggiunto (" + maxDomande + "). Fine partita.");
        }
    }

    /** Risposta errata */
    public void wrongAnswer() {
        lives--;
        if (lives <= 0) gameOver = true;
    }

    /** Chiamato ogni secondo: decrementa tempo e aggiorna difficoltà dinamica */
    public void tick() {
        if (timeLeft > 0) {
            timeLeft--;
            // Difficoltà dinamica: aumenta ogni 20 secondi trascorsi, partendo da quella del docente
            int elapsed = GameConfig.get().getSessionDuration() - timeLeft;
            int boost   = elapsed / 20;
            dynamicDifficulty = Math.min(difficulty + boost, 2);
        } else {
            gameOver = true;
            System.out.println("[GameState] Tempo scaduto. Fine partita.");
        }
    }
}
