import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ScoreStore - singleton thread-safe.
 * Salva e recupera la cronologia punteggi degli alunni.
 * Ogni record: nomeAlunno | operazione | punteggio | data
 */
public class ScoreStore {

    private static final String DATA_FILE;

    static {
        String sep  = System.getProperty("file.separator");
        String dir  = "Dati";
        new File(dir).mkdirs();
        DATA_FILE = dir + sep + "punteggi.dat";
    }

    public static class Record {
        public final String nomeAlunno;
        public final String operazione;
        public final int    punteggio;
        public final String data;

        public Record(String nomeAlunno, String operazione, int punteggio, String data) {
            this.nomeAlunno = nomeAlunno;
            this.operazione = operazione;
            this.punteggio  = punteggio;
            this.data       = data;
        }
    }

    private static volatile ScoreStore instance;
    public static ScoreStore get() {
        if (instance == null) {
            synchronized (ScoreStore.class) {
                if (instance == null) instance = new ScoreStore();
            }
        }
        return instance;
    }

    private final List<Record> records = new ArrayList<>();

    private ScoreStore() {
        caricaDaFile();
        // Se il file principale è vuoto, prova a caricare da punteggi.dat nella cartella corrente
        if (records.isEmpty()) {
            caricaDaFileFallback("punteggi.dat");
        }
    }

    /** Aggiunge un nuovo record di punteggio e salva su disco. */
    public synchronized void aggiungiPunteggio(String nomeAlunno, int operazione, int punteggio) {
        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String nomeOp = (operazione >= 0 && operazione < GameConfig.OP_NOMI.length)
            ? GameConfig.OP_NOMI[operazione] : "?";
        records.add(new Record(nomeAlunno, nomeOp, punteggio, data));
        salvaSuFile();
    }

    /** Restituisce tutti i record di un determinato alunno (dal più recente). */
    public synchronized List<Record> getPunteggiAlunno(String nomeAlunno) {
        List<Record> result = new ArrayList<>();
        for (Record r : records)
            if (r.nomeAlunno.equalsIgnoreCase(nomeAlunno))
                result.add(r);
        Collections.reverse(result);
        return result;
    }

    private synchronized void salvaSuFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (Record r : records)
                bw.write(r.nomeAlunno + "|" + r.operazione + "|" + r.punteggio + "|" + r.data + "\n");
        } catch (IOException e) {
            System.err.println("[ScoreStore] impossibile salvare: " + e.getMessage());
        }
    }

    private void caricaDaFile() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        leggiFile(f);
    }

    private void caricaDaFileFallback(String nomeFile) {
        File f = new File(nomeFile);
        if (!f.exists()) return;
        leggiFile(f);
        // Copia i dati nel file principale così da ora in poi vengono usati quelli
        salvaSuFile();
    }

    private void leggiFile(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String riga;
            while ((riga = br.readLine()) != null) {
                riga = riga.trim();
                if (riga.isEmpty()) continue;
                String[] p = riga.split("\\|", 4);
                if (p.length < 4) continue;
                try {
                    records.add(new Record(p[0], p[1], Integer.parseInt(p[2]), p[3]));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[ScoreStore] impossibile leggere: " + e.getMessage());
        }
    }
}
