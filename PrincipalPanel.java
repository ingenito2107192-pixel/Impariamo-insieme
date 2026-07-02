import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;

/**
 * PrincipalPanel - pannello del Preside.
 * Schede: Nuovo Docente | Nuovo Alunno | Elimina Utente | Assegna Alunni
 */
public class PrincipalPanel extends JPanel implements KeyListener {

    private static final int W = 800;
    private static final int H = 600;

    private BufferedImage bgImage;
    private final Runnable onLogout;

    private enum Campo { NOME, PASSWORD }
    private enum SezioneAttiva { CREDENZIALI, DOCENTE, ALUNNO, ELIMINA, ASSEGNA }

    private SezioneAttiva sezione    = SezioneAttiva.CREDENZIALI;
    private Campo         campoAttivo = Campo.NOME;

    private String nuovoNome     = "";
    private String nuovaPassword = "";
    private String feedbackMsg   = "";
    private boolean feedbackOk   = false;
    private String utenteSelezionato = null;

    // Assegnazione
    private String alunnoSelAssegna  = null;
    private String docenteSelAssegna = null;

    // Tabs – riga 1: solo Credenziali (in alto a sinistra, sfalsata)
    private static final int TAB_CRED_X = 20, TAB_CRED_Y = 8, TAB_CRED_W = 155, TAB_H = 36;
    // Tabs – riga 2: gli altri 4 tab
    private static final int TAB_Y = 54;
    private static final int TAB_DOC_X     = 20,  TAB_DOC_W     = 175;
    private static final int TAB_ALU_X     = 205, TAB_ALU_W     = 175;
    private static final int TAB_ELIM_X    = 390, TAB_ELIM_W    = 175;
    private static final int TAB_ASSEGNA_X = 575, TAB_ASSEGNA_W = 205;

    // Credenziali preside
    private String nuovoNomePreside  = "";
    private String nuovaPassPreside  = "";
    private enum CampoCredenziali { NOME, PASS, CONFERMA }
    private CampoCredenziali campoCredenziali = CampoCredenziali.NOME;
    private String confermaPassPreside = "";

    // Form aggiunta
    private static final int FORM_X = 180, FORM_W = 430;
    private static final int NOME_Y = 165, PASS_Y = 225, FIELD_H = 38;
    private static final int BTN_ADD_X = 180, BTN_ADD_Y = 280, BTN_ADD_W = 430, BTN_ADD_H = 44;

    // Pannello elimina
    private static final int ELIM_X = 40, ELIM_Y = 110, ELIM_W = 720, ELIM_H = 360;
    private static final int ROW_H = 44;
    private static final int BTN_ELIM_W = 100, BTN_ELIM_H = 28;
    private int scrollElim = 0;

    // Lista utenti sola lettura
    private static final int LIST_X = 40, LIST_Y = 410, LIST_W = 720, LIST_H = 155;
    private int scrollOffset = 0;

    // Logout
    private static final int BTN_OUT_X = 620, BTN_OUT_Y = 10, BTN_OUT_W = 165, BTN_OUT_H = 36;

    // Pannello assegna
    private static final int ASSEGNA_X = 40, ASSEGNA_Y = 120, ASSEGNA_W = 340, ASSEGNA_H = 390;
    private static final int DOCENTI_X = 420, DOCENTI_Y = 120, DOCENTI_W = 340, DOCENTI_H = 390;
    private static final int BTN_ASSEGNA_W = 160, BTN_ASSEGNA_H = 38;
    private int scrollAlunni = 0, scrollDocenti = 0;

    public PrincipalPanel(Runnable onLogout) {
        this.onLogout = onLogout;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
        addMouseWheelListener(e -> {
            int dir = e.getWheelRotation() > 0 ? 1 : -1;
            int mx = e.getX(), my = e.getY();
            if (sezione == SezioneAttiva.ASSEGNA) {
                if (inRect(mx, my, ASSEGNA_X, ASSEGNA_Y, ASSEGNA_W, ASSEGNA_H)) {
                    scrollAlunni = Math.max(0, scrollAlunni + dir);
                } else if (inRect(mx, my, DOCENTI_X, DOCENTI_Y, DOCENTI_W, DOCENTI_H)) {
                    scrollDocenti = Math.max(0, scrollDocenti + dir);
                }
            } else if (sezione == SezioneAttiva.ELIMINA) {
                scrollElim = Math.max(0, scrollElim + dir);
            } else {
                scrollOffset = Math.max(0, scrollOffset + dir);
            }
            repaint();
        });
        bgImage = ImageHelper.loadImage("schermate/schermata Preside.png");
    }

    public void reset() {
        nuovoNome = ""; nuovaPassword = ""; feedbackMsg = "";
        campoAttivo = Campo.NOME; scrollOffset = 0; scrollElim = 0;
        utenteSelezionato = null; alunnoSelAssegna = null; docenteSelAssegna = null;
        scrollAlunni = 0; scrollDocenti = 0;
        nuovoNomePreside = ""; nuovaPassPreside = ""; confermaPassPreside = "";
        campoCredenziali = CampoCredenziali.NOME;
        sezione = SezioneAttiva.CREDENZIALI;
        repaint();
    }

