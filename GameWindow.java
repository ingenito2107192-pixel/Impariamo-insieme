import java.awt.*;
import javax.swing.*;

/**
 * GameWindow
 * ----------
 * Flusso docente: Login → TeacherImagePanel → GameSettingsPanel
 *                 → avvia GameServer → MonitorPanel (monitoraggio live)
 *
 * Flusso alunno:  Login → StudentPanel → WaitingPanel (GameClient si connette)
 *                 → GamePanel (quando il docente avvia)
 *
 * Fix thread:
 * - gameClient.disconnect() chiamato in ogni punto di uscita dell'alunno
 * - gameServer.stop() chiamato in ogni punto di uscita del docente
 * - onDisconnect del client ignorato se l'alunno è già nel gioco o è tornato al login
 * - markPlaying() chiamato prima di mostrare il GamePanel
 */
public class GameWindow extends JFrame {

    private static final int W = 800;
    private static final int H = 600;

    private final CardLayout cards = new CardLayout();
    private final JPanel     root  = new JPanel(cards);

    private static final String CARD_START     = "start";
    private static final String CARD_LOGIN     = "login";
    private static final String CARD_STUDENT   = "student";
    private static final String CARD_TEACHER   = "teacher";
    private static final String CARD_SETTINGS  = "settings";
    private static final String CARD_PRINCIPAL = "principal";
    private static final String CARD_WAITING   = "waiting";
    private static final String CARD_GAME      = "game";
    private static final String CARD_MONITOR   = "monitor";
    private static final String CARD_EXTRA     = "extra";
    private static final String CARD_SOLO_GAME = "sologame";

    private LoginPanel        loginPanel;
    private TeacherImagePanel teacherPanel;
    private GameSettingsPanel settingsPanel;
    private MonitorPanel      monitorPanel;
    private GameServer        gameServer;
    private GameClient        gameClient;

    private String nomeUtenteCorrente = "";

