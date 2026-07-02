import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BubbleMode
 * ----------
 * Modalità "oggetti rimbalzanti" con sfondo e sprite specifici per operazione:
 *
 *   Addizione      → sfondo prato        + cuori
 *   Sottrazione    → sfondo spiaggia     + bolle/palline
 *   Moltiplicazione→ sfondo cielo nott.  + stelle
 *   Divisione      → sfondo cielo        + palloncini
 *
 * Gli sprite vengono caricati da:
 *   immagini/operazioni/Sprite tagliato/<operazione>/
 * in ordine casuale a ogni nuova domanda.
 * Il file "nome.png" è l'idle; "nome 2.png" è l'esplosione al click corretto.
 */
public class BubbleMode implements GameMode {

    private static final int W = 800;
    private static final int H = 600;

    // Velocità base per livello: Facile=0.3, Medio=0.5, Difficile=1.0
    private static final float[] BASE_SPEED = { 0.3f, 0.5f, 1.0f };

    private GameState       state;
    private MathQuestion    question;
    private ArrayList<Ball> balls  = new ArrayList<>();
    private final Random    rand   = new Random();
    private final Runnable  onGameOver;
    private javax.swing.Timer nextQuestionTimer = null;

    private long wrongFlashEnd   = 0;   // timestamp ms fine flash rosso
    private long correctFlashEnd = 0;  // timestamp ms fine flash verde

    /** Sfondo caricato all'init in base all'operazione. */
    private BufferedImage background = null;

    /**
     * Pool degli sprite per l'operazione corrente.
     * Aggiornato a ogni nuova domanda (rimescolato).
     */
    private List<SpriteLoader.OperationSprite> spritePool = new ArrayList<>();

    // ── Costruttore ───────────────────────────────────────────────

    public BubbleMode(Runnable onGameOver) {
        this.onGameOver = onGameOver;
    }

    @Override public String getModeId()          { return "bubble"; }
    @Override public String getDisplayName()     { return "Oggetti rimbalzanti"; }
    @Override public String getCurrentQuestion() { return question != null ? question.questionText : ""; }

    // ── Init ──────────────────────────────────────────────────────

    @Override
    public void init(GameState state) {
        this.state = state;
        balls.clear();

        // Carica sfondo per l'operazione corrente
        background = SpriteLoader.get().getBackground(state.operation);
        if (background == null)
            System.err.println("[BubbleMode] Sfondo non trovato per op=" + state.operation);

        // Carica pool sprite operazione
        spritePool = SpriteLoader.get().getSpritesForOperation(state.operation);
        if (spritePool.isEmpty())
            System.err.println("[BubbleMode] Nessuno sprite trovato per op=" + state.operation);

        question = new MathQuestion(state.operation, state.dynamicDifficulty);
        spawnBalls();

        System.out.println("[BubbleMode] Init op=" + state.operation
            + " sprites=" + spritePool.size()
            + " numOggetti=" + GameConfig.get().getNumOggetti()
            + " tempo=" + state.timeLeft);
    }

    // ── Update ────────────────────────────────────────────────────

    @Override
    public void update() {
        for (Ball b : balls) b.update(W, H);
        balls.removeIf(Ball::isDone);
        if (balls.isEmpty() && !state.gameOver && nextQuestionTimer == null) nextQuestion();
    }

    // ── Draw ──────────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g) {
        // ── Sfondo ──────────────────────────────────────────────
        if (background != null) {
            g.drawImage(background, 0, 0, W, H, null);
        } else {
            g.setColor(new Color(245, 248, 255));
            g.fillRect(0, 0, W, H);
        }

        // ── Barra superiore ──────────────────────────────────────
        // Colore barra varia per operazione per coerenza visiva
        g.setColor(getBarColor(state.operation));
        g.fillRect(0, 0, W, 60);

        // Domanda — verde se risposta corretta appena data, bianca altrimenti
        long now = System.currentTimeMillis();
        boolean wrongFlash   = now < wrongFlashEnd;
        boolean correctFlash = now < correctFlashEnd;

