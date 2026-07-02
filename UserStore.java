import java.io.*;
import java.util.*;

/**
 * UserStore - singleton thread-safe.
 * Gestisce docenti, alunni e l'assegnazione alunno→docente.
 * Formato file: RUOLO|nome|password|docenteAssegnato
 */
public class UserStore {

    private static final String PRESIDE_NOME     = "P";
    private static final String PRESIDE_PASSWORD = "p";
    private static final String DATA_FILE;

    static {
        // Salva sempre nella home dell'utente, indipendentemente dalla
        // cartella da cui si lancia l'applicazione
        String sep  = System.getProperty("file.separator");
        String dir  = "Dati";
        new java.io.File(dir).mkdirs();          // crea la cartella se non esiste
        DATA_FILE = dir + sep + "utenti.dat";
    }

    private static volatile UserStore instance;
    public static UserStore get() {
        if (instance == null) {
            synchronized (UserStore.class) {
                if (instance == null) instance = new UserStore();
            }
        }
        return instance;
    }

    private final Map<String, User> byName  = new HashMap<>();
    private final List<User> docenti = new ArrayList<>();
    private final List<User> alunni  = new ArrayList<>();

    private UserStore() {
        User preside = new User(PRESIDE_NOME, PRESIDE_PASSWORD, User.Ruolo.PRESIDE);
        byName.put(PRESIDE_NOME.toLowerCase(), preside);
        caricaDaFile();
        // Fallback: se non ci sono utenti nel file principale, prova dalla cartella del progetto
        if (docenti.isEmpty() && alunni.isEmpty()) {
            caricaDaFileFallback("utenti.dat");
        }
    }

    // ── Autenticazione ─────────────────────────────────────────────
    public synchronized User autentica(String nome, String password) {
        User u = byName.get(nome == null ? "" : nome.trim().toLowerCase());
        return (u != null && u.matchesPassword(password)) ? u : null;
    }

    // ── Gestione utenti ────────────────────────────────────────────
    public synchronized boolean esisteNome(String nome) {
        return byName.containsKey(nome == null ? "" : nome.trim().toLowerCase());
    }

    public synchronized boolean aggiungiDocente(String nome, String password) {
        if (nome == null || nome.trim().isEmpty() || password == null || password.isEmpty()) return false;
        if (esisteNome(nome)) return false;
        User u = new User(nome, password, User.Ruolo.DOCENTE);
        byName.put(nome.trim().toLowerCase(), u);
        docenti.add(u);
        salvasuFile();
        return true;
    }

    public synchronized boolean aggiungiAlunno(String nome, String password) {
        if (nome == null || nome.trim().isEmpty() || password == null || password.isEmpty()) return false;
        if (esisteNome(nome)) return false;
        User u = new User(nome, password, User.Ruolo.ALUNNO);
        byName.put(nome.trim().toLowerCase(), u);
        alunni.add(u);
        salvasuFile();
        return true;
    }

    public synchronized boolean eliminaUtente(String nome) {
        if (nome == null || nome.trim().isEmpty()) return false;
        User u = byName.get(nome.trim().toLowerCase());
        if (u == null || u.getRuolo() == User.Ruolo.PRESIDE) return false;
        byName.remove(nome.trim().toLowerCase());
        if (u.getRuolo() == User.Ruolo.DOCENTE) {
            docenti.remove(u);
            // rimuovi l'assegnazione dagli alunni di questo docente
            for (User a : alunni)
                if (u.getNome().equals(a.getDocenteAssegnato()))
                    a.setDocenteAssegnato(null);
        } else {
            alunni.remove(u);
        }
        salvasuFile();
        return true;
    }

    /**
     * Assegna un alunno a un docente (o rimuove l'assegnazione se docenteNome è null/"").
     * Restituisce false se alunno o docente non esistono.
     */
    public synchronized boolean assegnaAlunnoADocente(String nomeAlunno, String nomeDocente) {
        User alunno = byName.get(nomeAlunno == null ? "" : nomeAlunno.trim().toLowerCase());
        if (alunno == null || alunno.getRuolo() != User.Ruolo.ALUNNO) return false;
        if (nomeDocente == null || nomeDocente.trim().isEmpty()) {
            alunno.setDocenteAssegnato(null);
        } else {
            User docente = byName.get(nomeDocente.trim().toLowerCase());
            if (docente == null || docente.getRuolo() != User.Ruolo.DOCENTE) return false;
            alunno.setDocenteAssegnato(docente.getNome());
        }
        salvasuFile();
        return true;
    }

