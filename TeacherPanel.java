import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * TeacherPanel
 *
 * Pannello docente protetto da PIN.
 * Il docente configura: operazione, difficolta', durata e numero di vite.
 *
 * Flusso:
 *   1. Il docente clicca "Pannello Docente" nel menu principale
 *   2. Appare la schermata PIN
 *   3. Dopo il PIN corretto, appare la schermata di configurazione
 *   4. Il docente salva -> si torna al menu -> il bambino gioca
 *
 * Per aggiungere un nuovo parametro configurabile:
 *   1. Aggiungi il campo in GameConfig
 *   2. Aggiungi il widget corrispondente in drawConfig() / handleConfigClick()
 */
public class TeacherPanel extends JPanel implements MouseListener, KeyListener {

    private static final int W = 800;
    private static final int H = 600;

    // PIN di accesso (modificabile qui)
    private static final String CORRECT_PIN = "1234";

    // Sotto-schermate: "pin" | "config"
    private String subScreen = "pin";

    // Stato inserimento PIN
    private StringBuilder pinInput = new StringBuilder();
    private boolean       pinError = false;
    private int           pinErrorTimer = 0;

    // Callback per tornare al menu principale
    private Runnable onBack;

    // Copia locale della config (si salva solo al click "Salva")
    private int localOperation  = 0;
    private int localDifficulty = 0;
    private int localDuration   = 60;
    private int localLives      = 3;
    private boolean localFilastrocca = true;

