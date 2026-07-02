import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public class MonitorPanel extends JPanel {

    private static final int W = 800, H = 600;
    private static final int BTN_H       = 42;
    private static final int BTN_Y       = H - BTN_H - 10;   // 548
    private static final int BTN_W       = 200;
    private static final int BTN_START_W = 200;
    private static final int BTN_START_H = 42;
    private static final int BTN_START_X = 180;   // W/2 - BTN_START_W - 20 = 400-200-20
    private static final int BTN_STOP_X  = 420;   // W/2 + 20 = 400+20
    private static final int BTN_X       = 420;
    private static final int HEADER_H    = 68;
    private static final int COLS        = 4;
    private static final int CARD_W = 170, CARD_H = 116;
    private static final int CARD_GAP    = 8;
    private static final int EX_W = 720,  EX_H = 500;

    private final Runnable onStop;
    private List<GameServer.AlunnoStatus> alunni = new ArrayList<>();
    private String nomeDocente = "";
    private int alunnoEspanso = -1;
    private int livePulse = 0;
    private GameServer gameServer = null;
    private boolean gameStarted = false;
    private Runnable onStart = null;

    private final Timer mainTimer;
    private final Timer pollTimer;

    public MonitorPanel(Runnable onStop) {
        this.onStop = onStop;
        setPreferredSize(new Dimension(W, H));
        setDoubleBuffered(true);

        mainTimer = new Timer(50, e -> {
            livePulse = (livePulse + 1) % 8;
            repaint();
        });
        mainTimer.setRepeats(true);

        pollTimer = new Timer(80, e -> {
            if (gameServer != null) alunni = gameServer.getAllStatus();
        });
        pollTimer.setRepeats(true);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
    }

    private void handleClick(int mx, int my) {
        if (alunnoEspanso >= 0) { alunnoEspanso = -1; repaint(); return; }
        // Pulsante AVVIA
        if (!gameStarted && inRect(mx, my, BTN_START_X, BTN_Y, BTN_START_W, BTN_START_H)) {
            if (onStart != null) onStart.run();
            gameStarted = true;
            repaint();
            return;
        }
        // Pulsante FINE SESSIONE
        if (inRect(mx, my, BTN_STOP_X, BTN_Y, BTN_W, BTN_H)) { ferma(); onStop.run(); return; }
        for (int i = 0; i < alunni.size(); i++)
            if (cardRect(i).contains(mx, my)) { alunnoEspanso = i; repaint(); return; }
    }

    public void avvia()  { mainTimer.start(); pollTimer.start(); repaint(); }
    public void ferma()  { mainTimer.stop();  pollTimer.stop(); }
    public void setGameServer(GameServer server) { this.gameServer = server; }
    public void aggiornaAlunni(List<GameServer.AlunnoStatus> nuovi) { alunni = new ArrayList<>(nuovi); }
    public void setNomeDocente(String nome) { this.nomeDocente = nome; }
    public void setOnStart(Runnable r) { this.onStart = r; }
    public void resetStarted() { gameStarted = false; }

    private Rectangle cardRect(int i) {
        int col = i % COLS, row = i / COLS;
        int totalW = COLS * CARD_W + (COLS-1) * CARD_GAP;
        int startX = (W - totalW) / 2;
        return new Rectangle(startX + col*(CARD_W+CARD_GAP), HEADER_H+8 + row*(CARD_H+CARD_GAP), CARD_W, CARD_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawMonitorBg(g2);
        drawHeader(g2);
        drawGrid(g2);
        drawFooter(g2);
        if (alunnoEspanso >= 0 && alunnoEspanso < alunni.size())
            drawExpanded(g2, alunni.get(alunnoEspanso));
    }

    private void drawMonitorBg(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(14,20,44),0,H,new Color(24,34,66)));
        g.fillRect(0,0,W,H);
        g.setColor(new Color(255,255,255,7));
        for (int x=0;x<W;x+=40) g.drawLine(x,0,x,H);
        for (int y=0;y<H;y+=40) g.drawLine(0,y,W,y);
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(10,18,55,220));
        g.fillRect(0,0,W,HEADER_H);
        g.setColor(new Color(60,90,220,100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(0,HEADER_H,W,HEADER_H);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Arial",Font.BOLD,30));
        g.setColor(Color.WHITE);
        g.drawString("MONITOR",22,42);

        FontMetrics fm = g.getFontMetrics();
        int lx = 22+fm.stringWidth("MONITOR")+12;
        boolean pulse = livePulse < 4;
        if (pulse) { g.setColor(new Color(60,220,80,50)); g.fillOval(lx-4,28,18,18); }
        g.setColor(pulse ? new Color(60,220,80) : new Color(30,140,50));
        g.fillOval(lx,32,10,10);
        g.setFont(new Font("Arial",Font.BOLD,11));
        g.setColor(new Color(60,220,80));
        g.drawString("LIVE",lx+14,42);

        g.setFont(new Font("Arial",Font.PLAIN,12));
        g.setColor(new Color(130,160,220));
        String doc = "Docente: "+nomeDocente;
        fm = g.getFontMetrics();
        g.drawString(doc, W-fm.stringWidth(doc)-18, 26);

        String nA = alunni.isEmpty() ? "In attesa di alunni…"
                  : alunni.size()+(alunni.size()==1?" alunno":" alunni");
        g.setFont(new Font("Arial",Font.BOLD,12));
        g.setColor(new Color(100,145,255));
        fm = g.getFontMetrics();
        g.drawString(nA, W-fm.stringWidth(nA)-18, 44);

        if (!alunni.isEmpty()) {
            g.setFont(new Font("Arial",Font.ITALIC,10));
            g.setColor(new Color(70,100,160));
            fm = g.getFontMetrics();
            String hint = "Clicca su una scheda per vedere la live";
            g.drawString(hint, W-fm.stringWidth(hint)-18, 60);
        }
    }

    private void drawGrid(Graphics2D g) {
        if (alunni.isEmpty()) {
            g.setFont(new Font("Arial",Font.ITALIC,15));
            g.setColor(new Color(70,100,160));
            FontMetrics fm = g.getFontMetrics();
            String msg = "In attesa che gli alunni si connettano...";
            g.drawString(msg,(W-fm.stringWidth(msg))/2, HEADER_H+80);
            return;
        }
        for (int i=0; i<alunni.size(); i++) drawCard(g, alunni.get(i), cardRect(i));
    }

    // ── CARD PICCOLA: tempo / domande esatte / punteggio ──────────────────────
    private void drawCard(Graphics2D g, GameServer.AlunnoStatus a, Rectangle r) {
        Color accent = a.connesso ? new Color(70,130,255) : new Color(200,60,60);

        // Sfondo
        g.setColor(a.connesso ? new Color(20,32,76,235) : new Color(44,18,18,220));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        // Striscia colorata top
        g.setColor(accent);
        g.fillRect(r.x+5, r.y, r.width-10, 3);

        FontMetrics fm;

        // ── Nome alunno ──
        g.setFont(new Font("Arial",Font.BOLD,12));
        g.setColor(Color.WHITE);
        fm = g.getFontMetrics();
        String nome = truncate(a.nome, fm, r.width-12);
        g.drawString(nome, r.x+(r.width-fm.stringWidth(nome))/2, r.y+18);

        if (a.connesso) {
            // ── Punteggio (grande, al centro) ──
            g.setFont(new Font("Arial",Font.BOLD,32));
            g.setColor(new Color(90,190,255));
            fm = g.getFontMetrics();
            String sc = String.valueOf(a.score);
            g.drawString(sc, r.x+(r.width-fm.stringWidth(sc))/2, r.y+58);

            // ── Etichetta "PT" sotto il punteggio ──
            g.setFont(new Font("Arial",Font.PLAIN,9));
            g.setColor(new Color(70,120,180));
            fm = g.getFontMetrics();
            g.drawString("PT", r.x+(r.width-fm.stringWidth("PT"))/2, r.y+69);

            // ── Riga inferiore: [tempo] [✓ risposte] ──
            int bottomY = r.y + r.height - 8;

            // Tempo rimasto (sinistra basso)
            Color tc = a.tempoRimasto > 15 ? new Color(60,210,110)
                     : a.tempoRimasto > 5  ? new Color(255,195,50)
                                           : new Color(255,70,70);
            g.setFont(new Font("Arial",Font.BOLD,13));
            g.setColor(tc);
            fm = g.getFontMetrics();
            String ts = "⏱ " + a.tempoRimasto + "s";
            g.drawString(ts, r.x+6, bottomY);

            // Risposte esatte (destra basso)
            g.setFont(new Font("Arial",Font.BOLD,13));
            g.setColor(new Color(80,210,130));
            fm = g.getFontMetrics();
            String dr = "✓ " + a.domandeRisposte;
            g.drawString(dr, r.x + r.width - fm.stringWidth(dr) - 6, bottomY);

        } else {
            g.setColor(new Color(0,0,0,110));
            g.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setFont(new Font("Arial",Font.BOLD,10));
            g.setColor(new Color(200,70,70));
            fm = g.getFontMetrics();
            String off = "● OFFLINE";
            g.drawString(off, r.x+(r.width-fm.stringWidth(off))/2, r.y+r.height/2+4);
        }

        // Bordo card
        g.setColor(accent);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
        g.setStroke(new BasicStroke(1f));
    }

    // ── VISTA ESPANSA: live schermata dell'alunno ──────────────────────────────
    private void drawExpanded(Graphics2D g, GameServer.AlunnoStatus a) {
        // Overlay scuro
        g.setColor(new Color(0,0,0,210));
        g.fillRect(0,0,W,H);

        int cx = (W-EX_W)/2, cy = (H-EX_H)/2;

        // Decodifica il frame JPEG ricevuto via socket
        java.awt.image.BufferedImage frame = null;
        if (a.frameBase64 != null && !a.frameBase64.isEmpty()) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(a.frameBase64);
                frame = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            } catch (Exception ignored) {}
        }

        Shape oldClip = g.getClip();
        g.setClip(cx, cy, EX_W, EX_H);

        if (frame != null) {
            // Disegna lo schermo reale scalato
            g.drawImage(frame, cx, cy, EX_W, EX_H, null);
        } else {
            // Fallback: sfondo stellato se il frame non è ancora disponibile
            g.setPaint(new GradientPaint(cx,cy,new Color(5,10,30),cx,cy+EX_H,new Color(15,25,60)));
            g.fillRect(cx,cy,EX_W,EX_H);
            Random sr = new Random(a.nome.hashCode());
            g.setColor(new Color(255,255,255,130));
            for (int i=0;i<60;i++) {
                int sx=cx+sr.nextInt(EX_W), sy=cy+sr.nextInt(EX_H), sz=sr.nextInt(2)+1;
                g.fillOval(sx,sy,sz,sz);
            }
            if (!a.connesso) {
                g.setFont(new Font("Arial",Font.BOLD,28));
                g.setColor(new Color(200,70,70));
                FontMetrics fm = g.getFontMetrics();
                String off = a.nome+" — OFFLINE";
                g.drawString(off, cx+(EX_W-fm.stringWidth(off))/2, cy+EX_H/2);
            } else {
                g.setFont(new Font("Arial",Font.ITALIC,18));
                g.setColor(new Color(130,160,210));
                FontMetrics fm = g.getFontMetrics();
                String wait = "In attesa del primo frame...";
                g.drawString(wait, cx+(EX_W-fm.stringWidth(wait))/2, cy+EX_H/2);
            }
        }

        g.setClip(oldClip);

        // Bordo live
        g.setColor(a.connesso ? new Color(70,130,255) : new Color(200,60,60));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRect(cx,cy,EX_W,EX_H);
        g.setStroke(new BasicStroke(1f));

        // Badge LIVE sopra a sinistra
        if (a.connesso) {
            boolean pulse = livePulse < 4;
            int bx = cx + 10, by = cy + 10;
            g.setColor(new Color(0,0,0,160));
            g.fillRoundRect(bx-4, by-14, 62, 18, 6, 6);
            if (pulse) { g.setColor(new Color(220,50,50,80)); g.fillOval(bx-2, by-12, 14, 14); }
            g.setColor(pulse ? new Color(220,50,50) : new Color(140,30,30));
            g.fillOval(bx, by-10, 10, 10);
            g.setFont(new Font("Arial",Font.BOLD,11));
            g.setColor(Color.WHITE);
            g.drawString("LIVE", bx+14, by+1);
        }

        // Nome alunno sopra la finestra
        g.setFont(new Font("Arial",Font.BOLD,14));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String titolo = a.nome + (a.connesso ? " — In gioco" : " — Offline");
        g.drawString(titolo, cx+(EX_W-fm.stringWidth(titolo))/2, cy-6);

        // Hint chiudi
        g.setFont(new Font("Arial",Font.ITALIC,11));
        g.setColor(new Color(80,110,160));
        fm = g.getFontMetrics();
        String hint = "Clicca ovunque per chiudere";
        g.drawString(hint, cx+(EX_W-fm.stringWidth(hint))/2, cy+EX_H+16);
    }

    private void drawFooter(Graphics2D g) {
        g.setColor(new Color(35,55,120,120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(20,BTN_Y-8,W-20,BTN_Y-8);
        g.setStroke(new BasicStroke(1f));

        // Pulsante AVVIA (verde, sinistra) — solo se non ancora avviato
        if (!gameStarted) {
            g.setColor(new Color(0,0,0,40));
            g.fillRoundRect(BTN_START_X+3,BTN_Y+3,BTN_START_W,BTN_START_H,12,12);
            g.setColor(new Color(30,130,60));
            g.fillRoundRect(BTN_START_X,BTN_Y,BTN_START_W,BTN_START_H,12,12);
            g.setColor(new Color(255,255,255,25));
            g.fillRoundRect(BTN_START_X+4,BTN_Y+3,BTN_START_W-8,BTN_START_H/2-3,8,8);
            g.setColor(new Color(60,180,90));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(BTN_START_X,BTN_Y,BTN_START_W,BTN_START_H,12,12);
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("Arial",Font.BOLD,14));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            String lbl = "▶ Avvia partita";
            g.drawString(lbl, BTN_START_X+(BTN_START_W-fm.stringWidth(lbl))/2, BTN_Y+BTN_START_H/2+fm.getAscent()/2-3);
        } else {
            // Etichetta "In corso..." quando avviato
            g.setFont(new Font("Arial",Font.BOLD,12));
            g.setColor(new Color(60,200,90));
            FontMetrics fm = g.getFontMetrics();
            String lbl = "● Partita in corso";
            g.drawString(lbl, BTN_START_X+(BTN_START_W-fm.stringWidth(lbl))/2, BTN_Y+BTN_START_H/2+fm.getAscent()/2-3);
        }

        // Pulsante FINE SESSIONE (rosso, destra)
        g.setColor(new Color(0,0,0,40));
        g.fillRoundRect(BTN_STOP_X+3,BTN_Y+3,BTN_W,BTN_H,12,12);
        g.setColor(new Color(150,30,30));
        g.fillRoundRect(BTN_STOP_X,BTN_Y,BTN_W,BTN_H,12,12);
        g.setColor(new Color(255,255,255,25));
        g.fillRoundRect(BTN_STOP_X+4,BTN_Y+3,BTN_W-8,BTN_H/2-3,8,8);
        g.setColor(new Color(210,65,65));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(BTN_STOP_X,BTN_Y,BTN_W,BTN_H,12,12);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial",Font.BOLD,14));
        g.setColor(Color.WHITE);
        FontMetrics fm2 = g.getFontMetrics();
        String lbl2 = "Fine sessione";
        g.drawString(lbl2, BTN_STOP_X+(BTN_W-fm2.stringWidth(lbl2))/2, BTN_Y+BTN_H/2+fm2.getAscent()/2-3);
    }

    private String truncate(String s, FontMetrics fm, int maxW) {
        if (fm.stringWidth(s)<=maxW) return s;
        while (s.length()>1 && fm.stringWidth(s+"…")>maxW) s=s.substring(0,s.length()-1);
        return s+"…";
    }
    private boolean inRect(int mx,int my,int x,int y,int w,int h) {
        return mx>=x && mx<=x+w && my>=y && my<=y+h;
    }
}