    // ── Paint ─────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (bgImage != null) g2.drawImage(bgImage, 0, 0, W, H, null);
        else {
            GradientPaint bg = new GradientPaint(0,0,new Color(245,240,255),0,H,new Color(220,210,250));
            g2.setPaint(bg); g2.fillRect(0,0,W,H);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(new Color(70,30,130));
        drawCentered(g2, "Pannello Preside", 42);

        drawBtn(g2, BTN_OUT_X, BTN_OUT_Y, BTN_OUT_W, BTN_OUT_H, new Color(180,60,60), "Logout");

        drawTab(g2, TAB_CRED_X,    TAB_CRED_Y, TAB_CRED_W,    TAB_H, "Credenziali",    sezione == SezioneAttiva.CREDENZIALI);
        drawTab(g2, TAB_DOC_X,     TAB_Y, TAB_DOC_W,     TAB_H, "Nuovo Docente",  sezione == SezioneAttiva.DOCENTE);
        drawTab(g2, TAB_ALU_X,     TAB_Y, TAB_ALU_W,     TAB_H, "Nuovo Alunno",   sezione == SezioneAttiva.ALUNNO);
        drawTab(g2, TAB_ELIM_X,    TAB_Y, TAB_ELIM_W,    TAB_H, "Elimina",        sezione == SezioneAttiva.ELIMINA);
        drawTab(g2, TAB_ASSEGNA_X, TAB_Y, TAB_ASSEGNA_W, TAB_H, "Assegna Alunni", sezione == SezioneAttiva.ASSEGNA);

        switch (sezione) {
            case CREDENZIALI: drawPannelloCredenziali(g2); break;
            case ELIMINA: drawPannelloElimina(g2); break;
            case ASSEGNA: drawPannelloAssegna(g2); break;
            default:
                drawForm(g2, sezione == SezioneAttiva.DOCENTE ? "docente" : "alunno");
                if (!feedbackMsg.isEmpty()) {
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    g2.setColor(feedbackOk ? new Color(30,140,60) : new Color(190,40,40));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(feedbackMsg, (W - fm.stringWidth(feedbackMsg)) / 2, BTN_ADD_Y + BTN_ADD_H + 24);
                }
                g2.setColor(new Color(180,160,220)); g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(LIST_X, LIST_Y - 12, LIST_X + LIST_W, LIST_Y - 12); g2.setStroke(new BasicStroke(1f));
                drawLista(g2);
                break;
        }
    }

