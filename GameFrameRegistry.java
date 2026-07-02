import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro statico condiviso in memoria.
 * Ogni GamePanel deposita qui il suo ultimo frame renderizzato.
 * MonitorPanel lo legge per mostrare lo schermo live dell'alunno.
 */
public class GameFrameRegistry {
    private static final ConcurrentHashMap<String, BufferedImage> frames = new ConcurrentHashMap<>();

    public static void putFrame(String nomeAlunno, BufferedImage img) {
        if (nomeAlunno != null && img != null) frames.put(nomeAlunno, img);
    }

    public static BufferedImage getFrame(String nomeAlunno) {
        return nomeAlunno == null ? null : frames.get(nomeAlunno);
    }

    public static void removeFrame(String nomeAlunno) {
        if (nomeAlunno != null) frames.remove(nomeAlunno);
    }
}
