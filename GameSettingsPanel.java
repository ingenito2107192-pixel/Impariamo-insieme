import java.awt.*;
import java.awt.event.*;
import java.util.function.BiConsumer;
import javax.swing.*;

/**
 * GameSettingsPanel - permette al docente di configurare il gioco
 * prima di avviarlo (numero di domande, difficoltà, ecc.).
 */
public class GameSettingsPanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    private static final String[] OP_NAMES = {
        "Addizione", "Sottrazione", "Moltiplicazione", "Divisione"
    };
    private static final Color[] OP_COLORS = {
        new Color(50,  150,  200), // Sottrazione 
        new Color(30,  160,  60),  // Addizione   
        new Color(140,  50, 190),  // Moltiplica  
        new Color(220, 110,  20),  // Divisione   
    };

    // Impostazioni selezionabili (Stato)
    private int NUMERO_DI_DOMANDE = 10;
    private int TEMPO = 60;                // espresso in secondi (es. 30, 60, 90, 120)
    private int NUMERO_DI_OGGETTI = 10;
    private int VELOCITA  = 1;             // 0=Facile, 1=Medio, 2=Difficile

    private final int opIndex;
    private final BiConsumer<Integer, int[]> onStart; // (opIndex, settings[]) → avvia
    private final Runnable onBack;

    // Pulsanti di navigazione generali
    private static final int BTN_W = 180, BTN_H = 46;
    private static final int BTN_BACK_X  = 80,  BTN_Y = 500;
    private static final int BTN_START_X = 540, BTN_START_Y = 500;

    // Dimensioni controlli comuni
    private static final int ARROW_W = 40, ARROW_H = 36;
    private static final int VAL_BOX_W = 80;

    // Coordinate Layout (Diviso in due colonne: Sinistra X=240, Destra X=560)
    private static final int COL_SX_X = 240;
    private static final int COL_DX_X = 560;
    
    private static final int RIGA_1_Y = 240; // Per Numero Domande (SX) e Numero Oggetti (DX)
    private static final int RIGA_2_Y = 360; // Per Tempo (SX) e Velocità (DX)

    // Controlli pulsanti velocità
    private static final int DIFF_BTN_W = 85, DIFF_BTN_H = 36;
    private static final String[] DIFF_LABELS = {"Facile", "Media", "Difficile"};

    public GameSettingsPanel(int opIndex,
                             BiConsumer<Integer, int[]> onStart,
                             Runnable onBack) {
        this.opIndex = opIndex;
        this.onStart = onStart;
        this.onBack  = onBack;

        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    private void handleClick(int mx, int my) {
        // Pulsante Indietro
        if (inRect(mx, my, BTN_BACK_X, BTN_Y, BTN_W, BTN_H)) {
            onBack.run();
            return;
        }
        
        // Pulsante Avvia Gioco (Passa ora un array con tutti e 4 i parametri configurati)
        if (inRect(mx, my, BTN_START_X, BTN_START_Y, BTN_W, BTN_H)) {
            onStart.accept(opIndex, new int[]{ NUMERO_DI_OGGETTI, VELOCITA, NUMERO_DI_DOMANDE, TEMPO });
            return;
        }

        // ── COLONNA SINISTRA ──
        
        // 1. Numero Domande: Freccia sinistra
        int arrowDomLX = COL_SX_X - VAL_BOX_W/2 - ARROW_W - 10;
        if (inRect(mx, my, arrowDomLX, RIGA_1_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (NUMERO_DI_DOMANDE > 5) { NUMERO_DI_DOMANDE -= 5; repaint(); }
            return;
        }
        // Numero Domande: Freccia destra
        int arrowDomRX = COL_SX_X + VAL_BOX_W/2 + 10;
        if (inRect(mx, my, arrowDomRX, RIGA_1_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (NUMERO_DI_DOMANDE < 30) { NUMERO_DI_DOMANDE += 5; repaint(); }
            return;
        }

        // 2. Tempo: Freccia sinistra (-30 secondi)
        int arrowTmpLX = COL_SX_X - VAL_BOX_W/2 - ARROW_W - 10;
        if (inRect(mx, my, arrowTmpLX, RIGA_2_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (TEMPO > 30) { TEMPO -= 30; repaint(); }
            return;
        }
        // Tempo: Freccia destra (+30 secondi)
        int arrowTmpRX = COL_SX_X + VAL_BOX_W/2 + 10;
        if (inRect(mx, my, arrowTmpRX, RIGA_2_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (TEMPO < 300) { TEMPO += 30; repaint(); }
            return;
        }

        // ── COLONNA DESTRA ──
        
        // 3. Numero Oggetti: Freccia sinistra
        int arrowOggLX = COL_DX_X - VAL_BOX_W/2 - ARROW_W - 10;
        if (inRect(mx, my, arrowOggLX, RIGA_1_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (NUMERO_DI_OGGETTI > 5) { NUMERO_DI_OGGETTI -= 5; repaint(); }
            return;
        }
        // Numero Oggetti: Freccia destra
        int arrowOggRX = COL_DX_X + VAL_BOX_W/2 + 10;
        if (inRect(mx, my, arrowOggRX, RIGA_1_Y - ARROW_H/2, ARROW_W, ARROW_H)) {
            if (NUMERO_DI_OGGETTI < 30) { NUMERO_DI_OGGETTI += 5; repaint(); }
            return;
        }

        // 4. Velocità: Pulsanti Selezione
        int totalDiffW = 3 * DIFF_BTN_W + 2 * 8;
        int diffStartX = COL_DX_X - totalDiffW / 2;
        for (int i = 0; i < 3; i++) {
            int dx = diffStartX + i * (DIFF_BTN_W + 8);
            if (inRect(mx, my, dx, RIGA_2_Y - DIFF_BTN_H/2, DIFF_BTN_W, DIFF_BTN_H)) {
                VELOCITA = i;
                repaint();
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Sfondo sfumato
        GradientPaint bg = new GradientPaint(0, 0, new Color(230, 240, 255), 0, H, new Color(200, 220, 255));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        Color opColor = OP_COLORS[opIndex];

        // Titolo
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(opColor);
        String titolo = "Impostazioni – " + OP_NAMES[opIndex];
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(titolo, (W - fm.stringWidth(titolo)) / 2, 75);

        // Separatore
        g2.setColor(new Color(opColor.getRed(), opColor.getGreen(), opColor.getBlue(), 80));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(60, 95, W - 60, 95);
        g2.setStroke(new BasicStroke(1f));

        // Card centrale estesa per ospitare i 4 controlli affiancati
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillRoundRect(50, 120, W - 100, 340, 24, 24);
        g2.setColor(new Color(opColor.getRed(), opColor.getGreen(), opColor.getBlue(), 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(50, 120, W - 100, 340, 24, 24);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(50, 50, 100));

        // ─────────────────────────────────────────────────────────────────
        // COLONNA SINISTRA
        // ─────────────────────────────────────────────────────────────────
        
        // RIGA 1: NUMERO DI DOMANDE
        String lblDom = "NUMERO DI DOMANDE:";
        fm = g2.getFontMetrics();
        g2.drawString(lblDom, COL_SX_X - fm.stringWidth(lblDom) / 2, RIGA_1_Y - 30);

        int arrowDomLX = COL_SX_X - VAL_BOX_W/2 - ARROW_W - 10;
        drawArrow(g2, arrowDomLX, RIGA_1_Y - ARROW_H/2, "◀", NUMERO_DI_DOMANDE > 5, opColor);

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(new Color(30, 30, 80));
        fm = g2.getFontMetrics();
        String domStr = String.valueOf(NUMERO_DI_DOMANDE);
        g2.drawString(domStr, COL_SX_X - fm.stringWidth(domStr) / 2, RIGA_1_Y + fm.getAscent()/2 - 2);

        int arrowDomRX = COL_SX_X + VAL_BOX_W/2 + 10;
        drawArrow(g2, arrowDomRX, RIGA_1_Y - ARROW_H/2, "▶", NUMERO_DI_DOMANDE < 30, opColor);

        // RIGA 2: TEMPO DI GIOCO
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(50, 50, 100));
        String lblTempo = "TEMPO MASSIMO:";
        fm = g2.getFontMetrics();
        g2.drawString(lblTempo, COL_SX_X - fm.stringWidth(lblTempo) / 2, RIGA_2_Y - 30);

        int arrowTmpLX = COL_SX_X - VAL_BOX_W/2 - ARROW_W - 10;
        drawArrow(g2, arrowTmpLX, RIGA_2_Y - ARROW_H/2, "◀", TEMPO > 30, opColor);

        g2.setFont(new Font("Arial", Font.BOLD, 24)); // Leggermente più piccolo per far stare la "s"
        g2.setColor(new Color(30, 30, 80));
        fm = g2.getFontMetrics();
        String tmpStr = TEMPO + "s";
        g2.drawString(tmpStr, COL_SX_X - fm.stringWidth(tmpStr) / 2, RIGA_2_Y + fm.getAscent()/2 - 2);

        int arrowTmpRX = COL_SX_X + VAL_BOX_W/2 + 10;
        drawArrow(g2, arrowTmpRX, RIGA_2_Y - ARROW_H/2, "▶", TEMPO < 300, opColor);


        // ─────────────────────────────────────────────────────────────────
        // COLONNA DESTRA
        // ─────────────────────────────────────────────────────────────────
        
        // RIGA 1: NUMERO DI OGGETTI
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(50, 50, 100));
        String lblOgg = "NUMERO DI OGGETTI:";
        fm = g2.getFontMetrics();
        g2.drawString(lblOgg, COL_DX_X - fm.stringWidth(lblOgg) / 2, RIGA_1_Y - 30);

        int arrowOggLX = COL_DX_X - VAL_BOX_W/2 - ARROW_W - 10;
        drawArrow(g2, arrowOggLX, RIGA_1_Y - ARROW_H/2, "◀", NUMERO_DI_OGGETTI > 5, opColor);

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(new Color(30, 30, 80));
        fm = g2.getFontMetrics();
        String oggStr = String.valueOf(NUMERO_DI_OGGETTI);
        g2.drawString(oggStr, COL_DX_X - fm.stringWidth(oggStr) / 2, RIGA_1_Y + fm.getAscent()/2 - 2);

        int arrowOggRX = COL_DX_X + VAL_BOX_W/2 + 10;
        drawArrow(g2, arrowOggRX, RIGA_1_Y - ARROW_H/2, "▶", NUMERO_DI_OGGETTI < 30, opColor);

        // RIGA 2: VELOCITÀ
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(50, 50, 100));
        String lblDiff = "VELOCITÀ:";
        fm = g2.getFontMetrics();
        g2.drawString(lblDiff, COL_DX_X - fm.stringWidth(lblDiff) / 2, RIGA_2_Y - 30);

        int totalDiffW = 3 * DIFF_BTN_W + 2 * 8;
        int diffStartX = COL_DX_X - totalDiffW / 2;
        for (int i = 0; i < 3; i++) {
            int dx = diffStartX + i * (DIFF_BTN_W + 8);
            boolean sel = (i == VELOCITA);
            g2.setColor(sel ? opColor : new Color(220, 225, 245));
            g2.fillRoundRect(dx, RIGA_2_Y - DIFF_BTN_H/2, DIFF_BTN_W, DIFF_BTN_H, 10, 10);
            g2.setColor(opColor);
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(dx, RIGA_2_Y - DIFF_BTN_H/2, DIFF_BTN_W, DIFF_BTN_H, 10, 10);
            g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(sel ? Color.WHITE : opColor);
            fm = g2.getFontMetrics();
            g2.drawString(DIFF_LABELS[i],
                dx + (DIFF_BTN_W - fm.stringWidth(DIFF_LABELS[i])) / 2,
                RIGA_2_Y + fm.getAscent()/2 - 2);
        }

        // ── Pulsanti di Navigazione ──
        // Indietro
        drawBtn(g2, BTN_BACK_X, BTN_Y, BTN_W, BTN_H, new Color(160, 60, 60), "◀  Indietro");
        // Avvia Gioco
        drawBtn(g2, BTN_START_X, BTN_START_Y, BTN_W, BTN_H, opColor, "Avvia Gioco  ▶");
    }

    // Metodo helper per disegnare graficamente le frecce direzionali abilita/disabilitate
    private void drawArrow(Graphics2D g2, int x, int y, String arrow, boolean enabled, Color activeColor) {
        g2.setColor(enabled ? activeColor : new Color(180, 180, 200));
        g2.fillRoundRect(x, y, ARROW_W, ARROW_H, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(arrow, x + (ARROW_W - fm.stringWidth(arrow)) / 2, y + ARROW_H/2 + fm.getAscent()/2 - 3);
    }

    private void drawBtn(Graphics2D g2, int x, int y, int w, int h, Color c, String label) {
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x+3, y+3, w, h, 12, 12);
        g2.setColor(c);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + (w - fm.stringWidth(label))/2, y + h/2 + fm.getAscent()/2 - 3);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }
}