/**
 * ObjectType
 * ----------
 * Catalogo di tutti i tipi di oggetto disponibili nel gioco.
 *
 * Ogni tipo conosce:
 *   - idleKey   : chiave della sprite idle (frame statico / loop)
 *   - exp0Key   : frame 0 dell'animazione di esplosione (corretta)
 *   - exp1Key   : frame 1 dell'animazione di esplosione
 *   - exp2Key   : frame 2 dell'animazione di esplosione (finale)
 *   - burstKey  : effetto burst sovrapposto al centro
 *   - hitKey    : frame tremolio risposta errata (riusa exp0 con tint rosso)
 *   - radius    : raggio di collisione in pixel
 *
 * I tipi sono raggruppati in categorie. I livelli di difficolta'
 * determinano quali categorie appaiono (vedi GameConfig).
 *
 * Per aggiungere un nuovo tipo:
 *   1. Aggiungi un valore qui con le chiavi corrette
 *   2. Assicurati che i file .png esistano in sprites/
 */
public enum ObjectType {

    // ─── SFERE COLORATE ─────────────────────────────────────────
    SPHERE_RED     ("sphere_red_idle",     "sphere_red_idle",      "sphere_red_idle",      "burst_red",    "burst_red",    32),
    SPHERE_ORANGE  ("sphere_orange_idle",  "sphere_orange_idle",   "sphere_orange_idle",   "burst_yellow", "burst_red",    32),
    SPHERE_YELLOW  ("sphere_yellow_idle",  "sphere_yellow_idle",   "sphere_yellow_idle",   "burst_yellow", "burst_red",    32),
    SPHERE_GREEN   ("sphere_green_idle",   "sphere_green_idle",    "sphere_green_idle",    "burst_green",  "burst_red",    32),
    SPHERE_BLUE    ("sphere_blue_idle",    "sphere_blue_idle",     "sphere_blue_idle",     "burst_blue",   "burst_red",    32),
    SPHERE_PURPLE  ("sphere_purple_idle",  "sphere_purple_idle",   "sphere_purple_idle",   "burst_purple", "burst_red",    32),
    SPHERE_PINK    ("sphere_pink_idle",    "sphere_pink_idle",     "sphere_pink_idle",     "burst_red",    "burst_red",    32),
    SPHERE_CYAN    ("sphere_cyan_idle",    "sphere_cyan_idle",     "sphere_cyan_idle",     "burst_blue",   "burst_red",    32),

    // ─── SFERE SPECIALI ──────────────────────────────────────────
    SPHERE_GOLD    ("sphere_gold_idle",    "sphere_gold_exp1",     "burst_yellow",         "burst_yellow", "burst_red",    34),
    SPHERE_SILVER  ("sphere_silver_idle",  "sphere_silver_exp1",   "burst_blue",           "burst_blue",   "burst_red",    34),
    SPHERE_BUBBLE  ("sphere_bubble_idle",  "sphere_bubble_exp1",   "burst_blue",           "burst_blue",   "burst_red",    34),
    SPHERE_GEM     ("sphere_gem_idle",     "sphere_gem_exp1",      "burst_green",          "burst_green",  "burst_red",    34),

    // ─── PALLONCINI TONDI ────────────────────────────────────────
    BALLOON_RED    ("balloon_red_idle",    "balloon_red_exp0",     "balloon_red_exp1",     "balloon_red_exp2",    "burst_red",    28),
    BALLOON_ORANGE ("balloon_orange_idle", "balloon_orange_exp0",  "balloon_orange_exp1",  "balloon_orange_exp2", "burst_red",    28),
    BALLOON_YELLOW ("balloon_yellow_idle", "balloon_yellow_exp0",  "balloon_yellow_exp1",  "balloon_yellow_exp2", "burst_yellow", 28),
    BALLOON_GREEN  ("balloon_green_idle",  "balloon_green_exp0",   "balloon_green_exp1",   "balloon_green_exp2",  "burst_green",  28),
    BALLOON_BLUE   ("balloon_blue_idle",   "balloon_blue_exp0",    "balloon_blue_exp1",    "balloon_blue_exp2",   "burst_blue",   28),
    BALLOON_PURPLE ("balloon_purple_idle", "balloon_purple_exp0",  "balloon_purple_exp1",  "balloon_purple_exp2", "burst_purple", 28),
    BALLOON_PINK   ("balloon_pink_idle",   "balloon_pink_exp0",    "balloon_pink_exp1",    "balloon_pink_exp2",   "burst_red",    28),
    BALLOON_CYAN   ("balloon_cyan_idle",   "balloon_cyan_exp0",    "balloon_cyan_exp1",    "balloon_cyan_exp2",   "burst_blue",   28),

    // ─── PALLONCINI CUORE ────────────────────────────────────────
    HEART_RED      ("heart_red_idle",      "heart_red_exp1",       "fx_hearts",            "burst_red",    "burst_red",    30),
    HEART_ORANGE   ("heart_orange_idle",   "heart_orange_idle",    "fx_hearts",            "burst_yellow", "burst_red",    30),
    HEART_YELLOW   ("heart_yellow_idle",   "heart_yellow_idle",    "fx_hearts",            "burst_yellow", "burst_red",    30),
    HEART_GREEN    ("heart_green_idle",    "heart_green_idle",     "fx_hearts",            "burst_green",  "burst_red",    30),
    HEART_BLUE     ("heart_blue_idle",     "heart_blue_idle",      "fx_hearts",            "burst_blue",   "burst_red",    30),
    HEART_PURPLE   ("heart_purple_idle",   "heart_purple_exp1",    "fx_hearts",            "burst_purple", "burst_red",    30),
    HEART_PINK     ("heart_pink_idle",     "heart_pink_idle",      "fx_hearts",            "burst_red",    "burst_red",    30),

