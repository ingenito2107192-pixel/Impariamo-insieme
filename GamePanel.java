import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/**
 * GamePanel
 * ---------
 * Pannello principale. Gestisce la navigazione tra schermate
 * e istanzia la modalità di gioco corretta.
 *
 * Aggiunge rispetto alla versione precedente:
 *   - preload di tutte le sprite all'avvio (SpriteLoader)
 *   - schermata di loading durante il preload
 */
public class GamePanel extends JPanel implements ActionListener, MouseListener, KeyListener {

    private static final int W = 800;
    private static final int H = 600;

    // Callback per tornare alla schermata di login/home
    private Runnable onExit;

    // Client di rete: se impostato, manda score/vite al server ogni secondo
    private GameClient gameClient = null;
    public void setGameClient(GameClient c) {
        this.gameClient = c;
        // Recupera il nome alunno dal client per il salvataggio punteggi
        if (c != null) this.nomeAlunno = c.getNomeAlunno();
    }

    // Nome dell'alunno che sta giocando (per salvare i punteggi)
    private String nomeAlunno = "";

    private static final String[] FILASTROCCHE = {
        "Quando due o piu' numeri vuoi mettere insieme,\n" +
        "usa l'addizione, che unisce con precisione.\n" +
        "Uno piu' uno fa due, questo e' il primo passo,\n" +
        "e piano piano impari senza nessun imbarazzo!\n" +
        "Sommare significa aggiungere con attenzione,\n" +
        "per scoprire il totale di ogni operazione.",

        "Quando vuoi togliere qualcosa da un insieme,\n" +
        "la sottrazione e' la regola che ti viene bene.\n" +
        "Se hai cinque caramelle e ne mangi un paio,\n" +
        "quante te ne restano? Conta senza sbaglio!\n" +
        "Togliere significa guardare cosa rimane alla fine,\n" +
        "e trovare la risposta senza spine.",

        "La moltiplicazione e' una somma ripetuta,\n" +
        "che rende piu' veloce una conta conosciuta.\n" +
        "Tre per quattro ti da' il totale immediato,\n" +
        "e il risultato e' presto calcolato!\n" +
        "Moltiplicare vuol dire ripetere con ordine,\n" +
        "per ottenere un risultato senza confusione.",

        "La divisione serve a distribuire in parti uguali,\n" +
        "quando hai qualcosa da condividere senza diseguali.\n" +
        "Se hai dieci biscotti tra cinque persone,\n" +
        "la divisione ti guida verso la soluzione!\n" +
        "Dividere significa condividere con giustizia,\n" +
        "trovando la giusta distribuzione con letizia."
    };

    // Stato 
    // Schermate: "ready" | "filastrocca" | "game" | "gameover"
    private String    screen = "ready";
    private boolean   paused = false;
    private GameState state;
    private GameMode  activeMode;

    private Map<String, GameMode> modeRegistry;

    private Timer gameLoop;
    private Timer countdown;
    private Timer frameTimer;
    private UdpFrameSender udpSender;

    // ── Pannello pausa (overlay Swing reale, evita problemi hit-test) ──
    private JPanel pauseOverlay;
    private JButton btnRiprendi;
    private JButton btnEsci;