    // ── Pannello Credenziali Preside ──────────────────────────────
    private void drawPannelloCredenziali(Graphics2D g2) {
        int panX = 160, panY = 110, panW = 480, panH = 310;
        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(panX, panY, panW, panH, 16, 16);
        g2.setColor(new Color(90,50,180,120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(panX, panY, panW, panH, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(70,30,130));
        FontMetrics fm = g2.getFontMetrics();
        String title = "Cambia Credenziali Preside";
        g2.drawString(title, panX + (panW - fm.stringWidth(title))/2, panY + 34);

        int fx = panX + 30, fw = panW - 60, fh = 36;
        int y1 = panY + 60, y2 = y1 + 56, y3 = y2 + 56;

        g2.setFont(new Font("Arial", Font.BOLD, 13)); g2.setColor(new Color(70,30,130));
        g2.drawString("Nuovo nome utente:", fx, y1 - 6);
        g2.drawString("Nuova password:", fx, y2 - 6);
        g2.drawString("Conferma password:", fx, y3 - 6);

        drawField(g2, fx, y1, fw, fh, nuovoNomePreside, "es. Admin", campoCredenziali == CampoCredenziali.NOME);
        drawField(g2, fx, y2, fw, fh, "*".repeat(nuovaPassPreside.length()), "password", campoCredenziali == CampoCredenziali.PASS);
        drawField(g2, fx, y3, fw, fh, "*".repeat(confermaPassPreside.length()), "conferma password", campoCredenziali == CampoCredenziali.CONFERMA);

        int btnY = y3 + fh + 18;
        drawBtn(g2, fx, btnY, fw, 40, new Color(90,50,180), "Salva nuove credenziali");

        if (!feedbackMsg.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(feedbackOk ? new Color(30,140,60) : new Color(190,40,40));
            fm = g2.getFontMetrics();
            g2.drawString(feedbackMsg, panX + (panW - fm.stringWidth(feedbackMsg))/2, btnY + 58);
        }
    }

    // ── Pannello Assegna ──────────────────────────────────────────
    private void drawPannelloAssegna(Graphics2D g2) {
        List<User> tuttiAlunni = UserStore.get().getAlunni();
        List<User> tuttiDocenti = UserStore.get().getDocenti();

        // Titoli colonne
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        g2.setColor(new Color(50,150,80));
        g2.drawString("Alunni", ASSEGNA_X + 12, ASSEGNA_Y - 8);
        g2.setColor(new Color(60,100,200));
        g2.drawString("Docenti", DOCENTI_X + 12, DOCENTI_Y - 8);

        // Pannello alunni
        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(ASSEGNA_X, ASSEGNA_Y, ASSEGNA_W, ASSEGNA_H, 14, 14);

        Shape oldClip = g2.getClip();
        g2.clipRect(ASSEGNA_X + 2, ASSEGNA_Y + 2, ASSEGNA_W - 4, ASSEGNA_H - 4);
        int y = ASSEGNA_Y + 30 - scrollAlunni * ROW_H;
        for (User a : tuttiAlunni) {
            boolean sel = a.getNome().equals(alunnoSelAssegna);
            g2.setColor(sel ? new Color(200,240,200) : new Color(235,250,235));
            g2.fillRoundRect(ASSEGNA_X + 8, y - 22, ASSEGNA_W - 42, ROW_H - 4, 8, 8);
            if (sel) { g2.setColor(new Color(30,140,60)); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(ASSEGNA_X+8, y-22, ASSEGNA_W-42, ROW_H-4, 8, 8); g2.setStroke(new BasicStroke(1f)); }
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(30,80,30));
            g2.drawString(a.getNome(), ASSEGNA_X + 18, y - 6);
            // docente assegnato
            String dAss = a.getDocenteAssegnato();
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            g2.setColor(dAss != null ? new Color(60,100,200) : new Color(160,160,180));
            g2.drawString(dAss != null ? "→ " + dAss : "(nessun docente)", ASSEGNA_X + 18, y + 7);
            y += ROW_H;
        }
        if (tuttiAlunni.isEmpty()) {
            g2.setFont(new Font("Arial", Font.ITALIC, 13)); g2.setColor(new Color(160,180,160));
            g2.drawString("(nessun alunno)", ASSEGNA_X + 20, ASSEGNA_Y + 50);
        }
        g2.setClip(oldClip);

        // Pannello docenti
        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(DOCENTI_X, DOCENTI_Y, DOCENTI_W, DOCENTI_H, 14, 14);

        g2.clipRect(DOCENTI_X + 2, DOCENTI_Y + 2, DOCENTI_W - 4, DOCENTI_H - 4);
        y = DOCENTI_Y + 30 - scrollDocenti * ROW_H;
        for (User d : tuttiDocenti) {
            boolean sel = d.getNome().equals(docenteSelAssegna);
            g2.setColor(sel ? new Color(200,220,255) : new Color(235,240,255));
            g2.fillRoundRect(DOCENTI_X + 8, y - 22, DOCENTI_W - 42, ROW_H - 4, 8, 8);
            if (sel) { g2.setColor(new Color(60,100,200)); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(DOCENTI_X+8, y-22, DOCENTI_W-42, ROW_H-4, 8, 8); g2.setStroke(new BasicStroke(1f)); }
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(30,40,100));
            g2.drawString(d.getNome(), DOCENTI_X + 18, y - 6);
            int nAlunni = UserStore.get().getAlunniDiDocente(d.getNome()).size();
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            g2.setColor(new Color(100,120,180));
            g2.drawString(nAlunni + " alunno/i assegnato/i", DOCENTI_X + 18, y + 7);
            y += ROW_H;
        }
        if (tuttiDocenti.isEmpty()) {
            g2.setFont(new Font("Arial", Font.ITALIC, 13)); g2.setColor(new Color(160,160,180));
            g2.drawString("(nessun docente)", DOCENTI_X + 20, DOCENTI_Y + 50);
        }
        g2.setClip(oldClip);

        // Bottoni scroll colonna Alunni (viola, sempre visibili)
        g2.setColor(scrollAlunni > 0 ? new Color(90,50,180) : new Color(190,180,220));
        g2.fillRoundRect(ASSEGNA_X+ASSEGNA_W-26, ASSEGNA_Y+6, 20, 20, 6, 6);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("▲", ASSEGNA_X+ASSEGNA_W-21, ASSEGNA_Y+20);
        g2.setColor(new Color(90,50,180));
        g2.fillRoundRect(ASSEGNA_X+ASSEGNA_W-26, ASSEGNA_Y+ASSEGNA_H-28, 20, 20, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString("▼", ASSEGNA_X+ASSEGNA_W-21, ASSEGNA_Y+ASSEGNA_H-13);

        // Bottoni scroll colonna Docenti (viola, sempre visibili)
        g2.setColor(scrollDocenti > 0 ? new Color(90,50,180) : new Color(190,180,220));
        g2.fillRoundRect(DOCENTI_X+DOCENTI_W-26, DOCENTI_Y+6, 20, 20, 6, 6);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("▲", DOCENTI_X+DOCENTI_W-21, DOCENTI_Y+20);
        g2.setColor(new Color(90,50,180));
        g2.fillRoundRect(DOCENTI_X+DOCENTI_W-26, DOCENTI_Y+DOCENTI_H-28, 20, 20, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString("▼", DOCENTI_X+DOCENTI_W-21, DOCENTI_Y+DOCENTI_H-13);

        // Bottone Assegna (attivo solo se entrambi selezionati)
        boolean abilitato = alunnoSelAssegna != null && docenteSelAssegna != null;
        int btnX = (W - BTN_ASSEGNA_W) / 2;
        int btnY = ASSEGNA_Y + ASSEGNA_H + 18;
        drawBtn(g2, btnX, btnY, BTN_ASSEGNA_W, BTN_ASSEGNA_H,
                abilitato ? new Color(60,100,200) : new Color(180,180,200),
                "Assegna →");

        // Bottone Rimuovi assegnazione
        if (alunnoSelAssegna != null) {
            User a = UserStore.get().getAlunni().stream()
                .filter(u -> u.getNome().equals(alunnoSelAssegna)).findFirst().orElse(null);
            if (a != null && a.hasDocente()) {
                drawBtn(g2, btnX + BTN_ASSEGNA_W + 10, btnY, BTN_ASSEGNA_W, BTN_ASSEGNA_H,
                        new Color(180,60,60), "Rimuovi");
            }
        }

        // Feedback
        if (!feedbackMsg.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(feedbackOk ? new Color(30,140,60) : new Color(190,40,40));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(feedbackMsg, (W - fm.stringWidth(feedbackMsg)) / 2, btnY + BTN_ASSEGNA_H + 22);
        }

        // Legenda
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(new Color(120,120,160));
    }

    // ── Pannello Elimina ──────────────────────────────────────────
    private void drawPannelloElimina(Graphics2D g2) {
        List<User> docenti = UserStore.get().getDocenti();
        List<User> alunni  = UserStore.get().getAlunni();

        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(ELIM_X, ELIM_Y, ELIM_W, ELIM_H, 16, 16);

        Shape oldClip = g2.getClip();
        g2.clipRect(ELIM_X, ELIM_Y + 2, ELIM_W - 35, ELIM_H - 4);

        int startY = ELIM_Y + 10 - scrollElim * ROW_H;
        int y = startY + 24;

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(60,100,200));
        g2.drawString("Docenti (" + docenti.size() + ")", ELIM_X + 14, y);
        y += ROW_H;

        if (docenti.isEmpty()) {
            g2.setFont(new Font("Arial", Font.ITALIC, 13)); g2.setColor(new Color(160,150,180));
            g2.drawString("(nessun docente registrato)", ELIM_X + 20, y); y += ROW_H;
        } else {
            for (User u : docenti) { drawRigaUtente(g2, u, y, new Color(230,240,255), new Color(60,100,200)); y += ROW_H; }
        }

        y += 10;
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(50,150,80));
        g2.drawString("Alunni (" + alunni.size() + ")", ELIM_X + 14, y);
        y += ROW_H;

        if (alunni.isEmpty()) {
            g2.setFont(new Font("Arial", Font.ITALIC, 13)); g2.setColor(new Color(160,180,160));
            g2.drawString("(nessun alunno registrato)", ELIM_X + 20, y);
        } else {
            for (User u : alunni) { drawRigaUtente(g2, u, y, new Color(230,250,235), new Color(50,150,80)); y += ROW_H; }
        }

        g2.setClip(oldClip);

        // Feedback dentro rettangolo in basso
        if (!feedbackMsg.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(feedbackOk ? new Color(30,140,60) : new Color(190,40,40));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(255,255,255,180));
            g2.fillRoundRect(ELIM_X+10, ELIM_Y+ELIM_H-28, ELIM_W-20, 22, 8, 8);
            g2.setColor(feedbackOk ? new Color(30,140,60) : new Color(190,40,40));
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            fm = g2.getFontMetrics();
            g2.drawString(feedbackMsg, (W - fm.stringWidth(feedbackMsg)) / 2, ELIM_Y + ELIM_H - 12);
        }

