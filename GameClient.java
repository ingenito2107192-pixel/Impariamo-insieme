import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * GameClient - avviato dall'alunno dopo aver cliccato Play.
 * Protocollo:
 *   → LOGIN:nomeAlunno
 *   → SCORE:punti:vite
 *   ← START:OPERAZIONE:numOggetti:velocita:numDomande:tempoSecondi
 *   ← STOP
 */
public class GameClient {

    public static final String HOST = "localhost";
    public static final int    PORT = 12345;

    private static final int MAX_RETRIES    = 30;
    private static final int RETRY_DELAY_MS = 1000;

    private Socket           socket;
    private PrintWriter      out;
    private volatile boolean running   = false;
    private volatile boolean cancelled = false;

    public enum State { CONNECTING, WAITING, PLAYING, DISCONNECTED }
    private volatile State state = State.DISCONNECTED;

    private final String                          nomeAlunno;
    private final Consumer<GameServer.Operazione> onGameStart;
    private final Consumer<String>                onStatus;
    private final Runnable                        onDisconnect;
    private final Runnable                        onStop;

    public GameClient(String nomeAlunno,
                      Consumer<GameServer.Operazione> onGameStart,
                      Consumer<String> onStatus,
                      Runnable onDisconnect,
                      Runnable onStop) {
        this.nomeAlunno   = nomeAlunno;
        this.onGameStart  = onGameStart;
        this.onStatus     = onStatus;
        this.onDisconnect = onDisconnect;
        this.onStop       = onStop;
    }

    public String getNomeAlunno() { return nomeAlunno; }
    public State  getState()      { return state; }

    public void connectAsync() {
        cancelled = false;
        state     = State.CONNECTING;

        Thread t = new Thread(() -> {
            notifyStatus("Connessione al docente in corso...");

            int attempt = 0;
            while (!cancelled) {
                attempt++;
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(HOST, PORT), 2000);

                    if (cancelled) {
                        try { s.close(); } catch (IOException ignored) {}
                        state = State.DISCONNECTED;
                        return;
                    }

                    socket  = s;
                    out     = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                    running = true;
                    state   = State.WAITING;

                    out.println("LOGIN:" + nomeAlunno);
                    notifyStatus("Connesso! In attesa che il docente avvii il gioco...");
                    listen();
                    return;

                } catch (IOException e) {
                    if (cancelled) { state = State.DISCONNECTED; return; }
                    final int att = attempt;
                    notifyStatus("In attesa del docente... (tentativo " + att + ")");
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) {
                        state = State.DISCONNECTED;
                        return;
                    }
                }
            }
            state = State.DISCONNECTED;
        }, "GameClient-Connect");
        t.setDaemon(true);
        t.start();
    }

    public void sendFrame(String base64jpeg) {
        if (out != null && running && state == State.PLAYING)
            out.println("FRAME:" + base64jpeg);
    }

    public void sendScore(int score, int vite) {
        if (out != null && running && state == State.PLAYING)
            out.println("SCORE:" + score + ":" + vite);
    }

    public void sendScoreFull(int score, int vite, int tempoRimasto, int domandeRisposte, String domandaCorrente) {
        if (out != null && running && state == State.PLAYING) {
            // Sostituisce i ':' nel testo domanda per non rompere il protocollo
            String domSafe = domandaCorrente == null ? "" : domandaCorrente.replace(":", "=");
            out.println("SCORE:" + score + ":" + vite + ":" + tempoRimasto + ":" + domandeRisposte + ":" + domSafe);
        }
    }

    public void disconnect() {
        cancelled = true;
        running   = false;
        state     = State.DISCONNECTED;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public void markPlaying() { state = State.PLAYING; }

    private void listen() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"), 524288)) {
            String line;
            while (!cancelled && (line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
            if (!cancelled) {
                boolean wasPlaying = (state == State.PLAYING);
                state = State.DISCONNECTED;
                if (wasPlaying && onStop != null)
                    javax.swing.SwingUtilities.invokeLater(onStop);
                else if (onDisconnect != null)
                    javax.swing.SwingUtilities.invokeLater(onDisconnect);
            } else {
                state = State.DISCONNECTED;
            }
        }
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("START:")) {
            // Formato: START:OP:numOggetti:velocita:numDomande:tempoSecondi
            String[] parts = msg.split(":");
            if (parts.length < 2) return;

            String opName = parts[1];
            try {
                GameServer.Operazione op = GameServer.Operazione.valueOf(opName);

                // Applica i parametri del docente al GameConfig locale PRIMA di lanciare il gioco
                if (parts.length >= 6) {
                    int numOggetti  = Integer.parseInt(parts[2]);
                    int velocita    = Integer.parseInt(parts[3]);
                    int numDomande  = Integer.parseInt(parts[4]);
                    int tempo       = Integer.parseInt(parts[5]);

                    GameConfig cfg = GameConfig.get();
                    cfg.setNumOggetti(numOggetti);
                    cfg.setDifficulty(velocita);
                    cfg.setNumDomande(numDomande);
                    cfg.setSessionDuration(tempo);

                    System.out.println("[Client] Config applicata:"
                        + " oggetti=" + numOggetti
                        + " vel=" + velocita
                        + " domande=" + numDomande
                        + " tempo=" + tempo + "s");
                }

                // Avvia il gioco sull'EDT — il GameConfig è già aggiornato
                state = State.PLAYING;  // setta PLAYING subito, prima del lancio EDT
                javax.swing.SwingUtilities.invokeLater(() -> onGameStart.accept(op));

            } catch (Exception e) {
                System.err.println("[Client] Errore parsing START: " + msg + " -> " + e.getMessage());
            }

        } else if (msg.equals("STOP")) {
            System.out.println("[Client] Ricevuto STOP dal server.");
            cancelled = true;
            running   = false;
            state     = State.DISCONNECTED;
            if (onStop != null)
                javax.swing.SwingUtilities.invokeLater(onStop);
        }
    }

    private void notifyStatus(String m) {
        if (onStatus != null && !cancelled)
            javax.swing.SwingUtilities.invokeLater(() -> onStatus.accept(m));
    }
}