    private void buildPauseOverlay() {
        pauseOverlay = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, getWidth(), getHeight());
                int bw = 340, bh = 220;
                int bx = (getWidth() - bw) / 2, by = (getHeight() - bh) / 2;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 40, 90, 230));
                g2.fillRoundRect(bx, by, bw, bh, 24, 24);
                g2.setColor(new Color(100, 140, 255));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(bx, by, bw, bh, 24, 24);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String titolo = "⏸  PAUSA";
                g2.drawString(titolo, (getWidth() - fm.stringWidth(titolo)) / 2, by + 58);
            }
        };
        pauseOverlay.setOpaque(false);
        pauseOverlay.setBounds(0, 0, W, H);

        int bw = 340, bh = 220;
        int bx = (W - bw) / 2, by = (H - bh) / 2;

        btnRiprendi = makeOverlayButton("▶  Riprendi", new Color(50, 170, 80));
        btnRiprendi.setBounds(bx + 40, by + 88, bw - 80, 48);
        btnRiprendi.addActionListener(ev -> {
            hidePauseOverlay();
            paused = false;
            gameLoop.start();
            countdown.start();
            requestFocusInWindow();
        });

        btnEsci = makeOverlayButton("✕  Torna alla schermata alunno", new Color(190, 50, 50));
        btnEsci.setBounds(bx + 40, by + 150, bw - 80, 48);
        btnEsci.addActionListener(ev -> {
            hidePauseOverlay();
            paused = false;
            // Ferma i timer senza passare per la schermata gameover
            gameLoop.stop();
            countdown.stop();
            if (activeMode != null) activeMode.onGameEnd();
            if (onExit != null) onExit.run();
        });

        pauseOverlay.add(btnRiprendi);
        pauseOverlay.add(btnEsci);
    }

    private JButton makeOverlayButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showPauseOverlay() {
        if (pauseOverlay == null) buildPauseOverlay();
        if (pauseOverlay.getParent() == null) add(pauseOverlay);
        pauseOverlay.setVisible(true);
        setComponentZOrder(pauseOverlay, 0);
        revalidate();
        repaint();
    }

    private void hidePauseOverlay() {
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        repaint();
    }

    // Costruttore
    public GamePanel(Runnable onExit) {
        this.onExit = onExit;
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(245, 248, 255));
        setLayout(null);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        modeRegistry = buildModeRegistry();

        gameLoop  = new Timer(16, this);
        countdown = new Timer(1000, e -> {
            if (state != null && "game".equals(screen)) {
                state.tick();
                if (gameClient != null) {
                    String domanda = activeMode != null ? activeMode.getCurrentQuestion() : "";
                    gameClient.sendScoreFull(state.score, state.lives, state.timeLeft,
                                             state.domandeRisposte, domanda);
                }
                if (state.gameOver) endGame();
                repaint();
            }
        });

        // Preload sprite in background (silenzioso, senza mostrare schermata loading)
        new Thread(() -> {
            SpriteLoader.get().preloadAll();
            SwingUtilities.invokeLater(() -> { screen = "ready"; repaint(); });
        }).start();
    }

    public void stopGame(){
        gameLoop.stop();
        countdown.stop();
        if (frameTimer != null) frameTimer.stop();
        if (udpSender != null) { udpSender.close(); udpSender = null; }
        if(activeMode != null){
            activeMode.onGameEnd();
        }
        screen = "teacher_stopped";
        repaint();
    }

    /**
     * Chiamato da GameWindow quando vuole avviare il gioco.
     * Lancia direttamente la partita (o la filastrocca se abilitata).
     */
    public void launchGame() {
        if (gameClient != null) this.nomeAlunno = gameClient.getNomeAlunno();
        paused = false;
        gameLoop.stop();
        countdown.stop();

        // Crea UDP sender verso il server
        if (gameClient != null && udpSender == null) {
            udpSender = new UdpFrameSender(GameClient.HOST, gameClient.getNomeAlunno());
        }

        // Timer invio frame via UDP
        if (frameTimer == null) {
            frameTimer = new Timer(80, e -> {
                if (gameClient == null || !gameClient.getState().equals(GameClient.State.PLAYING)) return;
                if (activeMode == null || !"game".equals(screen)) return;
                if (udpSender == null) return;
                // Cattura snapshot sull'EDT
                java.awt.image.BufferedImage snapshot =
                    new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D sg = snapshot.createGraphics();
                activeMode.draw(sg);
                sg.dispose();
                final java.awt.image.BufferedImage img = snapshot;
                final UdpFrameSender sender = udpSender;
                new Thread(() -> {
                    try {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(32768);
                        javax.imageio.ImageWriteParam param = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next().getDefaultWriteParam();
                        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.30f);
                        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
                        writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
                        writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
                        writer.dispose();
                        sender.sendFrame(baos.toByteArray());  // invia raw bytes via UDP
                    } catch (Exception ex) { /* ignora */ }
                }, "frame-sender").start();
            });
            frameTimer.setRepeats(true);
        }
        frameTimer.start();
        goToFilastroccaOrGame();
    }

    // Registry modalità 
    private Map<String, GameMode> buildModeRegistry() {
        Map<String, GameMode> r = new LinkedHashMap<>();
        GameMode bm = new BubbleMode(() -> endGame());
        r.put(bm.getModeId(), bm);
        // Aggiungi nuove modalità qui
        return r;
    }

    // Navigazione
    private void startGame() {
        state      = new GameState();
        activeMode = modeRegistry.get(GameConfig.get().getGameModeId());
        if (activeMode == null) activeMode = modeRegistry.values().iterator().next();
        activeMode.init(state);
        // Notifica il server che l'alunno ha ricominciato da zero
        if (gameClient != null) gameClient.sendScore(0, state.lives);
        screen = "game";
        gameLoop.start();
        countdown.start();
        requestFocusInWindow();
    }

    private void goToFilastroccaOrGame() {
        if (GameConfig.get().isShowFilastrocca()) { screen = "filastrocca"; repaint(); }
        else startGame();
    }

    private void endGame() {
        if ("gameover".equals(screen)) return;
        gameLoop.stop();
        countdown.stop();
        if (frameTimer != null) frameTimer.stop();
        if (udpSender != null) { udpSender.close(); udpSender = null; }
        if (activeMode != null) activeMode.onGameEnd();
        if (gameClient != null) gameClient.sendScore(state.score, state.lives);
        // Salva il punteggio nella cronologia
        if (nomeAlunno != null && !nomeAlunno.isEmpty())
            ScoreStore.get().aggiungiPunteggio(nomeAlunno, state.operation, state.score);
        screen = "gameover";
        repaint();
    }

    // Game loop 
    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused) return;
        if (activeMode != null && screen.equals("game")) {
            activeMode.update();
            if (state.gameOver) endGame();
        }
        repaint();
    }

    //  Disegno 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (screen) {
            case "ready":       /* schermata vuota durante caricamento sprite — ignorata */ break;
            case "menu":        drawMenu(g2);        break;
            case "filastrocca": drawFilastrocca(g2); break;
            case "game":        if (activeMode != null) activeMode.draw(g2); break;
            case "gameover":    drawGameOver(g2);    break;
            case "teacher_stopped": drawTeacherStopped(g2); break;
        }

        // Deposita il frame nel registro per il monitor del docente
        if (nomeAlunno != null && !nomeAlunno.isEmpty() && "game".equals(screen)) {
            java.awt.image.BufferedImage frame =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D fg = frame.createGraphics();
            fg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (activeMode != null) activeMode.draw(fg);
            fg.dispose();
            GameFrameRegistry.putFrame(nomeAlunno, frame);
        }

        // Overlay pausa gestito da pauseOverlay JPanel (vedi showPauseOverlay)
    }

    // Schermata TEACHER STOPPED (docente ha terminato la sessione)
    private void drawTeacherStopped(Graphics2D g) {
        // Sfondo rosso
        GradientPaint bg = new GradientPaint(0,0,new Color(180,30,30),0,H,new Color(100,10,10));
        g.setPaint(bg); g.fillRect(0,0,W,H);

        // Overlay scuro
        g.setColor(new Color(0,0,0,80));
        g.fillRect(0,0,W,H);

        // Testo principale
        g.setFont(new Font("Arial", Font.BOLD, 38));
        g.setColor(Color.WHITE);
        drawCentered(g, "IL DOCENTE HA TERMINATO", H/2 - 60);
        drawCentered(g, "LA SESSIONE", H/2 - 10);

        // Pulsante home
        drawBigBtn(g, (W-260)/2, H/2 + 50, 260, 55, new Color(80,80,80), "Torna alla Home");
    }

    // Schermata LOADING 
    private void drawLoading(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0,0,new Color(30,40,100),0,H,new Color(10,20,60));
        g.setPaint(bg); g.fillRect(0,0,W,H);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.WHITE);
        drawCentered(g, "Math Game", 220);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(new Color(180, 200, 255));
        drawCentered(g, "Caricamento sprite in corso...", 280);
        // Barra avanzamento animata
        g.setColor(new Color(60, 80, 180));
        g.fillRoundRect(200, 320, 400, 14, 8, 8);
        long t = (System.currentTimeMillis() / 10) % 400;
        g.setColor(new Color(120, 160, 255));
        g.fillRoundRect(200, 320, (int)t, 14, 8, 8);
    }

    //  Schermata MENU
    private void drawMenu(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0,0,new Color(230,240,255),0,H,new Color(200,225,250));
        g.setPaint(bg); g.fillRect(0,0,W,H);

        // Anteprima sprite nella zona decorativa
        drawMenuSpritePreviews(g);

        g.setFont(new Font("Arial", Font.BOLD, 44));
        g.setColor(new Color(50,60,130));
        drawCentered(g, "Math Game", 80);

        g.setFont(new Font("Arial", Font.PLAIN, 17));
        g.setColor(new Color(90,100,140));
        drawCentered(g, "Scoppi gli oggetti con il risultato corretto!", 112);

        // Box config corrente
        GameConfig cfg = GameConfig.get();
        String info = "Operazione: " + GameConfig.OP_NOMI[cfg.getOperation()]
                + "   |   Difficolta': " + GameConfig.DIFF_NOMI[cfg.getDifficulty()]
                + "   |   Tempo: " + cfg.getSessionDuration() + "s"
                + "   |   Vite: " + cfg.getInitialLives();
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        int infoW = fm.stringWidth(info) + 40;
        int infoX = (W - infoW) / 2;
        g.setColor(new Color(60,70,140,180));
        g.fillRoundRect(infoX, 128, infoW, 28, 10, 10);
        g.setColor(new Color(200,215,255));
        g.drawString(info, infoX + 20, 147);

        // Pulsanti
        drawBigBtn(g, (W-280)/2, 185, 280, 65, new Color(80,170,90),  "Inizia a Giocare");
    }

    /** Disegna alcune sprite di anteprima nel menu per mostrare gli oggetti */
    private void drawMenuSpritePreviews(Graphics2D g) {
        String[] previews = {
            "balloon_red_idle","balloon_yellow_idle","balloon_green_idle","balloon_blue_idle","balloon_purple_idle",
            "star_gold_idle","heart_red_idle","drop_blue_idle","ring_green_idle","sphere_gold_idle"
        };
        int startX = 40; int y = 370; int sz = 54; int gap = 66;
        for (int i = 0; i < previews.length && i < 10; i++) {
            java.awt.image.BufferedImage img = SpriteLoader.get().load(previews[i]);
            if (img != null) {
                g.drawImage(img, startX + i * gap, y, sz, sz, null);
            }
        }
        // seconda riga
        String[] previews2 = {
            "ball_soccer_idle","ball_basketball_idle","ball_tennis_idle",
            "sphere_red_idle","sphere_blue_idle","sphere_purple_idle",
            "star_red_idle","star_blue_idle","drop_green_idle","ring_red_idle"
        };
        y = 430;
        for (int i = 0; i < previews2.length && i < 10; i++) {
            java.awt.image.BufferedImage img = SpriteLoader.get().load(previews2[i]);
            if (img != null) {
                g.drawImage(img, startX + i * gap, y, sz, sz, null);
            }
        }
    }

    // Schermata FILASTROCCA 
    private void drawFilastrocca(Graphics2D g) {
        int op = GameConfig.get().getOperation();
        GradientPaint bg = new GradientPaint(0,0,new Color(255,252,230),0,H,new Color(255,238,185));
        g.setPaint(bg); g.fillRect(0,0,W,H);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(getOpColor(op));
        drawCentered(g, GameConfig.OP_SIMBOLI[op] + "  " + GameConfig.OP_NOMI[op], 58);

        g.setColor(new Color(255,255,255,180));
        g.fillRoundRect(70,78,660,380,20,20);
        g.setColor(getOpColor(op));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(70,78,660,380,20,20);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Arial", Font.PLAIN, 17));
        g.setColor(new Color(60,60,80));
        String[] righe = FILASTROCCHE[op].split("\n");
        int lineY = 140;
        for (String riga : righe) { g.drawString(riga, 190, lineY); lineY += 38; }

        drawBigBtn(g, (W-260)/2, 488, 260, 55, getOpColor(op), "Inizia a Giocare!");
    }

    // Schermata GAMEOVER 
    // Immagine schermata risultato (caricata una volta sola)
    private java.awt.image.BufferedImage risultatoImg = null;
    private boolean risultatoImgTried = false;

    private void drawGameOver(Graphics2D g) {
        if (state == null) return;

        // Carica l'immagine risultato la prima volta
        if (!risultatoImgTried) {
            risultatoImg      = ImageHelper.loadImage("risultato.png.png");
            risultatoImgTried = true;
        }

        // Sfondo: immagine risultato oppure gradiente di fallback
        if (risultatoImg != null) {
            g.drawImage(risultatoImg, 0, 0, W, H, null);
        } else {
            GradientPaint bg = new GradientPaint(0,0,new Color(230,240,255),0,H,new Color(200,215,245));
            g.setPaint(bg); g.fillRect(0,0,W,H);
        }

        // Punteggio centrato su x=403, y=530
        g.setFont(new Font("Arial", Font.BOLD, 80));
        String sc = String.valueOf(state.score);
        FontMetrics fm = g.getFontMetrics();
        int scoreX = 403 - fm.stringWidth(sc) / 2;
        int scoreY = 420;
        g.setColor(new Color(0,0,0,120));
        g.drawString(sc, scoreX + 3, scoreY + 3);
        g.setColor(new Color(50, 50, 180));
        g.drawString(sc, scoreX, scoreY);

        // Pulsante 3D centrato su x=403, y=475
        int btnW = 280, btnH = 50;
        int btnX = 403 - btnW / 2;
        int btnY = 475 - btnH / 2;
        // Ombra 3D
        g.setColor(new Color(20, 50, 130));
        g.fillRoundRect(btnX + 4, btnY + 7, btnW, btnH, 14, 14);
        // Corpo pulsante
        g.setColor(new Color(60, 110, 220));
        g.fillRoundRect(btnX, btnY, btnW, btnH, 14, 14);
        // Lucido superiore
        g.setColor(new Color(255, 255, 255, 60));
        g.fillRoundRect(btnX + 4, btnY + 3, btnW - 8, btnH / 2 - 4, 10, 10);
        // Bordo
        g.setColor(new Color(100, 160, 255));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(btnX, btnY, btnW, btnH, 14, 14);
        g.setStroke(new BasicStroke(1f));
        // Testo
        g.setFont(new Font("Arial", Font.BOLD, 18));
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        String lbl = "Schermata principale";
        g.drawString(lbl, btnX + (btnW - fm.stringWidth(lbl)) / 2, btnY + btnH / 2 + fm.getAscent() / 2 - 3);
    }

    // Utilità disegno
    private void drawBigBtn(Graphics2D g, int x, int y, int w, int h, Color c, String label) {
        g.setColor(new Color(0,0,0,28));
        g.fillRoundRect(x+3,y+3,w,h,15,15);
        g.setColor(c);
        g.fillRoundRect(x,y,w,h,15,15);
        g.setColor(c.brighter());
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x,y,w,h,15,15);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 19));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x+(w-fm.stringWidth(label))/2, y+h/2+fm.getAscent()/2-3);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    private Color getOpColor(int op) {
        switch(op) {
            case 0: return new Color(50,  150, 200);
            case 1: return new Color(30,  160,  60);
            case 2: return new Color(140,  50, 190);
            default:return new Color(220, 110,  20);
        }
    }

    private String getMotivMsg(int score) {
        if (score == 0)  return "Dai, puoi farcela!";
        if (score < 30)  return "Buono!";
        if (score < 60)  return "Bene!";
        if (score < 100) return "Ottimo lavoro! Sei un campione!";
        return "Fantastico! Sei un genio della matematica!";
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }

    // Click — usa mousePressed (più affidabile di mouseClicked)
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        if (paused) return;  // overlay JButton gestisce i click in pausa

        switch (screen) {
            case "menu":
                if (inRect(mx, my, (W-280)/2, 185, 280, 65)) {
                    goToFilastroccaOrGame();
                }
                break;
            case "filastrocca":
                if (inRect(mx,my,(W-260)/2,488,260,55)) startGame();
                break;
            case "game":
                if (activeMode != null) activeMode.onMouseClick(e);
                break;
            case "teacher_stopped":
                if (inRect(mx,my,(W-260)/2,H/2+50,260,55)) { if (onExit != null) onExit.run(); }
                break;
            case "gameover":
                if (state != null) {
                    int btnW = 280, btnH = 50;
                    int btnX = 403 - btnW / 2;
                    int btnY = 475 - btnH / 2;
                    if (inRect(mx, my, btnX, btnY, btnW, btnH)) { if (onExit != null) onExit.run(); }
                }
                break;
        }
    }

    @Override public void mouseClicked(MouseEvent e)  {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e)  {}
    public void mouseExited(MouseEvent e)   {}

    // Tasto ESC → pausa / riprendi (durante il gioco) oppure torna alla Home
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ESCAPE) return;

        if ("game".equals(screen)) {
            paused = !paused;
            if (paused) {
                gameLoop.stop();
                countdown.stop();
                showPauseOverlay();
            } else {
                hidePauseOverlay();
                gameLoop.start();
                countdown.start();
                requestFocusInWindow();
            }
            repaint();
        } else {
            if (onExit != null) onExit.run();
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}
}