    // Costruttore
    public TeacherPanel(Runnable onBack) {
        this.onBack = onBack;
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(240, 245, 255));
        addMouseListener(this);
        setFocusable(true);
        addKeyListener(this);
    }

    /** Mostra il pannello resettando allo schermo PIN */
    public void show() {
        subScreen  = "pin";
        pinInput   = new StringBuilder();
        pinError   = false;
        // Legge la config corrente per pre-popolare i valori
        GameConfig cfg   = GameConfig.get();
        localOperation   = cfg.getOperation();
        localDifficulty  = cfg.getDifficulty();
        localDuration    = cfg.getSessionDuration();
        localLives       = cfg.getInitialLives();
        localFilastrocca = cfg.isShowFilastrocca();
        requestFocusInWindow();
        repaint();
    }

    // 
    // Disegno
    // 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (subScreen.equals("pin"))    drawPin(g2);
        else                            drawConfig(g2);
    }

    // 
    // Schermata PIN
    // 
    private void drawPin(Graphics2D g) {
        // Sfondo gradiente
        GradientPaint bg = new GradientPaint(0, 0, new Color(220, 230, 255), 0, H, new Color(190, 210, 245));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        // Icona e titolo
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(new Color(50, 60, 130));
        drawCentered(g, "Pannello Docente", 100);

        g.setFont(new Font("Arial", Font.PLAIN, 17));
        g.setColor(new Color(100, 110, 150));
        drawCentered(g, "Inserisci il PIN per accedere alle impostazioni", 140);

        // Box PIN
        int bw = 260, bh = 55;
        int bx = (W - bw) / 2, by = 175;
        g.setColor(Color.WHITE);
        g.fillRoundRect(bx, by, bw, bh, 12, 12);
        g.setColor(pinError ? new Color(220, 60, 60) : new Color(100, 120, 200));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(bx, by, bw, bh, 12, 12);
        g.setStroke(new BasicStroke(1f));

        // Pallini PIN (nasconde i caratteri)
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(60, 70, 140));
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < pinInput.length(); i++) dots.append("* ");
        FontMetrics fm = g.getFontMetrics();
        g.drawString(dots.toString().trim(), bx + (bw - fm.stringWidth(dots.toString().trim())) / 2, by + 37);

        // Messaggio errore
        if (pinError) {
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.setColor(new Color(200, 50, 50));
            drawCentered(g, "PIN errato. Riprova.", 255);
        }

        // Tastierino numerico
        drawNumpad(g, 270, 275);

        // Pulsante indietro
        drawButton(g, 310, 530, 180, 45, new Color(150, 160, 190), "Indietro");
    }

    /** Disegna il tastierino numerico (1-9, 0, cancella) */
    private void drawNumpad(Graphics2D g, int startX, int startY) {
        int btnW = 70, btnH = 55, gap = 10;
        String[] labels = { "1","2","3","4","5","6","7","8","9","<","0","OK" };
        Color[] colors  = {
            new Color(100,130,200), new Color(100,130,200), new Color(100,130,200),
            new Color(100,130,200), new Color(100,130,200), new Color(100,130,200),
            new Color(100,130,200), new Color(100,130,200), new Color(100,130,200),
            new Color(200,100,100), new Color(100,130,200), new Color(60,160,80)
        };
        for (int i = 0; i < 12; i++) {
            int col = i % 3, row = i / 3;
            int x = startX + col * (btnW + gap);
            int y = startY + row * (btnH + gap);
            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(x + 3, y + 3, btnW, btnH, 10, 10);
            g.setColor(colors[i]);
            g.fillRoundRect(x, y, btnW, btnH, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(labels[i], x + (btnW - fm.stringWidth(labels[i])) / 2, y + btnH / 2 + fm.getAscent() / 2 - 3);
        }
    }

    // 
    // Schermata CONFIGURAZIONE
    // 
    private void drawConfig(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(220, 230, 255), 0, H, new Color(190, 210, 245));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        // Titolo
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(50, 60, 130));
        drawCentered(g, "Impostazioni Sessione", 45);

        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(new Color(100, 110, 150));
        drawCentered(g, "Configura la sessione per la classe", 70);

        int startY = 100;
        int rowH   = 100;

        //  Operazione 
        drawSectionLabel(g, "Operazione matematica", 30, startY);
        for (int i = 0; i < 4; i++) {
            boolean sel = (localOperation == i);
            drawChoiceButton(g, 30 + i * 187, startY + 28, 175, 50,
                    GameConfig.OP_SIMBOLI[i] + " " + GameConfig.OP_NOMI[i], sel,
                    getOpColor(i));
        }

        // --- Difficolta' iniziale ---
        startY += rowH;
        drawSectionLabel(g, "Difficolta' iniziale", 30, startY);
        String[] diffs = { "Facile (1-6)", "Medio (1-12)", "Difficile (1-20)" };
        for (int i = 0; i < 3; i++) {
            boolean sel = (localDifficulty == i);
            drawChoiceButton(g, 30 + i * 253, startY + 28, 240, 50, diffs[i], sel, new Color(80, 120, 200));
        }

        // Durata sessione 
        startY += rowH;
        drawSectionLabel(g, "Durata sessione (secondi)", 30, startY);
        int[] durate = { 30, 45, 60, 90, 120 };
        for (int i = 0; i < durate.length; i++) {
            boolean sel = (localDuration == durate[i]);
            drawChoiceButton(g, 30 + i * 150, startY + 28, 138, 50, durate[i] + "s", sel, new Color(60, 140, 160));
        }

        // Numero vite 
        startY += rowH;
        drawSectionLabel(g, "Numero di vite", 30, startY);
        int[] viteOpts = { 1, 2, 3, 4, 5 };
        for (int i = 0; i < viteOpts.length; i++) {
            boolean sel = (localLives == viteOpts[i]);
            String lbl  = viteOpts[i] + (viteOpts[i] == 1 ? " vita" : " vite");
            drawChoiceButton(g, 30 + i * 148, startY + 28, 136, 50, lbl, sel, new Color(180, 80, 80));
        }

        // Filastrocca 
        startY += rowH;
        drawSectionLabel(g, "Mostra filastrocca introduttiva", 30, startY);
        drawChoiceButton(g, 30,  startY + 28, 150, 50, "Si",  localFilastrocca,  new Color(80, 170, 90));
        drawChoiceButton(g, 195, startY + 28, 150, 50, "No", !localFilastrocca, new Color(180, 80, 80));

        // Pulsanti Salva / Annulla
        drawButton(g, 490, startY + 28, 140, 50, new Color(80, 170, 90),   "Salva");
        drawButton(g, 645, startY + 28, 140, 50, new Color(150, 160, 190), "Annulla");
    }

    // 
    // Widget riutilizzabili
    // 
    private void drawSectionLabel(Graphics2D g, String label, int x, int y) {
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(new Color(70, 80, 130));
        g.drawString(label, x, y + 14);
    }

    private void drawChoiceButton(Graphics2D g, int x, int y, int w, int h,
                                   String label, boolean selected, Color baseColor) {
        if (selected) {
            g.setColor(baseColor);
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(new Color(220, 228, 245));
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(new Color(0, 0, 0, 30));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, w, h, 12, 12);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(70, 80, 130));
        }
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + fm.getAscent() / 2 - 3);
    }

    private void drawButton(Graphics2D g, int x, int y, int w, int h, Color c, String label) {
        g.setColor(new Color(0,0,0,25));
        g.fillRoundRect(x+3, y+3, w, h, 12, 12);
        g.setColor(c);
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + fm.getAscent() / 2 - 3);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    private Color getOpColor(int op) {
        switch (op) {
            case 0: return new Color(80,  170, 90);
            case 1: return new Color(200, 80,  80);
            case 2: return new Color(70,  120, 200);
            default:return new Color(200, 150, 40);
        }
    }

    // 
    // Gestione click
    // 
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (subScreen.equals("pin")) {
            handlePinClick(mx, my);
        } else {
            handleConfigClick(mx, my);
        }
    }

    private void handlePinClick(int mx, int my) {
        // Pulsante indietro
        if (mx >= 310 && mx <= 490 && my >= 530 && my <= 575) {
            onBack.run(); return;
        }

        // Tastierino
        int startX = 270, startY = 275;
        int btnW = 70, btnH = 55, gap = 10;
        String[] labels = { "1","2","3","4","5","6","7","8","9","<","0","OK" };
        for (int i = 0; i < 12; i++) {
            int col = i % 3, row = i / 3;
            int x = startX + col * (btnW + gap);
            int y = startY + row * (btnH + gap);
            if (mx >= x && mx <= x + btnW && my >= y && my <= y + btnH) {
                String lbl = labels[i];
                if (lbl.equals("<")) {
                    if (pinInput.length() > 0) pinInput.deleteCharAt(pinInput.length() - 1);
                } else if (lbl.equals("OK")) {
                    checkPin();
                } else {
                    if (pinInput.length() < 8) pinInput.append(lbl);
                }
                pinError = false;
                repaint();
                return;
            }
        }
    }

    private void checkPin() {
        if (pinInput.toString().equals(CORRECT_PIN)) {
            subScreen = "config";
            pinError  = false;
        } else {
            pinError      = true;
            pinErrorTimer = 60;
            pinInput      = new StringBuilder();
        }
        repaint();
    }

    private void handleConfigClick(int mx, int my) {
        int startY = 100;
        int rowH   = 100;

        // --- Operazione ---
        for (int i = 0; i < 4; i++) {
            int x = 30 + i * 187, y = startY + 28;
            if (inRect(mx, my, x, y, 175, 50)) { localOperation = i; repaint(); return; }
        }

        //  Difficolta'
        startY += rowH;
        for (int i = 0; i < 3; i++) {
            if (inRect(mx, my, 30 + i * 253, startY + 28, 240, 50)) { localDifficulty = i; repaint(); return; }
        }

        //  Durata 
        startY += rowH;
        int[] durate = { 30, 45, 60, 90, 120 };
        for (int i = 0; i < durate.length; i++) {
            if (inRect(mx, my, 30 + i * 150, startY + 28, 138, 50)) { localDuration = durate[i]; repaint(); return; }
        }

        // Vite 
        startY += rowH;
        int[] viteOpts = { 1, 2, 3, 4, 5 };
        for (int i = 0; i < viteOpts.length; i++) {
            if (inRect(mx, my, 30 + i * 148, startY + 28, 136, 50)) { localLives = viteOpts[i]; repaint(); return; }
        }

        //  Filastrocca 
        startY += rowH;
        if (inRect(mx, my, 30,  startY + 28, 150, 50)) { localFilastrocca = true;  repaint(); return; }
        if (inRect(mx, my, 195, startY + 28, 150, 50)) { localFilastrocca = false; repaint(); return; }

        //  Salva 
        if (inRect(mx, my, 490, startY + 28, 140, 50)) {
            saveConfig();
            onBack.run();
            return;
        }

        //  Annulla 
        if (inRect(mx, my, 645, startY + 28, 140, 50)) {
            onBack.run();
        }
    }

    /** Salva la configurazione locale nel singleton GameConfig */
    private void saveConfig() {
        GameConfig cfg = GameConfig.get();
        cfg.setOperation(localOperation);
        cfg.setDifficulty(localDifficulty);
        cfg.setSessionDuration(localDuration);
        cfg.setInitialLives(localLives);
        cfg.setShowFilastrocca(localFilastrocca);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }


    // KeyListener (digitazione PIN da tastiera fisica)
    @Override
    public void keyTyped(KeyEvent e) {
        if (!subScreen.equals("pin")) return;
        char c = e.getKeyChar();
        if (Character.isDigit(c) && pinInput.length() < 8) {
            pinInput.append(c);
            pinError = false;
            repaint();
        } else if (c == '\n') {
            checkPin();
        }
    }
    @Override public void keyPressed(KeyEvent e) {
        if (!subScreen.equals("pin")) return;
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && pinInput.length() > 0) {
            pinInput.deleteCharAt(pinInput.length() - 1);
            repaint();
        }
    }
    @Override public void keyReleased(KeyEvent e) {}

    // MouseListener obbligatori
    public void mouseClicked(MouseEvent e)  {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e)  {}
    public void mouseExited(MouseEvent e)   {}
}
