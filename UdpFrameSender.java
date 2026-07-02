import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invia frame JPEG frammentati via UDP al server.
 * Ogni chunk: frameId(4) + chunkIdx(2) + totalChunks(2) + nomeLen(1) + nome(n) + dati
 */
public class UdpFrameSender {

    private static final int CHUNK_SIZE = 8192;
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private final int serverPort = UdpFrameReceiver.UDP_PORT;
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private final byte[] nomeBytes;

    public UdpFrameSender(String serverHost, String nomeAlunno) {
        this.nomeBytes = nomeAlunno.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            socket = new DatagramSocket();
            socket.setSendBufferSize(256 * 1024);
            serverAddr = InetAddress.getByName(serverHost);
            System.out.println("[UDP] Sender pronto verso " + serverHost + ":" + serverPort);
        } catch (Exception e) {
            System.err.println("[UDP] Impossibile creare sender: " + e.getMessage());
        }
    }

    public void sendFrame(byte[] jpegBytes) {
        if (socket == null || serverAddr == null) return;
        int frameId = frameCounter.incrementAndGet();
        int totalChunks = (jpegBytes.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        if (totalChunks == 0) totalChunks = 1;

        int headerSize = 9 + nomeBytes.length;
        byte[] pkt = new byte[headerSize + CHUNK_SIZE];

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int chunkLen = Math.min(CHUNK_SIZE, jpegBytes.length - offset);

            // Header
            pkt[0] = (byte)(frameId >> 24); pkt[1] = (byte)(frameId >> 16);
            pkt[2] = (byte)(frameId >> 8);  pkt[3] = (byte)(frameId);
            pkt[4] = (byte)(i >> 8);        pkt[5] = (byte)(i);
            pkt[6] = (byte)(totalChunks >> 8); pkt[7] = (byte)(totalChunks);
            pkt[8] = (byte)(nomeBytes.length);
            System.arraycopy(nomeBytes, 0, pkt, 9, nomeBytes.length);
            System.arraycopy(jpegBytes, offset, pkt, headerSize, chunkLen);

            try {
                socket.send(new DatagramPacket(pkt, headerSize + chunkLen, serverAddr, serverPort));
            } catch (Exception e) {
                System.err.println("[UDP] Errore invio chunk: " + e.getMessage());
            }
        }
    }

    public void close() {
        if (socket != null) socket.close();
    }
}
