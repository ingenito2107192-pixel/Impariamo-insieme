import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URI;
import java.util.HashMap;

public class ImageHelper {

    private static final HashMap<String, BufferedImage> cache = new HashMap<>();
    private static File projectRoot = null;

    public static synchronized File getProjectRoot() {
        if (projectRoot != null) return projectRoot;

        // Strategia 1: da posizione del .class / .jar
        try {
            URI uri = ImageHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File dir = new File(uri);
            if (dir.isFile()) dir = dir.getParentFile();
            for (File cur = dir; cur != null; cur = cur.getParentFile()) {
                if (new File(cur, "immagini").isDirectory()) {
                    projectRoot = cur;
                    return projectRoot;
                }
            }
        } catch (Exception ignored) {}

        // Strategia 2: da working directory
        for (File cur = new File(System.getProperty("user.dir")); cur != null; cur = cur.getParentFile()) {
            if (new File(cur, "immagini").isDirectory()) {
                projectRoot = cur;
                return projectRoot;
            }
        }

        // Strategia 3: sottocartelle dirette della working dir
        File workDir = new File(System.getProperty("user.dir"));
        File[] subDirs = workDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File sub : subDirs) {
                if (new File(sub, "immagini").isDirectory()) {
                    projectRoot = sub;
                    return projectRoot;
                }
            }
        }

        projectRoot = workDir;
        System.err.println("[ImageHelper] ATTENZIONE: cartella immagini non trovata. Usando: " + projectRoot.getAbsolutePath());
        return projectRoot;
    }

    public static synchronized BufferedImage loadImage(String relativePath) {
        if (cache.containsKey(relativePath)) return cache.get(relativePath);

        File f = new File(getProjectRoot(), "immagini/" + relativePath);
        BufferedImage img = null;
        try {
            if (f.exists()) {
                img = ImageIO.read(f);
            } else {
                System.err.println("[ImageHelper] MANCANTE: " + f.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[ImageHelper] ERRORE lettura " + relativePath + ": " + e.getMessage());
        }
        cache.put(relativePath, img); // HashMap accetta null, ConcurrentHashMap no
        return img;
    }
}
