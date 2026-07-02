import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * GameServer - avviato dal docente.
 * Protocollo messaggi:
 *   Client → Server:  LOGIN:nomeAlunno
 *                     SCORE:punti:vite
 *   Server → Client:  START:OPERAZIONE:numOggetti:velocita:numDomande:tempoSecondi
 *                     STOP
 */
public class GameServer {

    public static final int PORT = 12345;

    public enum Operazione { ADDIZIONE, SOTTRAZIONE, MOLTIPLICAZIONE, DIVISIONE }

    public static class AlunnoStatus {
        public final String  nome;
        public final int     score;
        public final int     vite;
        public final boolean connesso;
        public final int     tempoRimasto;
        public final int     domandeRisposte;
        public final String  domandaCorrente;
        public final String  frameBase64; // JPEG compresso, può essere null

        public AlunnoStatus(String nome, int score, int vite, boolean connesso,
                            int tempoRimasto, int domandeRisposte, String domandaCorrente,
                            String frameBase64) {
            this.nome            = nome;
            this.score           = score;
            this.vite            = vite;
            this.connesso        = connesso;
            this.tempoRimasto    = tempoRimasto;
            this.domandeRisposte = domandeRisposte;
            this.domandaCorrente = domandaCorrente;
            this.frameBase64     = frameBase64;
        }
    }

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    private volatile Operazione currentOperazione = null;

    // Parametri di configurazione salvati al momento di startGame()
    private volatile int cfgNumOggetti    = 10;
    private volatile int cfgVelocita      = 1;
    private volatile int cfgNumDomande    = 10;
    private volatile int cfgTempoSecondi  = 60;

    private final Consumer<List<AlunnoStatus>> onStatusUpdate;
    private UdpFrameReceiver udpReceiver;

    public GameServer(Consumer<List<AlunnoStatus>> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    public void start() {
        running = true;

        // Avvia receiver UDP per i frame live
        udpReceiver = new UdpFrameReceiver((nomeAlunno, jpegBytes) -> {
            String b64 = java.util.Base64.getEncoder().encodeToString(jpegBytes);
            for (ClientHandler ch : clients) {
                if (nomeAlunno.equals(ch.nome)) {
                    ch.frameBase64 = b64;
                    break;
                }
            }
        });
        udpReceiver.start();

        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new java.net.InetSocketAddress(PORT));
                System.out.println("[Server] In ascolto sulla porta " + PORT);
                while (running) {
                    try {
                        Socket s = serverSocket.accept();
                        ClientHandler ch = new ClientHandler(s);
                        clients.add(ch);
                        ch.start();
                    } catch (IOException e) {
                        if (running) System.err.println("[Server] Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[Server] Impossibile avviare sulla porta " + PORT + ": " + e.getMessage());
                running = false;
            }
        }, "GameServer-Accept");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Avvia il gioco inviando a tutti gli alunni l'operazione
     * e tutti i parametri di configurazione scelti dal docente.
     */
    public void startGame(Operazione op) {
        currentOperazione = op;

        // Legge la config dal singleton (già impostata da GameWindow prima di chiamare startGame)
        GameConfig cfg = GameConfig.get();
        cfgNumOggetti   = cfg.getNumOggetti();
        cfgVelocita     = cfg.getDifficulty();
        cfgNumDomande   = cfg.getNumDomande();
        cfgTempoSecondi = cfg.getSessionDuration();

        String msg = buildStartMsg(op);
        for (ClientHandler ch : clients) ch.send(msg);

        System.out.println("[Server] Gioco avviato: " + op
            + " | oggetti=" + cfgNumOggetti
            + " vel=" + cfgVelocita
            + " domande=" + cfgNumDomande
            + " tempo=" + cfgTempoSecondi + "s");
    }

    private String buildStartMsg(Operazione op) {
        return "START:" + op.name()
             + ":" + cfgNumOggetti
             + ":" + cfgVelocita
             + ":" + cfgNumDomande
             + ":" + cfgTempoSecondi;
    }

    public void stopGame() {
        currentOperazione = null;
        for (ClientHandler ch : clients) ch.send("STOP");
    }

    public void stop() {
        running = false;
        for (ClientHandler ch : clients) ch.send("STOP");
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        for (ClientHandler ch : clients) ch.close();
        clients.clear();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (udpReceiver != null) udpReceiver.stop();
    }

    public int getClientCount() { return clients.size(); }

    /** Restituisce uno snapshot dello stato di tutti gli alunni connessi. */
    public List<AlunnoStatus> getAllStatus() {
        List<AlunnoStatus> snapshot = new ArrayList<>();
        for (ClientHandler ch : clients)
            snapshot.add(new AlunnoStatus(ch.nome, ch.score, ch.vite, true,
                                          ch.tempoRimasto, ch.domandeRisposte, ch.domandaCorrente,
                                          ch.frameBase64));
        return snapshot;
    }

    private void notifyUpdate() {
        List<AlunnoStatus> snapshot = new ArrayList<>();
        for (ClientHandler ch : clients)
            snapshot.add(new AlunnoStatus(ch.nome, ch.score, ch.vite, true,
                                          ch.tempoRimasto, ch.domandeRisposte, ch.domandaCorrente,
                                          ch.frameBase64));
        if (onStatusUpdate != null)
            javax.swing.SwingUtilities.invokeLater(() -> onStatusUpdate.accept(snapshot));
    }

    private class ClientHandler extends Thread {
        final Socket socket;
        PrintWriter out;

        volatile String nome             = "?";
        volatile int    score            = 0;
        volatile int    vite             = 3;
        volatile int    tempoRimasto     = 0;
        volatile int    domandeRisposte  = 0;
        volatile String domandaCorrente  = "";
        volatile String frameBase64      = null;

        ClientHandler(Socket socket) {
            this.socket = socket;
            setDaemon(true);
            try {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            } catch (IOException e) {
                System.err.println("[Server] Errore output: " + e.getMessage());
            }
        }

        void send(String msg) { if (out != null) out.println(msg); }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"), 524288)) { // 512KB per frame JPEG
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line.trim());
                }
            } catch (IOException ignored) {
            } finally {
                clients.remove(this);
                System.out.println("[Server] " + nome + " disconnesso.");
                notifyUpdate();
            }
        }

        private void handleMessage(String msg) {
            if (msg.startsWith("LOGIN:")) {
                nome = msg.substring(6).trim();
                System.out.println("[Server] Alunno loggato: " + nome);
                // Se il gioco è già in corso, manda START completo subito
                if (currentOperazione != null) {
                    send(buildStartMsg(currentOperazione));
                }
                notifyUpdate();
            } else if (msg.startsWith("SCORE:")) {
                String[] parts = msg.split(":");
                if (parts.length >= 3) {
                    try {
                        score = Integer.parseInt(parts[1]);
                        vite  = Integer.parseInt(parts[2]);
                        if (parts.length >= 6) {
                            tempoRimasto    = Integer.parseInt(parts[3]);
                            domandeRisposte = Integer.parseInt(parts[4]);
                            domandaCorrente = parts[5];
                        }
                        notifyUpdate();
                    } catch (NumberFormatException ignored) {}
                }
            } else if (msg.startsWith("FRAME:")) {
                // I frame ora arrivano via UDP - ignora quelli TCP residui
            }
        }
    }
}
