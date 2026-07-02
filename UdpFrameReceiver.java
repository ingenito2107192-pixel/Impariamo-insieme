import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Riceve frame JPEG frammentati via UDP sulla porta 12346.
 * Ogni pacchetto ha header: frameId(4) + chunkIdx(2) + totalChunks(2) + nomeLen(1) + nome(n) + dati
 * Quando tutti i chunk di un frame arrivano, riassembla e notifica il listener.
 */
public class UdpFrameReceiver {

    public static final int UDP_PORT = 12346;
    private static final int CHUNK_SIZE = 8192;
    private static final int MAX_PENDING = 8; // frame in attesa di completamento per alunno

    public interface FrameListener {
        void onFrame(String nomeAlunno, byte[] jpegBytes);
    }

    private DatagramSocket udpSocket;
    private Thread receiveThread;
    private volatile boolean running = false;
    private FrameListener listener;

    // nomeAlunno -> (frameId -> chunks)
    private final ConcurrentHashMap<String, LinkedHashMap<Integer, byte[][]>> pending = new ConcurrentHashMap<>();

    public UdpFrameReceiver(FrameListener listener) {
        this.listener = listener;
    }

    public void start() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setReceiveBufferSize(256 * 1024);
            running = true;
            receiveThread = new Thread(this::receiveLoop, "udp-frame-receiver");
            receiveThread.setDaemon(true);
            receiveThread.start();
            System.out.println("[UDP] Receiver avviato sulla porta " + UDP_PORT);
        } catch (Exception e) {
            System.err.println("[UDP] Impossibile avviare receiver: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (udpSocket != null) udpSocket.close();
    }

    private void receiveLoop() {
        byte[] buf = new byte[CHUNK_SIZE + 64];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        while (running) {
            try {
                udpSocket.receive(pkt);
                processPacket(pkt.getData(), pkt.getLength());
            } catch (Exception e) {
                if (running) System.err.println("[UDP] Errore ricezione: " + e.getMessage());
            }
        }
    }

    private void processPacket(byte[] data, int len) {
        if (len < 9) return;
        int frameId     = ((data[0]&0xFF)<<24)|((data[1]&0xFF)<<16)|((data[2]&0xFF)<<8)|(data[3]&0xFF);
        int chunkIdx    = ((data[4]&0xFF)<<8)|(data[5]&0xFF);
        int totalChunks = ((data[6]&0xFF)<<8)|(data[7]&0xFF);
        int nomeLen     = data[8]&0xFF;
        if (len < 9 + nomeLen) return;
        String nome = new String(data, 9, nomeLen, java.nio.charset.StandardCharsets.UTF_8);
        int dataOffset = 9 + nomeLen;
        int dataLen = len - dataOffset;
        if (dataLen < 0) return;

        byte[] chunk = Arrays.copyOfRange(data, dataOffset, dataOffset + dataLen);

        pending.computeIfAbsent(nome, k -> new LinkedHashMap<Integer,byte[][]>() {
            protected boolean removeEldestEntry(Map.Entry<Integer,byte[][]> e) { return size() > MAX_PENDING; }
        });

        LinkedHashMap<Integer,byte[][]> frames = pending.get(nome);
        synchronized (frames) {
            frames.computeIfAbsent(frameId, k -> new byte[totalChunks][]);
            byte[][] chunks = frames.get(frameId);
            if (chunkIdx < chunks.length) chunks[chunkIdx] = chunk;

            // Controlla se tutti i chunk sono arrivati
            boolean complete = true;
            int total = 0;
            for (byte[] c : chunks) { if (c == null) { complete = false; break; } total += c.length; }
            if (complete) {
                frames.remove(frameId);
                byte[] jpeg = new byte[total];
                int pos = 0;
                for (byte[] c : chunks) { System.arraycopy(c, 0, jpeg, pos, c.length); pos += c.length; }
                listener.onFrame(nome, jpeg);
            }
        }
    }
}