    public GameWindow() {
        setTitle("Impariamo insieme");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        root.setPreferredSize(new Dimension(W, H));

        // ── Preside ───────────────────────────────────────────────
        PrincipalPanel principalPanel = new PrincipalPanel(() -> {
            loginPanel.reset();
            showCard(CARD_LOGIN);
        });

        // ── Monitor docente ───────────────────────────────────────
        monitorPanel = new MonitorPanel(() -> {
            stopServer();
            showCard(CARD_TEACHER);
        });

        // ── GamePanel alunno ──────────────────────────────────────
        final StudentPanel[] studentPanelRef = new StudentPanel[1];
        GamePanel gamePanel = new GamePanel(() -> {
            disconnectClient();
            if (studentPanelRef[0] != null) studentPanelRef[0].setSessioneCompletata();
            showCard(CARD_STUDENT);
        });

        // ── GamePanel gioco autonomo (livelli extra) ──────────────
        GamePanel soloGamePanel = new GamePanel(() -> {
            showCard(CARD_STUDENT);
        });

        // ── ExtraLevelsPanel ──────────────────────────────────────
        ExtraLevelsPanel extraPanel = new ExtraLevelsPanel(
            () -> showCard(CARD_STUDENT),
            op -> {
                // Mostra le impostazioni prima di avviare il gioco autonomo
                GameSettingsPanel settingsExtra = new GameSettingsPanel(
                    op,
                    (opIdx, settings) -> {
                        GameConfig cfg = GameConfig.get();
                        cfg.setOperation(opIdx);
                        cfg.setNumOggetti(settings[0]);
                        cfg.setDifficulty(settings[1]);
                        cfg.setNumDomande(settings[2]);
                        cfg.setSessionDuration(settings[3]);
                        cfg.setInitialLives(3);
                        cfg.setShowFilastrocca(false);
                        soloGamePanel.setGameClient(null);
                        soloGamePanel.launchGame();
                        root.remove(root.getComponent(root.getComponentCount() - 1));
                        showCard(CARD_SOLO_GAME);
                    },
                    () -> showCard(CARD_EXTRA)
                );
                root.add(settingsExtra, "settingsExtra");
                showCard("settingsExtra");
            }
        );

        // ── WaitingPanel ──────────────────────────────────────────
        WaitingPanel waitingPanel = new WaitingPanel(() -> {
            // L'alunno ha premuto "Torna indietro": disconnetti ma rimani loggato
            disconnectClient();
            showCard(CARD_STUDENT);
        });

        // ── TeacherImagePanel ─────────────────────────────────────
        teacherPanel = new TeacherImagePanel(
            (int op) -> {
                if (settingsPanel != null) root.remove(settingsPanel);
                settingsPanel = new GameSettingsPanel(
                    op,
                    (opIndex, settings) -> {
                        GameConfig cfg = GameConfig.get();
                        cfg.setOperation(opIndex);
                        cfg.setNumOggetti(settings[0]);
                        cfg.setDifficulty(settings[1]);
                        cfg.setNumDomande(settings[2]);
                        cfg.setSessionDuration(settings[3]);

                        stopServer();

                        gameServer = new GameServer(alunniConnessi ->
                            monitorPanel.aggiornaAlunni(alunniConnessi));
                        gameServer.start();

                        GameServer.Operazione opEnum = GameServer.Operazione.values()[opIndex];

                        // NON avviare subito — aspetta che il docente prema "Avvia partita" nel monitor
                        monitorPanel.setOnStart(() -> gameServer.startGame(opEnum));
                        monitorPanel.setNomeDocente(nomeUtenteCorrente);
                        monitorPanel.setGameServer(gameServer);
                        monitorPanel.resetStarted();
                        showCard(CARD_MONITOR);
                        monitorPanel.avvia();
                    },
                    () -> showCard(CARD_TEACHER)
                );
                root.add(settingsPanel, CARD_SETTINGS);
                showCard(CARD_SETTINGS);
            },
            () -> {
                // Logout docente: ferma server se attivo
                stopServer();
                loginPanel.reset();
                showCard(CARD_LOGIN);
            }
        );

        // ── Login ─────────────────────────────────────────────────
        loginPanel = new LoginPanel(
            // ALUNNO
            () -> {
                nomeUtenteCorrente = loginPanel.getNomeLoggato();
                showCard(CARD_STUDENT);
            },
            // DOCENTE
            () -> {
                nomeUtenteCorrente = loginPanel.getNomeLoggato();
                teacherPanel.setNomeDocente(nomeUtenteCorrente);
                showCard(CARD_TEACHER);
            },
            // PRESIDE
            () -> {
                principalPanel.reset();
                showCard(CARD_PRINCIPAL);
                principalPanel.requestFocusInWindow();
            },
            // TORNA INDIETRO → StartPanel
            () -> showCard(CARD_START)
        );

        // ── StudentPanel ──────────────────────────────────────────
        StudentPanel studentPanel = new StudentPanel(
            waitingPanel,
            // onPlayClicked
            () -> {
                // Disconnetti client precedente se ancora attivo
                disconnectClient();

                gameClient = new GameClient(
                    nomeUtenteCorrente,
                    // START ricevuto dal server
                    op -> {
                        waitingPanel.stop();
                        GameConfig.get().setOperation(op.ordinal());
                        gamePanel.setGameClient(gameClient);
                        gameClient.markPlaying();   // impedisce callback indesiderate
                        gamePanel.launchGame();
                        showCard(CARD_GAME);
                    },
                    // aggiornamento stato connessione
                    status -> {
                        // Aggiorna solo se siamo ancora nella WaitingPanel
                        if (gameClient != null &&
                            gameClient.getState() != GameClient.State.PLAYING &&
                            gameClient.getState() != GameClient.State.DISCONNECTED) {
                            waitingPanel.setStatus(status);
                        }
                    },
                    // onDisconnect: chiamato solo se il server cade mentre l'alunno aspetta
                    () -> {
                        waitingPanel.stop();
                        waitingPanel.setStatus("Il docente si è disconnesso. Torna indietro per riprovare.");
                    },
                    // onStop: docente ha terminato la sessione, rimanda l'alunno alla schermata iniziale
                    () -> {
                        waitingPanel.stop();
                        gamePanel.stopGame();
                        disconnectClient();
                        showCard(CARD_STUDENT);
                    }
                );

                waitingPanel.start();
                showCard(CARD_WAITING);
                gameClient.connectAsync();
            },
            () -> {},   // onGameStart non usato qui
            () -> {
                // Logout dalla schermata alunno (prima di premere Play)
                disconnectClient();
                if (studentPanelRef[0] != null) studentPanelRef[0].reset();
                loginPanel.reset();
                showCard(CARD_LOGIN);
            },
            () -> showCard(CARD_EXTRA)
        );
        studentPanelRef[0] = studentPanel;

        // ── StartPanel ────────────────────────────────────────────
        StartPanel startPanel = new StartPanel(() -> {
            loginPanel.reset();
            showCard(CARD_LOGIN);
            loginPanel.requestFocusInWindow();
        });

        root.add(startPanel,     CARD_START);
        root.add(loginPanel,     CARD_LOGIN);
        root.add(waitingPanel,   CARD_WAITING);
        root.add(studentPanel,   CARD_STUDENT);
        root.add(extraPanel,     CARD_EXTRA);
        root.add(soloGamePanel,  CARD_SOLO_GAME);
        root.add(teacherPanel,   CARD_TEACHER);
        root.add(principalPanel, CARD_PRINCIPAL);
        root.add(gamePanel,      CARD_GAME);
        root.add(monitorPanel,   CARD_MONITOR);

        add(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        showCard(CARD_START);
        startPanel.requestFocusInWindow();
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** Disconnette e annulla il client corrente in modo sicuro. */
    private void disconnectClient() {
        if (gameClient != null) {
            gameClient.disconnect();
            gameClient = null;
        }
    }

    /** Ferma il server corrente e il timer del monitor in modo sicuro. */
    private void stopServer() {
        if (monitorPanel != null) monitorPanel.ferma();
        if (monitorPanel != null) monitorPanel.setGameServer(null);
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
    }

    private void showCard(String name) {
        cards.show(root, name);
        for (Component c : root.getComponents()) {
            if (c.isVisible()) { c.requestFocusInWindow(); break; }
        }
    }
}