        int totalItems = docenti.size() + alunni.size() + 4;
        if (totalItems > ELIM_H / ROW_H) {
            g2.setColor(scrollElim > 0 ? new Color(90,50,180) : new Color(190,180,220));
            g2.fillRoundRect(ELIM_X+ELIM_W-28, ELIM_Y+8, 22, 22, 6, 6);
            g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString("▲", ELIM_X+ELIM_W-23, ELIM_Y+23);
            g2.setColor(new Color(90,50,180));
            g2.fillRoundRect(ELIM_X+ELIM_W-28, ELIM_Y+ELIM_H-32, 22, 22, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString("▼", ELIM_X+ELIM_W-23, ELIM_Y+ELIM_H-16);
        }
    }

    private void drawRigaUtente(Graphics2D g2, User u, int y, Color bgColor, Color accentColor) {
        boolean sel = u.getNome().equals(utenteSelezionato);
        g2.setColor(sel ? new Color(255,210,210) : bgColor);
        g2.fillRoundRect(ELIM_X+10, y-28, ELIM_W-50, ROW_H-4, 10, 10);
        if (sel) { g2.setColor(new Color(200,40,40)); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(ELIM_X+10, y-28, ELIM_W-50, ROW_H-4, 10, 10); g2.setStroke(new BasicStroke(1f)); }
        g2.setFont(new Font("Arial", Font.BOLD, 15)); g2.setColor(new Color(40,20,80));
        g2.drawString(u.getNome(), ELIM_X+24, y-10);
        g2.setFont(new Font("Arial", Font.ITALIC, 12)); g2.setColor(accentColor);
        String extra = (u.getRuolo() == User.Ruolo.ALUNNO && u.hasDocente()) ? " → " + u.getDocenteAssegnato() : "";
        g2.drawString(u.getRuolo().name().toLowerCase() + extra, ELIM_X+24, y+4);
        int btnX = ELIM_X+ELIM_W-BTN_ELIM_W-52;
        int btnY = y-26+(ROW_H-4-BTN_ELIM_H)/2;
        g2.setColor(sel ? new Color(170,20,20) : new Color(210,60,60));
        g2.fillRoundRect(btnX, btnY, BTN_ELIM_W, BTN_ELIM_H, 8, 8);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String lbl = sel ? "Conferma" : "Elimina";
        g2.drawString(lbl, btnX+(BTN_ELIM_W-fm.stringWidth(lbl))/2, btnY+BTN_ELIM_H-7);
    }

