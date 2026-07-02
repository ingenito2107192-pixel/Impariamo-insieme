import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * WaitingPanel - mostra "ATTENDI IL DOCENTE..." all'alunno
 * con un cerchio di caricamento stile Windows 7.
 * Fix: setStatus ora salva e mostra il messaggio correttamente.
 */
public class WaitingPanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    private final Runnable onBack;

    private static final int BTN_X = 300, BTN_Y = 420, BTN_W = 200, BTN_H = 44;

    private int    spinAngle  = 0;
    private Timer  spinTimer;
    private String statusMsg  = "In attesa del docente...";

    public WaitingPanel(Runnable onBack) {
        this.onBack = onBack;
        setPreferredSize(new Dimension(W, H));
        setDoubleBuffered(true);

        spinTimer = new Timer(16, e -> {
            spinAngle = (spinAngle + 6) % 360;
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (inRect(e.getX(), e.getY(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
                    stop();
                    onBack.run();
                }
            }
        });
    }

    public void start() {
        statusMsg = "Connessione al docente in corso...";
        spinTimer.start();
        repaint();
    }

    public void stop() {
        spinTimer.stop();
    }

    /** Aggiorna il messaggio di stato visibile all'alunno. */
    public void setStatus(String msg) {
        this.statusMsg = (msg != null) ? msg : "";
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Sfondo
        GradientPaint bg = new GradientPaint(0, 0, new Color(230, 240, 255), 0, H, new Color(180, 210, 255));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Spinner stile Windows 7
        int cx = W / 2;
        int cy = H / 2 - 90;
        int numDots = 8, dotRadius = 8, ringRadius = 30;
        for (int i = 0; i < numDots; i++) {
            double angle = Math.toRadians(spinAngle + i * (360.0 / numDots));
            int dx = (int)(cx + ringRadius * Math.cos(angle));
            int dy = (int)(cy + ringRadius * Math.sin(angle));
            float alpha = (float)(i + 1) / numDots;
            g2.setColor(new Color(0, 120, 215, (int)(alpha * 255)));
            int r = (int)(dotRadius * (0.5f + 0.5f * alpha));
            g2.fillOval(dx - r, dy - r, r * 2, r * 2);
        }

        // Titolo
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.setColor(new Color(50, 80, 180));
        String msg = "ATTENDI IL DOCENTE...";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2 - 20);

        // Messaggio di stato dinamico
        if (statusMsg != null && !statusMsg.isEmpty()) {
            g2.setFont(new Font("Arial", Font.PLAIN, 15));
            g2.setColor(new Color(80, 100, 160));
            fm = g2.getFontMetrics();
            g2.drawString(statusMsg, (W - fm.stringWidth(statusMsg)) / 2, H / 2 + 18);
        }

        // Bottone torna indietro
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(BTN_X + 2, BTN_Y + 2, BTN_W, BTN_H, 12, 12);
        g2.setColor(new Color(160, 60, 60));
        g2.fillRoundRect(BTN_X, BTN_Y, BTN_W, BTN_H, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        fm = g2.getFontMetrics();
        String lbl = "Torna indietro";
        g2.drawString(lbl, BTN_X + (BTN_W - fm.stringWidth(lbl)) / 2, BTN_Y + BTN_H / 2 + fm.getAscent() / 2 - 3);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
