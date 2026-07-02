import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class LoginPanel extends JPanel implements KeyListener {

    private static final int W = 800;
    private static final int H = 600;

    private final Runnable onStudent;
    private final Runnable onTeacher;
    private final Runnable onPrincipal;
    private final Runnable onBack;

    private BufferedImage bgImage;

    private String  nome     = "";
    private String  password = "";
    private boolean focusNome = true;
    private String  errorMsg  = "";

    private static final int FIELD_X = 230;
    private static final int FIELD_W = 340;
    private static final int NOME_Y  = 228;
    private static final int PASS_Y  = 285;
    private static final int FIELD_H = 38;
    private static final int BTN_X   = 230;
    private static final int BTN_Y   = 340;
    private static final int BTN_W   = 340;
    private static final int BTN_H   = 48;

    // Bottone "Torna Indietro" in alto a sinistra
    private static final int BACK_X  = 15;
    private static final int BACK_Y  = 15;
    private static final int BACK_W  = 160;
    private static final int BACK_H  = 40;

    public LoginPanel(Runnable onStudent, Runnable onTeacher, Runnable onPrincipal, Runnable onBack) {
        this.onStudent   = onStudent;
        this.onTeacher   = onTeacher;
        this.onPrincipal = onPrincipal;
        this.onBack      = onBack;
        init();
    }

    public LoginPanel(Runnable onStudent, Runnable onTeacher, Runnable onPrincipal) {
        this(onStudent, onTeacher, onPrincipal, null);
    }

    public LoginPanel(Runnable onStudent, Runnable onTeacher) {
        this(onStudent, onTeacher, null, null);
    }

    private void init() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
            @Override public void mouseMoved(MouseEvent e)   { }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setCursor(isOnBack(e.getX(), e.getY())
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });
        bgImage = ImageHelper.loadImage("schermata_login.jpeg");
    }

    private boolean isOnBack(int mx, int my) {
        return onBack != null
            && mx >= BACK_X && mx <= BACK_X + BACK_W
            && my >= BACK_Y && my <= BACK_Y + BACK_H;
    }

    public void reset() {
        nome = ""; password = ""; focusNome = true; errorMsg = "";
        repaint();
    }

    /** Restituisce il nome dell'utente che ha appena fatto login. */
    public String getNomeLoggato() { return nome.trim(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, W, H, null);
        } else {
            GradientPaint bg = new GradientPaint(0,0,new Color(200,230,180),0,H,new Color(150,200,255));
            g2.setPaint(bg); g2.fillRect(0,0,W,H);
        }

        // Bottone "← Torna Indietro" visibile in alto a sinistra
        if (onBack != null) {
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRoundRect(BACK_X + 2, BACK_Y + 3, BACK_W, BACK_H, 20, 20);
            g2.setColor(new Color(255, 255, 255, 210));
            g2.fillRoundRect(BACK_X, BACK_Y, BACK_W, BACK_H, 20, 20);
            g2.setColor(new Color(80, 50, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(BACK_X, BACK_Y, BACK_W, BACK_H, 20, 20);
            g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.setColor(new Color(80, 50, 180));
            g2.drawString("← Torna Indietro", BACK_X + 14, BACK_Y + BACK_H / 2 + 6);
        }

        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(195,130,410,310,30,30);

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(new Color(50,40,120));
        String titolo = "Impariamo la Matematica!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(titolo, (W-fm.stringWidth(titolo))/2, 175);

        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(new Color(120,110,170));
        String hint = "Accedi come alunno, docente o preside";
        fm = g2.getFontMetrics();
        g2.drawString(hint, (W-fm.stringWidth(hint))/2, 198);

        drawField(g2, FIELD_X, NOME_Y - FIELD_H/2, FIELD_W, FIELD_H, nome, "Nome", focusNome);
        drawField(g2, FIELD_X, PASS_Y - FIELD_H/2, FIELD_W, FIELD_H,
                  "*".repeat(password.length()), "Password", !focusNome);

        g2.setColor(new Color(80,50,180));
        g2.fillRoundRect(BTN_X, BTN_Y, BTN_W, BTN_H, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        fm = g2.getFontMetrics();
        g2.drawString("Accedi", BTN_X+(BTN_W-fm.stringWidth("Accedi"))/2,
                      BTN_Y+BTN_H/2+fm.getAscent()/2-3);

        if (!errorMsg.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(200,50,50));
            fm = g2.getFontMetrics();
            g2.drawString(errorMsg, (W-fm.stringWidth(errorMsg))/2, BTN_Y+BTN_H+22);
        }
    }

    private void drawField(Graphics2D g2, int x, int y, int w, int h,
                            String value, String placeholder, boolean focused) {
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x,y,w,h,10,10);
        g2.setColor(focused ? new Color(80,50,180) : new Color(200,200,220));
        g2.setStroke(new BasicStroke(focused ? 2f : 1.5f));
        g2.drawRoundRect(x,y,w,h,10,10);
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        if (value.isEmpty()) {
            g2.setColor(new Color(180,180,200));
            g2.drawString(placeholder, x+14, y+h/2+5);
        } else {
            g2.setColor(new Color(40,40,80));
            g2.drawString(value, x+14, y+h/2+5);
        }
        if (focused) {
            FontMetrics fm = g2.getFontMetrics();
            int cx = x+14+fm.stringWidth(value);
            g2.setColor(new Color(80,50,180));
            g2.fillRect(cx, y+8, 2, h-16);
        }
    }

    private void handleClick(int mx, int my) {
        if (isOnBack(mx, my)) { onBack.run(); return; }
        if (mx>=FIELD_X && mx<=FIELD_X+FIELD_W && my>=NOME_Y-FIELD_H/2 && my<=NOME_Y+FIELD_H/2) {
            focusNome = true; errorMsg = ""; repaint(); return;
        }
        if (mx>=FIELD_X && mx<=FIELD_X+FIELD_W && my>=PASS_Y-FIELD_H/2 && my<=PASS_Y+FIELD_H/2) {
            focusNome = false; errorMsg = ""; repaint(); return;
        }
        if (mx>=BTN_X && mx<=BTN_X+BTN_W && my>=BTN_Y && my<=BTN_Y+BTN_H) {
            doLogin();
        }
    }

    private void doLogin() {
        if (nome.trim().isEmpty())  { errorMsg = "Inserisci il tuo nome!"; repaint(); return; }
        if (password.isEmpty())     { errorMsg = "Inserisci la password!"; repaint(); return; }
        User utente = UserStore.get().autentica(nome, password);
        if (utente == null) {
            errorMsg = "Nome o password errati!"; repaint(); return;
        }
        errorMsg = "";
        switch (utente.getRuolo()) {
            case PRESIDE: if (onPrincipal != null) onPrincipal.run(); break;
            case DOCENTE: onTeacher.run(); break;
            case ALUNNO:  onStudent.run(); break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c == '\t') { focusNome = !focusNome; repaint(); return; }
        if (c == '\n') { doLogin(); return; }
        if (c == '\b') return;
        if (focusNome) { if (nome.length() < 30) nome += c; }
        else           { if (password.length() < 20) password += c; }
        errorMsg = ""; repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (focusNome) { if (!nome.isEmpty()) nome = nome.substring(0, nome.length()-1); }
            else           { if (!password.isEmpty()) password = password.substring(0, password.length()-1); }
            repaint();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
}