    // ── Form aggiunta ─────────────────────────────────────────────
    private void drawForm(Graphics2D g2, String tipo) {
        g2.setColor(new Color(255,255,255,190));
        g2.fillRoundRect(FORM_X-20, TAB_Y+TAB_H, FORM_W+40, 280, 16, 16);
        g2.setFont(new Font("Arial", Font.BOLD, 14)); g2.setColor(new Color(70,30,130));
        g2.drawString("Nome " + tipo + ":", FORM_X, NOME_Y-6);
        g2.drawString("Password:", FORM_X, PASS_Y-6);
        drawField(g2, FORM_X, NOME_Y, FORM_W, FIELD_H, nuovoNome, "es. Mario Rossi", campoAttivo==Campo.NOME);
        drawField(g2, FORM_X, PASS_Y, FORM_W, FIELD_H, "*".repeat(nuovaPassword.length()), "password", campoAttivo==Campo.PASSWORD);
        drawBtn(g2, BTN_ADD_X, BTN_ADD_Y, BTN_ADD_W, BTN_ADD_H, new Color(90,50,180),
                "Aggiungi "+tipo.substring(0,1).toUpperCase()+tipo.substring(1));
    }

    private void drawField(Graphics2D g2, int x, int y, int w, int h, String value, String placeholder, boolean focused) {
        g2.setColor(Color.WHITE); g2.fillRoundRect(x,y,w,h,10,10);
        g2.setColor(focused ? new Color(90,50,180) : new Color(200,190,230));
        g2.setStroke(new BasicStroke(focused?2f:1.5f)); g2.drawRoundRect(x,y,w,h,10,10); g2.setStroke(new BasicStroke(1f));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        if (value.isEmpty()) { g2.setColor(new Color(190,180,210)); g2.drawString(placeholder, x+12, y+h/2+5); }
        else { g2.setColor(new Color(40,20,80)); g2.drawString(value, x+12, y+h/2+5); }
        if (focused) { FontMetrics fm=g2.getFontMetrics(); g2.setColor(new Color(90,50,180)); g2.fillRect(x+12+fm.stringWidth(value), y+8, 2, h-16); }
    }

    private void drawTab(Graphics2D g2, int x, int y, int w, int h, String label, boolean attiva) {
        g2.setColor(attiva ? new Color(90,50,180) : new Color(200,190,230));
        g2.fillRoundRect(x,y,w,h,10,10);
        g2.setFont(new Font("Arial", Font.BOLD, 13)); g2.setColor(attiva ? Color.WHITE : new Color(90,70,150));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(label, x+(w-fm.stringWidth(label))/2, y+h/2+fm.getAscent()/2-3);
    }

    private void drawBtn(Graphics2D g2, int x, int y, int w, int h, Color c, String label) {
        g2.setColor(new Color(0,0,0,25)); g2.fillRoundRect(x+2,y+2,w,h,12,12);
        g2.setColor(c); g2.fillRoundRect(x,y,w,h,12,12);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(label, x+(w-fm.stringWidth(label))/2, y+h/2+fm.getAscent()/2-3);
    }

