import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpriteLoader
 * ------------
 * Singleton con cache e supporto per sprite suddivisi per operazione.
 *
 * Struttura cartelle attesa (relativa a projectRoot/immagini/):
 *   operazioni/Sprite tagliato/addizione/       cuori
 *   operazioni/Sprite tagliato/sottrazione/     bolle/palline
 *   operazioni/Sprite tagliato/moltiplicazione/ stelle
 *   operazioni/Sprite tagliato/divisione/       palloncini
 *
 * File "nome.png"   => sprite idle
 * File "nome 2.png" => sprite esplosione (click corretto)
 *
 * Metodo principale per il gioco:
 *   getSpritesForOperation(int op) -> lista casuale di OperationSprite
 *   getBackground(int op)          -> BufferedImage sfondo
 */
public class SpriteLoader {

    private static final BufferedImage NOT_FOUND =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private static volatile SpriteLoader instance;

    private final ConcurrentHashMap<String, BufferedImage> cache = new ConcurrentHashMap<>();

    private final String legacySpritesDir;
    private final File   opSpriteRoot;

    private static final String[] OP_DIRS = {
        "addizione",
        "sottrazione",
        "moltiplicazione",
        "divisione"
    };

    private static final String[] BG_PATHS = {
        "operazioni/cuoricini-addizione.png",
        "operazioni/bolle-sottrazione.png",
        "operazioni/stelle-moltiplicazione.png",
        "operazioni/palloncini-divisione.png"
    };

    // ── Singleton ────────────────────────────────────────────────

    private SpriteLoader() {
        File root = ImageHelper.getProjectRoot();
        File legacyDir = new File(root, "immagini/Sprite tagliato");
        legacySpritesDir = legacyDir.exists()
            ? legacyDir.getAbsolutePath() + "/"
            : "sprites/";
        opSpriteRoot = new File(root, "immagini/operazioni/Sprite tagliato");
    }

    public static SpriteLoader get() {
        if (instance == null) {
            synchronized (SpriteLoader.class) {
                if (instance == null) instance = new SpriteLoader();
            }
        }
        return instance;
    }

    // ── Sprite per operazione ────────────────────────────────────

    /** Coppia idle + esplosione di uno sprite operazione. */
    public static class OperationSprite {
        public final String idleKey;
        public final String expKey;
        public final int    radius;

        public OperationSprite(String idleKey, String expKey, int radius) {
            this.idleKey = idleKey;
            this.expKey  = expKey;
            this.radius  = radius;
        }
    }

    /**
     * Restituisce la lista degli sprite disponibili per l'operazione indicata,
     * in ordine casuale. Ogni chiamata rimescola la lista.
     * @param operation 0=addizione 1=sottrazione 2=moltiplicazione 3=divisione
     */
    public List<OperationSprite> getSpritesForOperation(int operation) {
        if (operation < 0 || operation >= OP_DIRS.length) return new ArrayList<>();

        File dir = new File(opSpriteRoot, OP_DIRS[operation]);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("[SpriteLoader] Cartella non trovata: " + dir.getAbsolutePath());
            return new ArrayList<>();
        }

        File[] idleFiles = dir.listFiles(
            (d, n) -> n.endsWith(".png") && !n.endsWith(" 2.png")
        );
        if (idleFiles == null || idleFiles.length == 0) return new ArrayList<>();

        List<OperationSprite> list = new ArrayList<>();
        for (File idle : idleFiles) {
            String idleKey = idle.getName().replace(".png", "");
            String expKey  = idleKey + " 2";
            File   expFile = new File(dir, expKey + ".png");

            loadFromFile(idleKey, idle);
            if (expFile.exists()) loadFromFile(expKey, expFile);

            String actualExpKey = expFile.exists() ? expKey : idleKey;
            int    radius       = estimateRadius(idle);

            list.add(new OperationSprite(idleKey, actualExpKey, radius));
        }
        Collections.shuffle(list);
        return list;
    }

    private int estimateRadius(File f) {
        BufferedImage img = loadFromFile(f.getName().replace(".png", ""), f);
        if (img == null) return 30;
        int halfMin = Math.min(img.getWidth(), img.getHeight()) / 2;
        return Math.max(24, Math.min(44, halfMin));
    }

    // ── Sfondo ───────────────────────────────────────────────────

    /** Restituisce l'immagine di sfondo per l'operazione, o null se non trovata. */
    public BufferedImage getBackground(int operation) {
        if (operation < 0 || operation >= BG_PATHS.length) return null;
        return ImageHelper.loadImage(BG_PATHS[operation]);
    }

    // ── API legacy ────────────────────────────────────────────────

    public BufferedImage load(String key) {
        BufferedImage cached = cache.get(key);
        if (cached == NOT_FOUND) return null;
        if (cached != null)      return cached;

        File f = new File(legacySpritesDir + key + ".png");
        if (!f.exists()) f = findInSubdirs(new File(legacySpritesDir), key + ".png");
        if (f == null || !f.exists()) { cache.put(key, NOT_FOUND); return null; }
        return loadFromFile(key, f);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private BufferedImage loadFromFile(String key, File f) {
        BufferedImage cached = cache.get(key);
        if (cached == NOT_FOUND) return null;
        if (cached != null)      return cached;
        try {
            BufferedImage img = ImageIO.read(f);
            cache.put(key, img != null ? img : NOT_FOUND);
            return img;
        } catch (Exception e) {
            cache.put(key, NOT_FOUND);
            return null;
        }
    }

    private File findInSubdirs(File dir, String filename) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        File direct = new File(dir, filename);
        if (direct.exists()) return direct;
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) return null;
        for (File sub : subdirs) {
            File found = findInSubdirs(sub, filename);
            if (found != null) return found;
        }
        return null;
    }

    public void preloadAll() {
        preloadDir(new File(legacySpritesDir));
        for (int op = 0; op < OP_DIRS.length; op++) getSpritesForOperation(op);
        System.out.println("[SpriteLoader] preloaded " + cache.size() + " sprites.");
    }

    private void preloadDir(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
        if (files != null) for (File f : files) load(f.getName().replace(".png", ""));
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) for (File sub : subdirs) preloadDir(sub);
    }
}
