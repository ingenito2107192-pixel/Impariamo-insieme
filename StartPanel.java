import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class StartPanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    // Centro spostato: più a sinistra (-26) e più in basso (+24)
    private static final int BTN_CX = 295;
    private static final int BTN_CY = 540;
    private static final int BTN_R  = 52;

    private final Runnable onMatematica;
    private BufferedImage bgImage = null;
    private boolean imageLoaded   = false;

    public StartPanel(Runnable onMatematica) {
        this.onMatematica = onMatematica;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setBackground(new Color(100, 180, 255));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isOnButton(e.getX(), e.getY())) {
                    onMatematica.run();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(isOnButton(e.getX(), e.getY())
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });
    }

    private boolean isOnButton(int mx, int my) {
        int dx = mx - BTN_CX;
        int dy = my - BTN_CY;
        return dx * dx + dy * dy <= BTN_R * BTN_R;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!imageLoaded) {
            imageLoaded = true;
            bgImage = ImageHelper.loadImage("s_inizio.png");
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, W, H, null);
        } else {
            g2.setColor(new Color(100, 180, 255));
            g2.fillRect(0, 0, W, H);
        }


    }
}
