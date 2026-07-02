import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;


public class TeacherImagePanel extends JPanel {

    private static final int W = 800;
    private static final int H = 600;

    private final IntConsumer onStartGame;
    private final Runnable onLogout;
    private BufferedImage bgImage;

    private String nomeDocente = null;

    private static final int BTN_OUT_X = 620, BTN_OUT_Y = 10, BTN_OUT_W = 165, BTN_OUT_H = 36;
    private static final int BTN_ALU_X = 610, BTN_ALU_Y = 550, BTN_ALU_W = 175, BTN_ALU_H = 38;

    private int hoverOp = -1;

    private static final int[][] OP_RECTS = {
        { 512, 195, 115, 130 },
        { 632, 195, 113, 130 },
        { 512, 336, 115, 129 },
        { 632, 336, 113, 129 },
    };

    private static final Color[] OP_COLORS = {
        new Color(50,  150, 200),
        new Color(30,  160,  60),
        new Color(140,  50, 190),
        new Color(220, 110,  20),
    };

    public TeacherImagePanel(IntConsumer onStartGame, Runnable onLogout) {
        this.onStartGame = onStartGame;
        this.onLogout    = onLogout;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);

        bgImage = ImageHelper.loadImage("Schermata docente.jpeg");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                if (mx >= BTN_OUT_X && mx <= BTN_OUT_X + BTN_OUT_W
                        && my >= BTN_OUT_Y && my <= BTN_OUT_Y + BTN_OUT_H) {
                    onLogout.run();
                    return;
                }
                if (mx >= BTN_ALU_X && mx <= BTN_ALU_X + BTN_ALU_W
                        && my >= BTN_ALU_Y && my <= BTN_ALU_Y + BTN_ALU_H) {
                    mostraAlunniAssegnati();
                    return;
                }
                int op = opAt(mx, my);
                if (op >= 0) onStartGame.accept(op);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int op = opAt(e.getX(), e.getY());
                if (op != hoverOp) {
                    hoverOp = op;
                    setCursor(op >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        });
    }

    public void setNomeDocente(String nome) {
        this.nomeDocente = nome;
    }

    private int opAt(int mx, int my) {
        for (int i = 0; i < 4; i++) {
            int[] r = OP_RECTS[i];
            if (mx >= r[0] && mx <= r[0]+r[2] && my >= r[1] && my <= r[1]+r[3])
                return i;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, W, H, null);
        } else {
            drawFallback(g2);
            return;
        }

        for (int i = 0; i < 4; i++) {
            int[] r = OP_RECTS[i];
            if (i == hoverOp) {
                Color c = OP_COLORS[i];
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
                g2.fillRoundRect(r[0], r[1], r[2], r[3], 14, 14);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 220));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawRoundRect(r[0]-2, r[1]-2, r[2]+4, r[3]+4, 16, 16);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                String tip = "Seleziona";
                FontMetrics fm = g2.getFontMetrics();
                int tx = r[0] + (r[2] - fm.stringWidth(tip)) / 2;
                int ty = r[1] + r[3] + 16;
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect(tx - 4, ty - 13, fm.stringWidth(tip) + 8, 18, 6, 6);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
                g2.drawString(tip, tx, ty);
            }
        }

        drawBtn(g2, BTN_OUT_X, BTN_OUT_Y, BTN_OUT_W, BTN_OUT_H, new Color(180, 60, 60), "Logout");
        drawBtnAlunni(g2, BTN_ALU_X, BTN_ALU_Y, BTN_ALU_W, BTN_ALU_H);
    }

    private void drawBtn(Graphics2D g2, int x, int y, int w, int h, Color c, String label) {
        g2.setColor(new Color(0, 0, 0, 25));
        g2.fillRoundRect(x+2, y+2, w, h, 12, 12);
        g2.setColor(c);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h/2 + fm.getAscent()/2 - 3);
    }

    private void drawBtnAlunni(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(x+3, y+3, w, h, 12, 12);
        g2.setColor(new Color(70, 160, 220));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(120, 200, 245, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String label = "ALUNNI";
        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h/2 + fm.getAscent()/2 - 3);
    }

    // ---------------------------------------------------------------
    // Finestra ALUNNI con scroll e pallino punteggi
    // ---------------------------------------------------------------

    private void mostraAlunniAssegnati() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = creaDialog(owner, "I tuoi alunni");

        final List<User> alunni = (nomeDocente != null)
            ? UserStore.get().getAlunniDiDocente(nomeDocente)
            : new java.util.ArrayList<>();

        final int PW = 400, PH = 420;
        final int ROW_H = 44;
        final int LX = 16, LY = 62, LW = PW - 32, LH = PH - 72;
        final int ROWS_VIS = LH / ROW_H;
        final int SX = LX + LW - 26, SW = 22, SH = 22;
        final int SU = LY + 4, SD = LY + LH - SH - 4;
        final int DOT_R = 9;
        final int DOT_CX = SX - DOT_R * 2 - 6;

        final int[] scroll   = { 0 };
        final int[] hoverDot = { -1 };

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setPaint(new GradientPaint(0, 0, new Color(225, 240, 255), 0, PH, new Color(195, 220, 245)));
                g2.fillRect(0, 0, PW, PH);

                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.setColor(new Color(30, 90, 160));
                String titolo = nomeDocente != null ? "Alunni di: " + nomeDocente : "Alunni assegnati";
                g2.drawString(titolo, (PW - g2.getFontMetrics().stringWidth(titolo)) / 2, 38);

                g2.setColor(new Color(100, 160, 220, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(LX, 50, LX + LW, 50);
                g2.setStroke(new BasicStroke(1f));

                if (alunni.isEmpty()) {
                    g2.setFont(new Font("Arial", Font.ITALIC, 15));
                    g2.setColor(new Color(120, 130, 150));
                    String msg = "Nessun alunno assegnato.";
                    g2.drawString(msg, (PW - g2.getFontMetrics().stringWidth(msg)) / 2, LY + LH / 2);
                    return;
                }

                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillRoundRect(LX, LY, LW, LH, 12, 12);

                // Salvo clip originale — userò due zone distinte:
                // clip lista (per testo/righe) e clip pallini (intera area pannello)
                Shape clipOriginale = g2.getClip();

                int y = LY + ROW_H - 8 - scroll[0] * ROW_H;
                for (int i = 0; i < alunni.size(); i++) {
                    int top   = y - ROW_H + 6;
                    int dotCY = top + (ROW_H - 4) / 2;

                    // Disegno riga solo se visibile dentro la lista
                    if (top + ROW_H > LY && top < LY + LH) {
                        // Sfondo riga (con clip lista)
                        g2.setClip(LX + 2, LY + 2, LW - 4, LH - 4);
                        g2.setColor(i % 2 == 0
                            ? new Color(235, 245, 255, 200)
                            : new Color(210, 228, 248, 160));
                        g2.fillRoundRect(LX + 4, top, LW - 8, ROW_H - 4, 8, 8);

                        // Numero e nome (con clip lista)
                        g2.setFont(new Font("Arial", Font.BOLD, 13));
                        g2.setColor(new Color(100, 150, 200));
                        g2.drawString((i + 1) + ".", LX + 12, y - 4);
                        g2.setFont(new Font("Arial", Font.PLAIN, 15));
                        g2.setColor(new Color(30, 50, 100));
                        g2.drawString(alunni.get(i).getNome(), LX + 36, y - 4);

                        // Pallino ★ — resetto clip così non viene tagliato
                        g2.setClip(clipOriginale);
                        boolean hov = (hoverDot[0] == i);
                        g2.setColor(new Color(0, 0, 0, 30));
                        g2.fillOval(DOT_CX - DOT_R + 2, dotCY - DOT_R + 2, DOT_R * 2, DOT_R * 2);
                        g2.setColor(hov ? new Color(30, 120, 210) : new Color(70, 155, 225));
                        g2.fillOval(DOT_CX - DOT_R, dotCY - DOT_R, DOT_R * 2, DOT_R * 2);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 11));
                        FontMetrics fmD = g2.getFontMetrics();
                    }
                    y += ROW_H;
                }
                g2.setClip(clipOriginale);

                // Frecce scroll
                g2.setColor(scroll[0] > 0 ? new Color(70, 140, 210) : new Color(180, 200, 220));
                g2.fillRoundRect(SX, SU, SW, SH, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                g2.drawString("\u25B2", SX + 4, SU + 15);

                g2.setColor(scroll[0] < alunni.size() - ROWS_VIS
                    ? new Color(70, 140, 210) : new Color(180, 200, 220));
                g2.fillRoundRect(SX, SD, SW, SH, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawString("\u25BC", SX + 4, SD + 15);
            }
        };
        panel.setPreferredSize(new Dimension(PW, PH));

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int dot = dotAt(e.getX(), e.getY(), alunni.size(), scroll[0],
                                LY, LH, ROW_H, DOT_CX, DOT_R);
                if (dot != hoverDot[0]) {
                    hoverDot[0] = dot;
                    panel.setCursor(dot >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    panel.repaint();
                }
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                int maxS = Math.max(0, alunni.size() - ROWS_VIS);
                if (mx >= SX && mx <= SX + SW) {
                    if (my >= SU && my <= SU + SH && scroll[0] > 0)
                        { scroll[0]--; panel.repaint(); return; }
                    if (my >= SD && my <= SD + SH && scroll[0] < maxS)
                        { scroll[0]++; panel.repaint(); return; }
                }
                int dot = dotAt(mx, my, alunni.size(), scroll[0],
                                LY, LH, ROW_H, DOT_CX, DOT_R);
                if (dot >= 0) mostraPunteggiAlunno(dialog, alunni.get(dot).getNome());
            }
        });

        panel.addMouseWheelListener(e -> {
            int maxS = Math.max(0, alunni.size() - ROWS_VIS);
            scroll[0] = Math.max(0, Math.min(maxS, scroll[0] + (e.getWheelRotation() > 0 ? 1 : -1)));
            panel.repaint();
        });

        mostraDialog(dialog, panel);
    }

    // ---------------------------------------------------------------
    // Finestra PUNTEGGI di un singolo alunno
    // ---------------------------------------------------------------

    private void mostraPunteggiAlunno(Window owner, String nomeAlunno) {
        JDialog d2 = creaDialog(owner, "Punteggi di " + nomeAlunno);

        final List<ScoreStore.Record> rec = ScoreStore.get().getPunteggiAlunno(nomeAlunno);

        final int PW = 400, PH = 420;
        final int ROW_H = 48;
        final int LX = 16, LY = 62, LW = PW - 32, LH = PH - 72;
        final int ROWS_VIS = LH / ROW_H;
        final int SX = LX + LW - 26, SW = 22, SH = 22;
        final int SU = LY + 4, SD = LY + LH - SH - 4;

        final int[] scroll = { 0 };

        final Color[] opColors = {
            new Color(50,  150, 200),
            new Color(30,  160,  60),
            new Color(140,  50, 190),
            new Color(220, 110,  20),
        };

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setPaint(new GradientPaint(0, 0, new Color(225, 240, 255), 0, PH, new Color(195, 220, 245)));
                g2.fillRect(0, 0, PW, PH);

                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.setColor(new Color(30, 90, 160));
                String titolo = "Punteggi di: " + nomeAlunno;
                g2.drawString(titolo, (PW - g2.getFontMetrics().stringWidth(titolo)) / 2, 38);

                g2.setColor(new Color(100, 160, 220, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(LX, 50, LX + LW, 50);
                g2.setStroke(new BasicStroke(1f));

                if (rec.isEmpty()) {
                    g2.setFont(new Font("Arial", Font.ITALIC, 15));
                    g2.setColor(new Color(120, 130, 150));
                    String msg = "Nessuna partita registrata.";
                    g2.drawString(msg, (PW - g2.getFontMetrics().stringWidth(msg)) / 2, LY + LH / 2);
                    return;
                }

                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillRoundRect(LX, LY, LW, LH, 12, 12);

                Shape clip = g2.getClip();
                g2.clipRect(LX + 2, LY + 2, LW - SW - 4, LH - 4);

                int y = LY + ROW_H - 8 - scroll[0] * ROW_H;
                for (int i = 0; i < rec.size(); i++) {
                    ScoreStore.Record r = rec.get(i);
                    int top = y - ROW_H + 6;

                    g2.setColor(i % 2 == 0
                        ? new Color(235, 245, 255, 200)
                        : new Color(210, 228, 248, 160));
                    g2.fillRoundRect(LX + 4, top, LW - 8, ROW_H - 4, 8, 8);

                    // Badge operazione
                    Color oc = opColors[opColorIndex(r.operazione)];
                    g2.setColor(oc);
                    g2.fillRoundRect(LX + 8, top + 7, 100, ROW_H - 18, 6, 6);
                    g2.setFont(new Font("Arial", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);
                    FontMetrics fmB = g2.getFontMetrics();
                    g2.drawString(r.operazione,
                        LX + 8 + (100 - fmB.stringWidth(r.operazione)) / 2,
                        top + (ROW_H - 18) / 2 + fmB.getAscent() / 2 + 5);

                    // Punteggio
                    g2.setFont(new Font("Arial", Font.BOLD, 16));
                    g2.setColor(new Color(30, 50, 100));
                    g2.drawString(r.punteggio + " pt", LX + 116, y - 6);

                    // Data
                    g2.setFont(new Font("Arial", Font.PLAIN, 11));
                    g2.setColor(new Color(100, 120, 160));
                    g2.drawString(r.data, LX + 210, y - 6);

                    y += ROW_H;
                }
                g2.setClip(clip);

                g2.setColor(scroll[0] > 0 ? new Color(70, 140, 210) : new Color(180, 200, 220));
                g2.fillRoundRect(SX, SU, SW, SH, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                g2.drawString("\u25B2", SX + 4, SU + 15);

                g2.setColor(scroll[0] < rec.size() - ROWS_VIS
                    ? new Color(70, 140, 210) : new Color(180, 200, 220));
                g2.fillRoundRect(SX, SD, SW, SH, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawString("\u25BC", SX + 4, SD + 15);
            }
        };
        panel.setPreferredSize(new Dimension(PW, PH));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                int maxS = Math.max(0, rec.size() - ROWS_VIS);
                if (mx >= SX && mx <= SX + SW) {
                    if (my >= SU && my <= SU + SH && scroll[0] > 0)
                        { scroll[0]--; panel.repaint(); }
                    else if (my >= SD && my <= SD + SH && scroll[0] < maxS)
                        { scroll[0]++; panel.repaint(); }
                }
            }
        });

        panel.addMouseWheelListener(e -> {
            int maxS = Math.max(0, rec.size() - ROWS_VIS);
            scroll[0] = Math.max(0, Math.min(maxS, scroll[0] + (e.getWheelRotation() > 0 ? 1 : -1)));
            panel.repaint();
        });

        mostraDialog(d2, panel);
    }

    // ---------------------------------------------------------------
    // Helpers condivisi
    // ---------------------------------------------------------------

    private JDialog creaDialog(Window owner, String titolo) {
        if (owner instanceof Frame)  return new JDialog((Frame)  owner, titolo, true);
        if (owner instanceof Dialog) return new JDialog((Dialog) owner, titolo, true);
        return new JDialog((Frame) null, titolo, true);
    }

    private void mostraDialog(JDialog dialog, JPanel contenuto) {
        JButton btnChiudi = new JButton("Chiudi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(70, 160, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    getHeight() / 2 + fm.getAscent() / 2 - 3);
            }
        };
        btnChiudi.setPreferredSize(new Dimension(120, 36));
        btnChiudi.setContentAreaFilled(false);
        btnChiudi.setBorderPainted(false);
        btnChiudi.setFocusPainted(false);
        btnChiudi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChiudi.addActionListener(ev -> dialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        bottom.setOpaque(false);
        bottom.add(btnChiudi);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.add(contenuto, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private int dotAt(int mx, int my, int size, int scroll,
                      int LY, int LH, int ROW_H, int DOT_CX, int DOT_R) {
        int y = LY + ROW_H - 8 - scroll * ROW_H;
        for (int i = 0; i < size; i++) {
            int top   = y - ROW_H + 6;
            int dotCY = top + (ROW_H - 4) / 2;
            int dx = mx - DOT_CX, dy = my - dotCY;
            if (dx*dx + dy*dy <= (DOT_R+3)*(DOT_R+3) && my >= LY && my <= LY + LH)
                return i;
            y += ROW_H;
        }
        return -1;
    }

    private int opColorIndex(String nome) {
        if (nome == null) return 0;
        switch (nome.toUpperCase()) {
            case "ADDIZIONE":       return 0;
            case "SOTTRAZIONE":     return 1;
            case "MOLTIPLICAZIONE": return 2;
            case "DIVISIONE":       return 3;
            default:                return 0;
        }
    }

    // ---------------------------------------------------------------
    // Fallback grafico
    // ---------------------------------------------------------------

    private void drawFallback(Graphics2D g2) {
        g2.setColor(new Color(220, 235, 255));
        g2.fillRect(0, 0, W, H);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.setColor(new Color(20, 40, 120));
        g2.drawString("IMPARIAMO LA MATEMATICA", 40, 70);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(60, 70, 140));
        g2.drawString("Scegli un'operazione per avviare il gioco:", 455, 155);

        String[] simboli = { "+", "\u2212", "\u00D7", "\u00F7" };
        String[] nomi    = { "ADDIZIONE", "SOTTRAZIONE", "MOLTIPLICAZIONE", "DIVISIONE" };

        for (int i = 0; i < 4; i++) {
            int[] r = OP_RECTS[i];
            Color c = OP_COLORS[i];
            boolean hover = (i == hoverOp);

            g2.setColor(hover ? c : new Color(240, 245, 255));
            g2.fillRoundRect(r[0], r[1], r[2], r[3], 16, 16);
            g2.setColor(hover ? c.brighter() : c);
            g2.setStroke(new BasicStroke(hover ? 3f : 2f));
            g2.drawRoundRect(r[0], r[1], r[2], r[3], 16, 16);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(hover ? Color.WHITE : c);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(simboli[i], r[0] + (r[2] - fm.stringWidth(simboli[i])) / 2, r[1] + 52);

            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(hover ? Color.WHITE : new Color(50, 60, 130));
            fm = g2.getFontMetrics();
            g2.drawString(nomi[i], r[0] + (r[2] - fm.stringWidth(nomi[i])) / 2, r[1] + r[3] - 12);
        }

        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(80, 90, 140));
        String hint = "Clicca un'operazione per avviare il gioco";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, (W - fm.stringWidth(hint)) / 2, H - 15);
    }
}
