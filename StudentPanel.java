import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * StudentPanel - schermata dell'alunno.
 * Al click su Play avvia il GameClient e mostra la WaitingPanel.
 * Aggiunto pulsante grande Play centrale e pulsante "LIVELLI EXTRA" in basso a destra.
 */
public class StudentPanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    private final Runnable onPlayClicked;
    private final Runnable onGameStart;
    private final WaitingPanel waitingPanel;
    private final Runnable onLogout;
    private BufferedImage bgImage;


    // Coordinate pulsante Logout (in alto a destra)
    private static final int BTN_OUT_X = 620, BTN_OUT_Y = 10, BTN_OUT_W = 165, BTN_OUT_H = 36;


    // Pulsante PLAY - grande, centrale
    private static final int PLAY_CX = 400;
    private static final int PLAY_CY = 380;
    private static final int PLAY_R  = 60;

    // Pulsante LIVELLI EXTRA - in basso, spostato a destra
    private static final int EXTRA_X = 580;
    private static final int EXTRA_Y = 510;
    private static final int EXTRA_W = 165;
    private static final int EXTRA_H = 55;

    // Hover state
    private boolean hoverPlay  = false;
    private boolean hoverExtra = false;

    private final Runnable onLivelliExtra;
    private boolean sessioneCompletata = false;

    public void setSessioneCompletata() {
        sessioneCompletata = true;
        repaint();
    }

    public void reset() {
        sessioneCompletata = false;
        repaint();
    }

    public StudentPanel(WaitingPanel waitingPanel, Runnable onPlayClicked, Runnable onGameStart, Runnable onLogout, Runnable onLivelliExtra) {
        this.waitingPanel  = waitingPanel;
        this.onPlayClicked = onPlayClicked;
        this.onGameStart   = onGameStart;
        this.onLogout    = onLogout;
        this.onLivelliExtra = onLivelliExtra;

        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);
        bgImage = ImageHelper.loadImage("schermata alunno.png");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                // Click sul pulsante Play
                int dx = mx - PLAY_CX;
                int dy = my - PLAY_CY;
                if (dx*dx + dy*dy <= (PLAY_R + 10) * (PLAY_R + 10)) {
                    handlePlay();
                    return;
                }
                // Click su LIVELLI EXTRA
                if (inRect(mx, my, EXTRA_X, EXTRA_Y, EXTRA_W, EXTRA_H)) {
                    handleLivelliExtra();
                }
                // Click su Esci
                if (mx >= BTN_OUT_X && mx <= BTN_OUT_X + BTN_OUT_W && my >= BTN_OUT_Y && my <= BTN_OUT_Y + BTN_OUT_H) {
                    onLogout.run();
                    return;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                int dx = mx - PLAY_CX, dy = my - PLAY_CY;
                boolean np = dx*dx + dy*dy <= (PLAY_R + 10) * (PLAY_R + 10);
                boolean ne = inRect(mx, my, EXTRA_X, EXTRA_Y, EXTRA_W, EXTRA_H);
                if (np != hoverPlay || ne != hoverExtra) {
                    hoverPlay  = np;
                    hoverExtra = ne;
                    setCursor(Cursor.getPredefinedCursor(
                        (np || ne) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        });
    }

    private void handlePlay() {
        if (sessioneCompletata) return;
        onPlayClicked.run();
    }

    private void handleLivelliExtra() {
        if (onLivelliExtra != null) onLivelliExtra.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Sfondo
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, W, H, null);
        } else {
            GradientPaint bg = new GradientPaint(0, 0, new Color(100, 180, 255), 0, H, new Color(60, 220, 120));
            g2.setPaint(bg); g2.fillRect(0, 0, W, H);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(Color.WHITE);
            drawCentered(g2, "IMPARIAMO LA MATEMATICA", 200);
        }

        drawPlayButton(g2);
        drawLivelliExtraButton(g2);
        
        // Pulsante Logout (in alto a destra)
        drawBtn(g2, BTN_OUT_X, BTN_OUT_Y, BTN_OUT_W, BTN_OUT_H, new Color(180, 60, 60), "Esci");
    }

    private void drawBtn(Graphics2D g2, int x, int y, int w, int h, Color c, String label) {
        g2.setColor(new Color(0, 0, 0, 25)); g2.fillRoundRect(x+2, y+2, w, h, 12, 12);
        g2.setColor(c); g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h/2 + fm.getAscent()/2 - 3);
    }

    private void drawPlayButton(Graphics2D g2) {
        int r = hoverPlay ? PLAY_R + 4 : PLAY_R;

        if (sessioneCompletata) {
            // Pulsante grigio disabilitato
            g2.setColor(new Color(0,0,0,60));
            g2.fillOval(PLAY_CX - r + 5, PLAY_CY - r + 6, r*2, r*2);
            g2.setColor(Color.WHITE);
            g2.fillOval(PLAY_CX - r - 8, PLAY_CY - r - 8, (r+8)*2, (r+8)*2);
            g2.setColor(new Color(160,160,160));
            g2.fillOval(PLAY_CX - r, PLAY_CY - r, r*2, r*2);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(80,80,80));
            FontMetrics fm = g2.getFontMetrics();
            String msg1 = "Sessione";
            String msg2 = "completata";
            g2.drawString(msg1, PLAY_CX - fm.stringWidth(msg1)/2, PLAY_CY - 8);
            g2.drawString(msg2, PLAY_CX - fm.stringWidth(msg2)/2, PLAY_CY + 10);
            return;
        }

        // OMBRA ESTERNA 
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval(PLAY_CX - r + 7, PLAY_CY - r + 8, r * 2, r * 2);

        // BORDO BIANCO ESTERNO SPESSO 
        int borderThick = 8;
        g2.setColor(Color.WHITE);
        g2.fillOval(PLAY_CX - r - borderThick, PLAY_CY - r - borderThick,
                    (r + borderThick) * 2, (r + borderThick) * 2);

        // CERCHIO VERDE CON GRADIENTE 
        Color greenTop    = hoverPlay ? new Color(100, 240, 60)  : new Color(80, 220, 40);
        Color greenBottom = hoverPlay ? new Color(15, 140, 10)   : new Color(10, 120, 5);
        GradientPaint gp = new GradientPaint(
            PLAY_CX, PLAY_CY - r, greenTop,
            PLAY_CX, PLAY_CY + r, greenBottom);
        g2.setPaint(gp);
        g2.fillOval(PLAY_CX - r, PLAY_CY - r, r * 2, r * 2);

        // RIFLESSO LUCIDO IN ALTO (effetto glossy) 
        int hW = (int)(r * 1.1);
        int hH = (int)(r * 0.55);
        GradientPaint highlight = new GradientPaint(
            PLAY_CX, PLAY_CY - r,      new Color(255, 255, 255, 160),
            PLAY_CX, PLAY_CY - r + hH, new Color(255, 255, 255, 0));
        g2.setPaint(highlight);
        Shape oldClip = g2.getClip();
        g2.setClip(new java.awt.geom.Ellipse2D.Float(PLAY_CX - r, PLAY_CY - r, r * 2, r * 2));
        g2.fillOval(PLAY_CX - hW / 2, PLAY_CY - r, hW, hH);
        g2.setClip(oldClip);

        // BORDO SCURO SUL CERCHIO VERDE 
        g2.setColor(new Color(0, 90, 0, 120));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(PLAY_CX - r, PLAY_CY - r, r * 2, r * 2);
        g2.setStroke(new BasicStroke(1f));

        // TRIANGOLO PLAY BIANCO BEN CENTRATO 
        int tw = (int)(r * 0.42);
        int th = (int)(r * 0.52);
        int ox = (int)(r * 0.10);
        int[] px = { PLAY_CX - tw + ox, PLAY_CX + tw + ox, PLAY_CX - tw + ox };
        int[] py = { PLAY_CY - th,       PLAY_CY,            PLAY_CY + th };
        // Ombra del triangolo
        g2.setColor(new Color(0, 80, 0, 80));
        int[] pxs = { px[0]+2, px[1]+2, px[2]+2 };
        int[] pys = { py[0]+2, py[1]+2, py[2]+2 };
        g2.fillPolygon(pxs, pys, 3);
        // Triangolo bianco
        g2.setColor(Color.WHITE);
        g2.fillPolygon(px, py, 3);
    }

    private void drawLivelliExtraButton(Graphics2D g2) {
        int x = EXTRA_X, y = EXTRA_Y, w = EXTRA_W, h = EXTRA_H;

        // Ombra
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 3, y + 3, w, h, 18, 18);

        // Sfondo azzurro metallico (stile palloncino)
        Color topColor = hoverExtra ? new Color(100, 160, 255) : new Color(70, 130, 230);
        Color botColor = hoverExtra ? new Color(30, 80, 180)   : new Color(20, 60, 160);
        GradientPaint gp = new GradientPaint(x, y, topColor, x, y + h, botColor);
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 18, 18);

        // Bordo bianco
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x, y, w, h, 18, 18);
        g2.setStroke(new BasicStroke(1f));

        // Riflesso lucido
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(x + 6, y + 4, w - 12, h / 3, 12, 12);

        // Testo su due righe
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String line1 = "LIVELLI";
        String line2 = "EXTRA";
        int totalH = fm.getAscent() * 2 + 4;
        int startY = y + (h - totalH) / 2 + fm.getAscent();
        g2.drawString(line1, x + (w - fm.stringWidth(line1)) / 2, startY);
        g2.drawString(line2, x + (w - fm.stringWidth(line2)) / 2, startY + fm.getAscent() + 4);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
