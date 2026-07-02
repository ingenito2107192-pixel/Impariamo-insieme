import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Ball {

    public enum State { IDLE, EXPLODING, HIT_WRONG, DONE }

    public int   x, y;
    public int   value;
    public State state = State.IDLE;

    private final ObjectType                   legacyType;
    private final SpriteLoader.OperationSprite opSprite;

    private static final int DRAW_W     = 150;
    private static final int DRAW_H     = 132;
    private int hitRadius = 40; // sovrascitto da resolveVisualCenter()

    private float fx, fy, vx, vy;
    private int   animFrame, animTimer;
    private float wobble;

    // Centro visivo precalcolato per questo sprite
    private int visualCX, visualCY;

    private static final int[]   EXP_TICKS  = { 6, 6, 6, 8 };
    private static final int     HIT_FRAMES = 14;
    private static final Random  rand       = new Random();

    // Cache statica: idleKey -> [cx, cy]
    private static final Map<String,int[]> SPRITE_CENTERS = loadCenters();

    private static Map<String,int[]> loadCenters() {
        Map<String,int[]> map = new HashMap<>();
        try {
            File f = new File(ImageHelper.getProjectRoot(),
                "immagini/operazioni/sprite_centers.json");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Ogni riga ha forma:  "nome sprite": [cx, cy],
                if (!line.startsWith("\"")) continue;
                int q1 = line.indexOf('"');
                int q2 = line.indexOf('"', q1 + 1);
                if (q1 < 0 || q2 < 0) continue;
                String key = line.substring(q1 + 1, q2);
                int lb = line.indexOf('[');
                int rb = line.indexOf(']');
                if (lb < 0 || rb < 0) continue;
                String[] nums = line.substring(lb + 1, rb).split(",");
                if (nums.length < 2) continue;
                int cx = Integer.parseInt(nums[0].trim());
                int cy = Integer.parseInt(nums[1].trim());
                int hr = nums.length > 2 ? Integer.parseInt(nums[2].trim()) : 35;
                map.put(key, new int[]{ cx, cy, hr });
            }
            br.close();
            System.out.println("[Ball] Caricati " + map.size() + " centri sprite.");
        } catch (Exception e) {
            System.err.println("[Ball] sprite_centers.json non trovato: " + e.getMessage());
        }
        return map;
    }

    // ── Costruttori ───────────────────────────────────────────────

    public Ball(int screenW, int screenH, int value, float speed,
                SpriteLoader.OperationSprite opSprite) {
        this.value      = value;
        this.opSprite   = opSprite;
        this.legacyType = null;
        resolveVisualCenter();
        initPhysics(screenW, screenH, speed);
    }

    public Ball(int screenW, int screenH, int value, float speed, ObjectType type) {
        this.value      = value;
        this.legacyType = type;
        this.opSprite   = null;
        resolveVisualCenter();
        initPhysics(screenW, screenH, speed);
    }

    private void resolveVisualCenter() {
        String key = null;
        if (opSprite   != null) key = opSprite.idleKey;
        if (legacyType != null) key = legacyType.idleKey;
        if (key != null && SPRITE_CENTERS.containsKey(key)) {
            int[] c = SPRITE_CENTERS.get(key);
            visualCX  = c[0];
            visualCY  = c[1];
            hitRadius = c.length > 2 ? c[2] : 35;
        } else {
            visualCX  = DRAW_W / 2;
            visualCY  = DRAW_H / 3;
            hitRadius = 35;
        }
    }

    private void initPhysics(int screenW, int screenH, float speed) {
        this.fx = rand.nextInt(screenW - 200) + 100;
        this.fy = rand.nextInt(screenH - 300) + 110;
        this.x  = (int) fx;
        this.y  = (int) fy;
        double angle = rand.nextDouble() * 2 * Math.PI;
        this.vx = (float)(Math.cos(angle) * speed);
        this.vy = (float)(Math.sin(angle) * speed);
        this.wobble    = rand.nextFloat() * 360f;
        this.animFrame = 0;
        this.animTimer = EXP_TICKS[0];
    }

    // ── Update ────────────────────────────────────────────────────

    public void update(int screenW, int screenH) {
        switch (state) {
            case IDLE:
                fx += vx;
                fy += vy;
                int hw = DRAW_W / 2, hh = DRAW_H / 2;
                if (fx - hw < 0)            { fx = hw;               vx =  Math.abs(vx); }
                if (fx + hw > screenW)      { fx = screenW - hw;     vx = -Math.abs(vx); }
                if (fy - hh < 65)           { fy = 65 + hh;          vy =  Math.abs(vy); }
                if (fy + hh > screenH - 52) { fy = screenH - 52 - hh; vy = -Math.abs(vy); }
                x = Math.round(fx);
                y = Math.round(fy);
                wobble = (wobble + 1.2f) % 360f;
                break;
            case EXPLODING:
                animTimer--;
                if (animTimer <= 0) {
                    animFrame++;
                    if (animFrame >= EXP_TICKS.length) state = State.DONE;
                    else animTimer = EXP_TICKS[animFrame];
                }
                break;
            case HIT_WRONG:
                animTimer--;
                if (animTimer <= 0) state = State.DONE;
                break;
            case DONE:
                break;
        }
    }

    // ── Draw ──────────────────────────────────────────────────────

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        SpriteLoader sl = SpriteLoader.get();

        switch (state) {
            case IDLE: {
                int yOff  = (int)(Math.sin(Math.toRadians(wobble)) * 5);
                int drawX = x - DRAW_W / 2;
                int drawY = y - DRAW_H / 2 + yOff;
                drawSprite(g2, idleImage(sl), drawX, drawY, DRAW_W, DRAW_H, 1.0f);
                drawNumber(g2, drawX + visualCX, drawY + visualCY);
                break;
            }
            case EXPLODING: {
                BufferedImage img   = (animFrame < 3) ? idleImage(sl) : expImage(sl);
                float scale = (animFrame < 3) ? 1.0f + animFrame * 0.25f : 1.6f;
                float alpha = (animFrame < 3) ? 1.0f : Math.max(0f,
                    1.0f - (float)(EXP_TICKS[3] - animTimer) / EXP_TICKS[3]);
                int sw = (int)(DRAW_W * scale), sh = (int)(DRAW_H * scale);
                drawSprite(g2, img, x - sw / 2, y - sh / 2, sw, sh, alpha);
                break;
            }
            case HIT_WRONG: {
                int shake = (animTimer % 4 < 2) ? 5 : -5;
                int drawX = x - DRAW_W / 2 + shake;
                int drawY = y - DRAW_H / 2;
                drawSprite(g2, idleImage(sl), drawX, drawY, DRAW_W, DRAW_H, 1.0f);
                drawNumber(g2, drawX + visualCX, drawY + visualCY);
                break;
            }
            case DONE:
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private BufferedImage idleImage(SpriteLoader sl) {
        if (opSprite   != null) return sl.load(opSprite.idleKey);
        if (legacyType != null) return sl.load(legacyType.idleKey);
        return null;
    }

    private BufferedImage expImage(SpriteLoader sl) {
        if (opSprite   != null) return sl.load(opSprite.expKey);
        if (legacyType != null) return sl.load(legacyType.exp2Key);
        return null;
    }

    private void drawSprite(Graphics2D g2, BufferedImage img,
                            int x, int y, int w, int h, float alpha) {
        if (img == null) {
            g2.setColor(new Color(180, 200, 255, (int)(alpha * 200)));
            g2.fillOval(x, y, w, h);
            return;
        }
        if (alpha < 1.0f) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            g2.drawImage(img, x, y, w, h, null);
            g2.setComposite(old);
        } else {
            g2.drawImage(img, x, y, w, h, null);
        }
    }

    private void drawNumber(Graphics2D g2, int cx, int cy) {
        String txt  = String.valueOf(value);
        Font   font = new Font("Arial", Font.BOLD, 32);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(txt) / 2;
        int ty = cy + fm.getAscent() / 2 - 3;
        // Contorno nero spesso per effetto cicciotto
        g2.setColor(new Color(0, 0, 0, 200));
        for (int ox = -2; ox <= 2; ox++)
            for (int oy = -2; oy <= 2; oy++)
                if (ox != 0 || oy != 0)
                    g2.drawString(txt, tx + ox, ty + oy);
        g2.setColor(Color.WHITE);
        g2.drawString(txt, tx, ty);
    }

    public void startExplosion() {
        state = State.EXPLODING; animFrame = 0; animTimer = EXP_TICKS[0];
    }
    public void startHitWrong() {
        state = State.HIT_WRONG; animTimer = HIT_FRAMES;
    }
    public boolean isDone() { return state == State.DONE; }
    public boolean contains(int mx, int my) {
        int ddx = mx - x, ddy = my - y;
        return ddx * ddx + ddy * ddy <= hitRadius * hitRadius;
    }
    public ObjectType getType() { return legacyType; }
}