        g.setFont(new Font("Arial", Font.BOLD, 30));
        if (correctFlash) {
            // Alone verde dietro il testo
            g.setColor(new Color(80, 255, 80, 70));
            FontMetrics fmQ = g.getFontMetrics();
            String solvedText = question.questionText.replace("?", String.valueOf(question.answer));
            int qw = fmQ.stringWidth(solvedText);
            g.fillRoundRect((W - qw) / 2 - 14, 10, qw + 28, 38, 12, 12);
            g.setColor(new Color(80, 255, 120));
            drawCentered(g, solvedText, 40);
        } else {
            g.setColor(Color.WHITE);
            drawCentered(g, question.questionText, 40);
        }

        // Nome operazione in alto a sinistra
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(GameConfig.OP_NOMI[state.operation], 10, 20);

        // ── Oggetti ──────────────────────────────────────────────
        for (Ball b : balls) b.draw(g);

        // ── Barra inferiore ───────────────────────────────────────
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, H - 48, W, 48);

        // Vite
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 130, 130));
        StringBuilder vite = new StringBuilder("Vite: ");
        for (int i = 0; i < state.lives; i++) vite.append("* ");
        for (int i = state.lives; i < GameConfig.get().getInitialLives(); i++) vite.append("  ");
        g.drawString(vite.toString(), 15, H - 17);

        // Livello dinamico
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(200, 220, 255));
        String lv = "Liv." + (state.dynamicDifficulty + 1);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(lv, W / 2 - fm.stringWidth(lv) / 2, H - 17);

        // Progresso domande
        if (state.maxDomande > 0) {
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(new Color(180, 220, 255));
            String domStr = "Dom: " + state.domandeRisposte + "/" + state.maxDomande;
            fm = g.getFontMetrics();
            g.drawString(domStr, W / 2 - fm.stringWidth(domStr) / 2 - 80, H - 17);
        }

        // Punti e tempo
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("Punti: " + state.score, W - 235, H - 17);
        g.setColor(state.timeLeft <= 10 ? new Color(255, 120, 120) : Color.WHITE);
        g.drawString("Tempo: " + state.timeLeft + "s", W - 110, H - 17);

        // ── Flash errato: overlay rosso che sfuma ─────────────────
        if (wrongFlash) {
            float ratio = Math.max(0f, (wrongFlashEnd - now) / 500f);
            int alpha = (int)(170 * ratio);
            g.setColor(new Color(220, 30, 30, alpha));
            g.fillRect(0, 0, W, H);
        }

        // ── Flash corretto: overlay verde che sfuma ───────────────
        if (correctFlash) {
            float ratio = Math.max(0f, (correctFlashEnd - now) / 1000f);
            int alpha = (int)(90 * ratio);
            g.setColor(new Color(40, 210, 80, alpha));
            g.fillRect(0, 0, W, H);
        }
    }

    // ── Click ─────────────────────────────────────────────────────

    @Override
    public void onMouseClick(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        for (Ball b : balls) {
            if (b.isDone() || b.state == Ball.State.EXPLODING
                           || b.state == Ball.State.HIT_WRONG) continue;
            if (b.contains(mx, my)) {
                if (b.value == question.answer) {
                    state.correctAnswer();
                    b.startExplosion();
                    correctFlashEnd = System.currentTimeMillis() + 1000;
                    if (nextQuestionTimer != null) nextQuestionTimer.stop();
                    if (state.gameOver) {
                        // Ultima domanda risposta: aspetta l'animazione poi chiudi
                        nextQuestionTimer = new javax.swing.Timer(1200, ev -> {
                            nextQuestionTimer = null;
                            onGameOver.run();
                        });
                    } else {
                        nextQuestionTimer = new javax.swing.Timer(1200, ev -> {
                            nextQuestionTimer = null;
                            nextQuestion();
                        });
                    }
                    nextQuestionTimer.setRepeats(false);
                    nextQuestionTimer.start();
                } else {
                    b.startHitWrong();
                    state.wrongAnswer();
                    wrongFlashEnd = System.currentTimeMillis() + 500;
                    if (state.gameOver) onGameOver.run();
                }
                break;
            }
        }
    }

    @Override
    public void onGameEnd() {
        if (nextQuestionTimer != null) {
            nextQuestionTimer.stop();
            nextQuestionTimer = null;
        }
        balls.clear();
    }

    // ── Logica interna ─────────────────────────────────────────────

    private void nextQuestion() {
        balls.clear();
        // Rimescola il pool sprite per la prossima domanda
        spritePool = SpriteLoader.get().getSpritesForOperation(state.operation);
        question   = new MathQuestion(state.operation, state.dynamicDifficulty);
        spawnBalls();
    }

    private void spawnBalls() {
        GameConfig cfg = GameConfig.get();

        int count      = Math.max(2, cfg.getNumOggetti() + state.dynamicDifficulty);
        int vel        = cfg.getDifficulty();
        float baseSpeed = BASE_SPEED[Math.max(0, Math.min(2, vel))];
        // Ogni livello dinamico aggiunge solo 0.1 px/frame
        float speed    = baseSpeed + state.dynamicDifficulty * 0.1f;

        int correctIdx = rand.nextInt(count);

        // Genera valori tutti distinti
        java.util.Set<Integer> usedValues = new java.util.HashSet<>();
        usedValues.add(question.answer);
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            if (i == correctIdx) {
                values[i] = question.answer;
            } else {
                int wrong = generateWrongUnique(usedValues);
                usedValues.add(wrong);
                values[i] = wrong;
            }
        }

        // Spawn con anti-sovrapposizione
        int minDist = 160; // distanza minima tra centri (px)
        int maxAttempts = 50;

        for (int i = 0; i < count; i++) {
            SpriteLoader.OperationSprite sp = !spritePool.isEmpty()
                ? spritePool.get(i % spritePool.size())
                : null;

            Ball candidate = null;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                candidate = new Ball(W, H, values[i], speed, sp);
                boolean overlaps = false;
                for (Ball existing : balls) {
                    int dx = candidate.x - existing.x;
                    int dy = candidate.y - existing.y;
                    if (dx * dx + dy * dy < minDist * minDist) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) break;
            }
            balls.add(candidate);
        }
    }

    private int generateWrong() {
        int wrong; int attempts = 0;
        // Variazione proporzionale alla risposta: almeno ±2, al più ±20
        int delta = Math.max(2, Math.min(20, question.answer / 5 + 2));
        do {
            wrong = question.answer + rand.nextInt(delta * 2 + 1) - delta;
            if (++attempts > 40) { wrong = question.answer + 1; break; }
        } while (wrong == question.answer || wrong < 0);
        return wrong;
    }

    private int generateWrongUnique(java.util.Set<Integer> usedValues) {
        int wrong;
        // Partiamo da un delta minimo di 5 per avere più candidati,
        // e lo allarghiamo ogni 10 tentativi falliti per uscire sempre dal loop.
        for (int attempt = 0; attempt < 100; attempt++) {
            int delta = Math.max(5, Math.min(30, question.answer / 4 + 5 + attempt / 10 * 5));
            wrong = question.answer + rand.nextInt(delta * 2 + 1) - delta;
            if (wrong >= 0 && !usedValues.contains(wrong)) return wrong;
        }
        // Fallback di sicurezza: primo intero positivo non ancora usato
        wrong = 1;
        while (usedValues.contains(wrong)) wrong++;
        return wrong;
    }

    // ── Utilità ───────────────────────────────────────────────────

    private Color getBarColor(int op) {
        switch (op) {
            case 0:  return new Color( 34, 100,  34); // addizione: verde scuro
            case 1:  return new Color( 30, 120, 180); // sottrazione: azzurro mare
            case 2:  return new Color( 40,  30,  90); // moltiplicazione: blu notte
            default: return new Color( 70, 130, 200); // divisione: azzurro cielo
        }
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }
}