    private void drawLista(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.BOLD, 14)); g2.setColor(new Color(70,30,130));
        g2.drawString("Utenti registrati:", LIST_X, LIST_Y-16);
        List<User> docenti = UserStore.get().getDocenti();
        List<User> alunni  = UserStore.get().getAlunni();
        g2.setColor(new Color(255,255,255,180)); g2.fillRoundRect(LIST_X, LIST_Y, LIST_W, LIST_H, 12, 12);
        Shape oldClip=g2.getClip(); g2.clipRect(LIST_X, LIST_Y, LIST_W, LIST_H);
        int col1=LIST_X+12, col2=LIST_X+LIST_W/2+10, rowH=22;
        int startY=LIST_Y+20-scrollOffset*rowH;
        g2.setFont(new Font("Arial", Font.BOLD, 13)); g2.setColor(new Color(60,100,200));
        if (startY>=LIST_Y&&startY<=LIST_Y+LIST_H) g2.drawString("-- Docenti ("+docenti.size()+") --", col1, startY);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        int y=startY+rowH;
        for (User u : docenti) {
            if (y>=LIST_Y+5&&y<=LIST_Y+LIST_H-5) { g2.setColor(new Color(230,240,255)); g2.fillRoundRect(col1-4,y-14,LIST_W/2-20,18,6,6); g2.setColor(new Color(40,40,80)); g2.drawString(u.getNome(),col1,y); }
            y+=rowH;
        }
        if (docenti.isEmpty()) { g2.setFont(new Font("Arial",Font.ITALIC,12)); g2.setColor(new Color(160,150,180)); if(startY+rowH>=LIST_Y&&startY+rowH<=LIST_Y+LIST_H) g2.drawString("(nessun docente)",col1,startY+rowH); }
        g2.setFont(new Font("Arial", Font.BOLD, 13)); g2.setColor(new Color(50,150,80));
        if (startY>=LIST_Y&&startY<=LIST_Y+LIST_H) g2.drawString("-- Alunni ("+alunni.size()+") --", col2, startY);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        y=startY+rowH;
        for (User u : alunni) {
            if (y>=LIST_Y+5&&y<=LIST_Y+LIST_H-5) { g2.setColor(new Color(230,250,235)); g2.fillRoundRect(col2-4,y-14,LIST_W/2-20,18,6,6); g2.setColor(new Color(40,40,80)); g2.drawString(u.getNome(),col2,y); }
            y+=rowH;
        }
        if (alunni.isEmpty()) { g2.setFont(new Font("Arial",Font.ITALIC,12)); g2.setColor(new Color(160,180,160)); if(startY+rowH>=LIST_Y&&startY+rowH<=LIST_Y+LIST_H) g2.drawString("(nessun alunno)",col2,startY+rowH); }
        g2.setClip(oldClip);

        // Bottoni scroll lista utenti
        int totalRows = docenti.size() + alunni.size() + 2;
        if (totalRows > LIST_H / rowH) {
            g2.setColor(scrollOffset > 0 ? new Color(90,50,180) : new Color(190,180,220));
            g2.fillRoundRect(LIST_X+LIST_W-28, LIST_Y+4, 22, 22, 6, 6);
            g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString("▲", LIST_X+LIST_W-23, LIST_Y+19);
            g2.setColor(new Color(90,50,180));
            g2.fillRoundRect(LIST_X+LIST_W-28, LIST_Y+LIST_H-28, 22, 22, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString("▼", LIST_X+LIST_W-23, LIST_Y+LIST_H-12);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm=g2.getFontMetrics(); g2.drawString(text, (W-fm.stringWidth(text))/2, y);
    }

    // ── Click ─────────────────────────────────────────────────────
    private void handleClick(int mx, int my) {
        if (inRect(mx,my,BTN_OUT_X,BTN_OUT_Y,BTN_OUT_W,BTN_OUT_H)) { onLogout.run(); return; }
        if (inRect(mx,my,TAB_CRED_X,TAB_CRED_Y,TAB_CRED_W,TAB_H))      { sezione=SezioneAttiva.CREDENZIALI; nuovoNomePreside=""; nuovaPassPreside=""; confermaPassPreside=""; feedbackMsg=""; campoCredenziali=CampoCredenziali.NOME; repaint(); return; }
        if (inRect(mx,my,TAB_DOC_X,TAB_Y,TAB_DOC_W,TAB_H))         { sezione=SezioneAttiva.DOCENTE;  nuovoNome=""; nuovaPassword=""; feedbackMsg=""; campoAttivo=Campo.NOME; repaint(); return; }
        if (inRect(mx,my,TAB_ALU_X,TAB_Y,TAB_ALU_W,TAB_H))         { sezione=SezioneAttiva.ALUNNO;   nuovoNome=""; nuovaPassword=""; feedbackMsg=""; campoAttivo=Campo.NOME; repaint(); return; }
        if (inRect(mx,my,TAB_ELIM_X,TAB_Y,TAB_ELIM_W,TAB_H))       { sezione=SezioneAttiva.ELIMINA;  feedbackMsg=""; utenteSelezionato=null; repaint(); return; }
        if (inRect(mx,my,TAB_ASSEGNA_X,TAB_Y,TAB_ASSEGNA_W,TAB_H)) { sezione=SezioneAttiva.ASSEGNA;  feedbackMsg=""; alunnoSelAssegna=null; docenteSelAssegna=null; repaint(); return; }

        if (sezione==SezioneAttiva.CREDENZIALI) { handleClickCredenziali(mx,my); return; }
        if (sezione==SezioneAttiva.ELIMINA)  { handleClickElimina(mx,my); return; }
        if (sezione==SezioneAttiva.ASSEGNA)  { handleClickAssegna(mx,my); return; }
        if (inRect(mx,my,FORM_X,NOME_Y,FORM_W,FIELD_H))              { campoAttivo=Campo.NOME;     repaint(); return; }
        if (inRect(mx,my,FORM_X,PASS_Y,FORM_W,FIELD_H))              { campoAttivo=Campo.PASSWORD; repaint(); return; }
        if (inRect(mx,my,BTN_ADD_X,BTN_ADD_Y,BTN_ADD_W,BTN_ADD_H))  { doAggiungi(); return; }
        // Bottoni scroll lista utenti
        if (inRect(mx,my,LIST_X+LIST_W-28,LIST_Y+4,22,22))            { if(scrollOffset>0){scrollOffset--;repaint();} return; }
        if (inRect(mx,my,LIST_X+LIST_W-28,LIST_Y+LIST_H-28,22,22))   { scrollOffset++; repaint(); return; }
    }

    private void handleClickCredenziali(int mx, int my) {
        int panX = 160, panY = 110, panW = 480;
        int fx = panX + 30, fw = panW - 60, fh = 36;
        int y1 = panY + 60, y2 = y1 + 56, y3 = y2 + 56;
        int btnY = y3 + fh + 18;

        if (inRect(mx,my,fx,y1,fw,fh)) { campoCredenziali=CampoCredenziali.NOME; repaint(); return; }
        if (inRect(mx,my,fx,y2,fw,fh)) { campoCredenziali=CampoCredenziali.PASS; repaint(); return; }
        if (inRect(mx,my,fx,y3,fw,fh)) { campoCredenziali=CampoCredenziali.CONFERMA; repaint(); return; }
        if (inRect(mx,my,fx,btnY,fw,40)) { doSalvaCredenziali(); }
    }

    private void doSalvaCredenziali() {
        if (nuovoNomePreside.trim().isEmpty()) { feedbackMsg="Inserisci un nome utente!"; feedbackOk=false; repaint(); return; }
        if (nuovaPassPreside.isEmpty())        { feedbackMsg="Inserisci una password!"; feedbackOk=false; repaint(); return; }
        if (!nuovaPassPreside.equals(confermaPassPreside)) { feedbackMsg="Le password non coincidono!"; feedbackOk=false; repaint(); return; }
        boolean ok = UserStore.get().cambiaCredenzialiPreside(nuovoNomePreside.trim(), nuovaPassPreside);
        feedbackMsg = ok ? "Credenziali aggiornate con successo!" : "Nome già in uso da un altro utente.";
        feedbackOk = ok;
        if (ok) { nuovoNomePreside=""; nuovaPassPreside=""; confermaPassPreside=""; campoCredenziali=CampoCredenziali.NOME; }
        repaint();
    }

    private void handleClickAssegna(int mx, int my) {
        List<User> alunni   = UserStore.get().getAlunni();
        List<User> docenti  = UserStore.get().getDocenti();

        // Bottoni scroll colonna Alunni — prima delle righe per evitare sovrapposizioni
        if (inRect(mx, my, ASSEGNA_X+ASSEGNA_W-26, ASSEGNA_Y+6, 20, 20))          { if(scrollAlunni>0){scrollAlunni--;repaint();} return; }
        if (inRect(mx, my, ASSEGNA_X+ASSEGNA_W-26, ASSEGNA_Y+ASSEGNA_H-28, 20, 20)) { scrollAlunni++; repaint(); return; }

        // Bottoni scroll colonna Docenti — prima delle righe per evitare sovrapposizioni
        if (inRect(mx, my, DOCENTI_X+DOCENTI_W-26, DOCENTI_Y+6, 20, 20))           { if(scrollDocenti>0){scrollDocenti--;repaint();} return; }
        if (inRect(mx, my, DOCENTI_X+DOCENTI_W-26, DOCENTI_Y+DOCENTI_H-28, 20, 20)) { scrollDocenti++; repaint(); return; }

        // Click su alunno — solo se dentro il pannello e non nella zona bottoni scroll
        int rowAlunniW = ASSEGNA_W - 16 - 30; // lascia spazio ai bottoni scroll a destra
        if (inRect(mx, my, ASSEGNA_X, ASSEGNA_Y, ASSEGNA_W - 28, ASSEGNA_H)) {
            int y = ASSEGNA_Y + 30 - scrollAlunni * ROW_H;
            for (User a : alunni) {
                if (inRect(mx, my, ASSEGNA_X+8, y-22, rowAlunniW, ROW_H-4)) {
                    alunnoSelAssegna = a.getNome().equals(alunnoSelAssegna) ? null : a.getNome();
                    feedbackMsg = ""; repaint(); return;
                }
                y += ROW_H;
            }
        }

        // Click su docente — solo se dentro il pannello e non nella zona bottoni scroll
        int rowDocentiW = DOCENTI_W - 16 - 30;
        if (inRect(mx, my, DOCENTI_X, DOCENTI_Y, DOCENTI_W - 28, DOCENTI_H)) {
            int y = DOCENTI_Y + 30 - scrollDocenti * ROW_H;
            for (User d : docenti) {
                if (inRect(mx, my, DOCENTI_X+8, y-22, rowDocentiW, ROW_H-4)) {
                    docenteSelAssegna = d.getNome().equals(docenteSelAssegna) ? null : d.getNome();
                    feedbackMsg = ""; repaint(); return;
                }
                y += ROW_H;
            }
        }

        // Bottone Assegna
        int btnX = (W - BTN_ASSEGNA_W) / 2;
        int btnY = ASSEGNA_Y + ASSEGNA_H + 18;
        if (inRect(mx, my, btnX, btnY, BTN_ASSEGNA_W, BTN_ASSEGNA_H)) {
            if (alunnoSelAssegna != null && docenteSelAssegna != null) {
                boolean ok = UserStore.get().assegnaAlunnoADocente(alunnoSelAssegna, docenteSelAssegna);
                feedbackMsg = ok
                    ? "\"" + alunnoSelAssegna + "\" assegnato a \"" + docenteSelAssegna + "\"!"
                    : "Errore durante l'assegnazione.";
                feedbackOk = ok;
                if (ok) { alunnoSelAssegna = null; docenteSelAssegna = null; }
            } else {
                feedbackMsg = "Seleziona prima un alunno e un docente.";
                feedbackOk  = false;
            }
            repaint(); return;
        }

        // Bottone Rimuovi
        if (alunnoSelAssegna != null) {
            User a = alunni.stream().filter(u -> u.getNome().equals(alunnoSelAssegna)).findFirst().orElse(null);
            if (a != null && a.hasDocente()) {
                int rimuoviX = btnX + BTN_ASSEGNA_W + 10;
                if (inRect(mx, my, rimuoviX, btnY, BTN_ASSEGNA_W, BTN_ASSEGNA_H)) {
                    boolean ok = UserStore.get().assegnaAlunnoADocente(alunnoSelAssegna, null);
                    feedbackMsg = ok ? "Assegnazione rimossa." : "Errore.";
                    feedbackOk  = ok;
                    if (ok) alunnoSelAssegna = null;
                    repaint(); return;
                }
            }
        }
    }

    private void handleClickElimina(int mx, int my) {
        List<User> docenti = UserStore.get().getDocenti();
        List<User> alunni  = UserStore.get().getAlunni();
        int startY = ELIM_Y+10-scrollElim*ROW_H;
        int y = startY+24+ROW_H;
        for (User u : docenti) {
            int btnX=ELIM_X+ELIM_W-BTN_ELIM_W-52, btnY=y-26+(ROW_H-4-BTN_ELIM_H)/2;
            if (inRect(mx,my,btnX,btnY,BTN_ELIM_W,BTN_ELIM_H)) { handleClickBottoneElimina(u.getNome()); return; }
            y+=ROW_H;
        }
        if (docenti.isEmpty()) y+=ROW_H;
        y+=10+ROW_H;
        for (User u : alunni) {
            int btnX=ELIM_X+ELIM_W-BTN_ELIM_W-52, btnY=y-26+(ROW_H-4-BTN_ELIM_H)/2;
            if (inRect(mx,my,btnX,btnY,BTN_ELIM_W,BTN_ELIM_H)) { handleClickBottoneElimina(u.getNome()); return; }
            y+=ROW_H;
        }
        if (inRect(mx,my,ELIM_X+ELIM_W-28,ELIM_Y+8,22,22)) { if(scrollElim>0){scrollElim--;repaint();} return; }
        if (inRect(mx,my,ELIM_X+ELIM_W-28,ELIM_Y+ELIM_H-32,22,22)) { scrollElim++; repaint(); return; }
        utenteSelezionato=null; repaint();
    }

    private void handleClickBottoneElimina(String nome) {
        if (nome.equals(utenteSelezionato)) {
            boolean ok = UserStore.get().eliminaUtente(nome);
            feedbackMsg = ok ? "\""+nome+"\" eliminato." : "Errore durante l'eliminazione.";
            feedbackOk=ok; utenteSelezionato=null;
        } else {
            utenteSelezionato=nome; feedbackMsg="";
        }
        repaint();
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx>=x && mx<=x+w && my>=y && my<=y+h;
    }

    private void doAggiungi() {
        if (nuovoNome.trim().isEmpty()) { feedbackMsg="Inserisci un nome!"; feedbackOk=false; repaint(); return; }
        if (nuovaPassword.isEmpty())    { feedbackMsg="Inserisci una password!"; feedbackOk=false; repaint(); return; }
        boolean ok;
        if (sezione==SezioneAttiva.DOCENTE) {
            ok=UserStore.get().aggiungiDocente(nuovoNome,nuovaPassword);
            feedbackMsg=ok?"Docente \""+nuovoNome+"\" aggiunto!":"Nome già in uso.";
        } else {
            ok=UserStore.get().aggiungiAlunno(nuovoNome,nuovaPassword);
            feedbackMsg=ok?"Alunno \""+nuovoNome+"\" aggiunto!":"Nome già in uso.";
        }
        feedbackOk=ok;
        if (ok) { nuovoNome=""; nuovaPassword=""; campoAttivo=Campo.NOME; }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (sezione==SezioneAttiva.CREDENZIALI) {
            char c=e.getKeyChar();
            if (c=='\t') { campoCredenziali = campoCredenziali==CampoCredenziali.NOME ? CampoCredenziali.PASS : campoCredenziali==CampoCredenziali.PASS ? CampoCredenziali.CONFERMA : CampoCredenziali.NOME; repaint(); return; }
            if (c=='\n') { doSalvaCredenziali(); return; }
            if (c=='\b') return;
            switch(campoCredenziali) {
                case NOME:     if(nuovoNomePreside.length()<30) nuovoNomePreside+=c; break;
                case PASS:     if(nuovaPassPreside.length()<20) nuovaPassPreside+=c; break;
                case CONFERMA: if(confermaPassPreside.length()<20) confermaPassPreside+=c; break;
            }
            feedbackMsg=""; repaint();
            return;
        }
        if (sezione==SezioneAttiva.ELIMINA||sezione==SezioneAttiva.ASSEGNA) return;
        char c=e.getKeyChar();
        if (c=='\t') { campoAttivo=(campoAttivo==Campo.NOME)?Campo.PASSWORD:Campo.NOME; repaint(); return; }
        if (c=='\n') { doAggiungi(); return; }
        if (c=='\b') return;
        if (campoAttivo==Campo.NOME) { if(nuovoNome.length()<30) nuovoNome+=c; }
        else { if(nuovaPassword.length()<20) nuovaPassword+=c; }
        feedbackMsg=""; repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (sezione==SezioneAttiva.CREDENZIALI && e.getKeyCode()==KeyEvent.VK_BACK_SPACE) {
            switch(campoCredenziali) {
                case NOME:     if(!nuovoNomePreside.isEmpty()) nuovoNomePreside=nuovoNomePreside.substring(0,nuovoNomePreside.length()-1); break;
                case PASS:     if(!nuovaPassPreside.isEmpty()) nuovaPassPreside=nuovaPassPreside.substring(0,nuovaPassPreside.length()-1); break;
                case CONFERMA: if(!confermaPassPreside.isEmpty()) confermaPassPreside=confermaPassPreside.substring(0,confermaPassPreside.length()-1); break;
            }
            repaint(); return;
        }
        if (e.getKeyCode()==KeyEvent.VK_BACK_SPACE && sezione!=SezioneAttiva.ELIMINA && sezione!=SezioneAttiva.ASSEGNA) {
            if (campoAttivo==Campo.NOME) { if(!nuovoNome.isEmpty()) nuovoNome=nuovoNome.substring(0,nuovoNome.length()-1); }
            else { if(!nuovaPassword.isEmpty()) nuovaPassword=nuovaPassword.substring(0,nuovaPassword.length()-1); }
            repaint();
        }
        if (e.getKeyCode()==KeyEvent.VK_DOWN) { if(sezione==SezioneAttiva.ELIMINA) scrollElim++; else if(sezione!=SezioneAttiva.ASSEGNA) scrollOffset++; repaint(); }
        if (e.getKeyCode()==KeyEvent.VK_UP)   { if(sezione==SezioneAttiva.ELIMINA){if(scrollElim>0)scrollElim--;} else if(sezione!=SezioneAttiva.ASSEGNA){if(scrollOffset>0)scrollOffset--;} repaint(); }
    }

    @Override public void keyReleased(KeyEvent e) {}
}