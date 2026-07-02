import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * ExtraLevelsPanel
 * ----------------
 * Schermata "Livelli Extra" per l'alunno.
 * Mostra 4 pulsanti operazione (addizione, sottrazione, moltiplicazione, divisione).
 * Al click si avvia direttamente il gioco autonomo senza bisogno del docente.
 */
public class ExtraLevelsPanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    private final Runnable onBack;
    private final java.util.function.Consumer<Integer> onStartGame;

    // Hover stati
    private int hoverOp = -1;

    private static final int BTN_W = 320, BTN_H = 80;
    private static final int COL1_X = 80, COL2_X = 400;
    private static final int ROW1_Y = 200, ROW2_Y = 310;

    private static final String[] OP_NOMI   = { "Addizione", "Sottrazione", "Moltiplicazione", "Divisione" };
    private static final String[] OP_SIMBOLI= { "+", "−", "×", "÷" };
    private static final Color[]  OP_COLORI = {
        new Color(50,  160, 200),
        new Color(30,  160,  60),
        new Color(140,  50, 190),
        new Color(220, 110,  20)
    };

    private static final int BTN_BACK_X = 310, BTN_BACK_Y = 520, BTN_BACK_W = 180, BTN_BACK_H = 40;

    public ExtraLevelsPanel(Runnable onBack, java.util.function.Consumer<Integer> onStartGame) {
        this.onBack      = onBack;
        this.onStartGame = onStartGame;

        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);

        // mousePressed è più affidabile di mouseClicked (che Swing annulla se il mouse si muove)
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e.getX(), e.getY()); }
        });

        // ESC → torna alla Home
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && onBack != null) {
                    onBack.run();
                }
            }
        });
    }

    private int[] opX = { COL1_X, COL2_X, COL1_X, COL2_X };
    private int[] opY = { ROW1_Y, ROW1_Y, ROW2_Y, ROW2_Y };

    private void handleClick(int mx, int my) {
        for (int i = 0; i < 4; i++) {
            if (inRect(mx,my,opX[i],opY[i],BTN_W,BTN_H)) { onStartGame.accept(i); return; }
        }
        if (inRect(mx,my,BTN_BACK_X,BTN_BACK_Y,BTN_BACK_W,BTN_BACK_H)) { onBack.run(); }
    }

    private void updateHover(int mx, int my) {
        int prev = hoverOp;
        hoverOp = -1;
        for (int i = 0; i < 4; i++) {
            if (inRect(mx,my,opX[i],opY[i],BTN_W,BTN_H)) { hoverOp = i; break; }
        }
        if (hoverOp != prev) {
            setCursor(Cursor.getPredefinedCursor(hoverOp >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Sfondo
        GradientPaint bg = new GradientPaint(0,0,new Color(245,240,255),0,H,new Color(220,210,250));
        g2.setPaint(bg); g2.fillRect(0,0,W,H);

        // Titolo
        g2.setFont(new Font("Arial", Font.BOLD, 34));
        g2.setColor(new Color(70,30,130));
        drawCentered(g2, "Livelli Extra", 80);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(new Color(110,80,170));
        drawCentered(g2, "Scegli un'operazione e gioca!", 115);

        // Pulsanti operazione
        for (int i = 0; i < 4; i++) {
            boolean hover = (hoverOp == i);
            drawOpButton(g2, opX[i], opY[i], BTN_W, BTN_H, i, hover);
        }

        // Pulsante indietro
        g2.setColor(new Color(0,0,0,20)); g2.fillRoundRect(BTN_BACK_X+2,BTN_BACK_Y+2,BTN_BACK_W,BTN_BACK_H,12,12);
        g2.setColor(new Color(140,130,180)); g2.fillRoundRect(BTN_BACK_X,BTN_BACK_Y,BTN_BACK_W,BTN_BACK_H,12,12);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("← Indietro", BTN_BACK_X+(BTN_BACK_W-fm.stringWidth("← Indietro"))/2, BTN_BACK_Y+BTN_BACK_H/2+fm.getAscent()/2-3);
    }

    private void drawOpButton(Graphics2D g2, int x, int y, int w, int h, int op, boolean hover) {
        Color c = OP_COLORI[op];
        Color lighter = c.brighter();

        // Ombra
        g2.setColor(new Color(0,0,0,30)); g2.fillRoundRect(x+3,y+3,w,h,18,18);

        // Sfondo
        GradientPaint gp = new GradientPaint(x, y, hover ? lighter : c, x, y+h, hover ? c : c.darker());
        g2.setPaint(gp); g2.fillRoundRect(x,y,w,h,18,18);

        // Bordo
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2.5f)); g2.drawRoundRect(x,y,w,h,18,18); g2.setStroke(new BasicStroke(1f));

        // Simbolo grande a sinistra
        g2.setFont(new Font("Arial", Font.BOLD, 46));
        g2.setColor(new Color(255,255,255,200));
        g2.drawString(OP_SIMBOLI[op], x + 24, y + h/2 + 16);

        // Nome operazione
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(OP_NOMI[op], x + 80, y + h/2 + fm.getAscent()/2 - 3);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (W - fm.stringWidth(text))/2, y);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx>=x && mx<=x+w && my>=y && my<=y+h;
    }
}