    /** Restituisce tutti gli alunni assegnati a un docente specifico. */
    public synchronized List<User> getAlunniDiDocente(String nomeDocente) {
        List<User> result = new ArrayList<>();
        for (User a : alunni)
            if (nomeDocente.equals(a.getDocenteAssegnato()))
                result.add(a);
        return result;
    }

    /** Restituisce tutti gli alunni non ancora assegnati a nessun docente. */
    public synchronized List<User> getAlunniSenzaDocente() {
        List<User> result = new ArrayList<>();
        for (User a : alunni)
            if (!a.hasDocente()) result.add(a);
        return result;
    }

    /** Cambia nome e/o password del preside. Restituisce false se il nuovo nome è già in uso. */
    public synchronized boolean cambiaCredenzialiPreside(String nuovoNome, String nuovaPassword) {
        if (nuovoNome == null || nuovoNome.trim().isEmpty()) return false;
        if (nuovaPassword == null || nuovaPassword.isEmpty()) return false;
        // Rimuovi vecchio preside dalla mappa
        User vecchio = null;
        for (Map.Entry<String, User> e : byName.entrySet()) {
            if (e.getValue().getRuolo() == User.Ruolo.PRESIDE) { vecchio = e.getValue(); byName.remove(e.getKey()); break; }
        }
        // Controlla conflitti di nome (con altri utenti)
        if (byName.containsKey(nuovoNome.trim().toLowerCase())) {
            // Reinserisci vecchio
            if (vecchio != null) byName.put(vecchio.getNome().toLowerCase(), vecchio);
            return false;
        }
        User nuovoPreside = new User(nuovoNome.trim(), nuovaPassword, User.Ruolo.PRESIDE);
        byName.put(nuovoNome.trim().toLowerCase(), nuovoPreside);
        return true;
    }

    /** Restituisce nome utente del preside corrente. */
    public synchronized String getNomePreside() {
        for (User u : byName.values()) if (u.getRuolo() == User.Ruolo.PRESIDE) return u.getNome();
        return "P";
    }
    public List<User> getDocenti() { return Collections.unmodifiableList(docenti); }
    public List<User> getAlunni()  { return Collections.unmodifiableList(alunni);  }

    // ── Persistenza ────────────────────────────────────────────────
    // Formato: RUOLO|nome|password|docenteAssegnato
    private synchronized void salvasuFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (User u : docenti)
                bw.write(u.getRuolo().name() + "|" + u.getNome() + "|" + u.getPassword() + "|\n");
            for (User u : alunni) {
                String d = u.getDocenteAssegnato() != null ? u.getDocenteAssegnato() : "";
                bw.write(u.getRuolo().name() + "|" + u.getNome() + "|" + u.getPassword() + "|" + d + "\n");
            }
        } catch (IOException e) {
            System.err.println("[UserStore] impossibile salvare: " + e.getMessage());
        }
    }

    private synchronized void caricaDaFile() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        leggiFile(f);
    }

    private synchronized void caricaDaFileFallback(String nomeFile) {
        File f = new File(nomeFile);
        if (!f.exists()) return;
        leggiFile(f);
        salvasuFile(); // copia nel file principale
    }

    private void leggiFile(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String riga;
            while ((riga = br.readLine()) != null) {
                riga = riga.trim();
                if (riga.isEmpty()) continue;
                String[] p = riga.split("\\|", 4);
                if (p.length < 3) continue;
                try {
                    User.Ruolo ruolo = User.Ruolo.valueOf(p[0]);
                    if (ruolo == User.Ruolo.PRESIDE) continue;
                    String nome = p[1], pass = p[2];
                    if (esisteNome(nome)) continue;
                    User u = new User(nome, pass, ruolo);
                    if (p.length == 4 && !p[3].isEmpty()) u.setDocenteAssegnato(p[3]);
                    byName.put(nome.trim().toLowerCase(), u);
                    if (ruolo == User.Ruolo.DOCENTE) docenti.add(u);
                    else alunni.add(u);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[UserStore] impossibile leggere: " + e.getMessage());
        }
    }
}