    // ─── STELLE ─────────────────────────────────────────────────
    STAR_RED       ("star_red_idle",       "star_red_idle",        "fx_stars",             "burst_red",    "burst_red",    30),
    STAR_ORANGE    ("star_orange_idle",    "star_orange_idle",     "fx_stars",             "burst_yellow", "burst_red",    30),
    STAR_YELLOW    ("star_yellow_idle",    "star_yellow_idle",     "fx2_stars_0",          "burst_yellow", "burst_red",    30),
    STAR_GREEN     ("star_green_idle",     "star_green_idle",      "fx2_stars_1",          "burst_green",  "burst_red",    30),
    STAR_BLUE      ("star_blue_idle",      "star_blue_idle",       "fx2_stars_2",          "burst_blue",   "burst_red",    30),
    STAR_GOLD      ("star_gold_idle",      "fx_star_yellow",       "fx2_stars_4",          "burst_yellow", "burst_red",    32),

    // ─── GOCCE ──────────────────────────────────────────────────
    DROP_RED       ("drop_red_idle",       "drop_red_idle",        "fx_splash",            "burst_red",    "burst_red",    26),
    DROP_BLUE      ("drop_blue_idle",      "drop_blue_idle",       "fx_splash",            "burst_blue",   "burst_red",    26),
    DROP_GREEN     ("drop_green_idle",     "drop_green_idle",      "fx_splash",            "burst_green",  "burst_red",    26),
    DROP_YELLOW    ("drop_yellow_idle",    "drop_yellow_idle",     "fx_splash",            "burst_yellow", "burst_red",    26),
    DROP_PURPLE    ("drop_purple_idle",    "drop_purple_idle",     "fx_splash",            "burst_purple", "burst_red",    26),

    // ─── BOLLE/ANELLI ────────────────────────────────────────────
    RING_RED       ("ring_red_idle",       "ring_red_idle",        "fx_burst_blue",        "burst_red",    "burst_red",    30),
    RING_BLUE      ("ring_blue_idle",      "ring_blue_idle",       "fx_burst_blue",        "burst_blue",   "burst_red",    30),
    RING_GREEN     ("ring_green_idle",     "ring_green_idle",      "fx_spark_green",       "burst_green",  "burst_red",    30),
    RING_YELLOW    ("ring_yellow_idle",    "ring_yellow_idle",     "fx_star_yellow",       "burst_yellow", "burst_red",    30),
    RING_PURPLE    ("ring_purple_idle",    "ring_purple_idle",     "fx_burst_purple",      "burst_purple", "burst_red",    30),

    // ─── PALLINE SPORTIVE ────────────────────────────────────────
    BALL_SOCCER    ("ball_soccer_idle",    "ball_soccer_exp0",     "ball_soccer_exp1",     "burst_red",    "burst_red",    32),
    BALL_BASKET    ("ball_basketball_idle","ball_basketball_exp0", "ball_basketball_exp1", "burst_yellow", "burst_red",    32),
    BALL_BASEBALL  ("ball_baseball_idle",  "ball_baseball_exp0",   "ball_baseball_exp1",   "burst_blue",   "burst_red",    32),
    BALL_TENNIS    ("ball_tennis_idle",    "ball_tennis_exp0",     "ball_tennis_exp1",     "burst_green",  "burst_red",    32),
    BALL_BEACH     ("ball_beach_idle",     "ball_beach_idle",      "burst_red",            "burst_yellow", "burst_red",    32);

    // ──────────────────────────────────────────────────────────────
    public final String idleKey;   // sprite idle
    public final String exp0Key;   // frame 0 esplosione
    public final String exp1Key;   // frame 1 esplosione
    public final String exp2Key;   // frame 2 esplosione (finale)
    public final String burstKey;  // effetto burst sovrapposto
    public final int    radius;    // raggio collisione

    ObjectType(String idle, String e0, String e1, String e2, String burst, int r) {
        this.idleKey  = idle;
        this.exp0Key  = e0;
        this.exp1Key  = e1;
        this.exp2Key  = e2;
        this.burstKey = burst;
        this.radius   = r;
    }

    // ─── Gruppi per difficolta' ───────────────────────────────────

    /** Oggetti usati al livello FACILE */
    public static final ObjectType[] EASY = {
        BALLOON_RED, BALLOON_YELLOW, BALLOON_GREEN, BALLOON_BLUE,
        SPHERE_RED, SPHERE_GREEN, SPHERE_BLUE,
        STAR_YELLOW, STAR_GREEN, STAR_BLUE,
    };

    /** Oggetti aggiunti al livello MEDIO */
    public static final ObjectType[] MEDIUM = {
        BALLOON_ORANGE, BALLOON_PURPLE, BALLOON_PINK, BALLOON_CYAN,
        SPHERE_ORANGE, SPHERE_PURPLE, SPHERE_PINK, SPHERE_CYAN,
        HEART_RED, HEART_BLUE, HEART_GREEN,
        DROP_BLUE, DROP_GREEN,
        RING_BLUE, RING_GREEN,
    };

    /** Oggetti aggiunti al livello DIFFICILE */
    public static final ObjectType[] HARD = {
        HEART_YELLOW, HEART_ORANGE, HEART_PURPLE, HEART_PINK,
        SPHERE_GOLD, SPHERE_SILVER, SPHERE_BUBBLE, SPHERE_GEM,
        STAR_RED, STAR_ORANGE, STAR_GOLD,
        DROP_RED, DROP_YELLOW, DROP_PURPLE,
        RING_RED, RING_YELLOW, RING_PURPLE,
        BALL_SOCCER, BALL_BASKET, BALL_BASEBALL, BALL_TENNIS, BALL_BEACH,
    };
}
