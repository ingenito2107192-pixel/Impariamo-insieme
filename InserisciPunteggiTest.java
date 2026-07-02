import java.io.*;
import java.nio.file.*;

/**
 * InserisciPunteggiTest
 * ----------------------
 * Utility di test: inserisce punteggi falsi nel file punteggi.dat
 * usato da ScoreStore, in modo che il docente possa subito vedere
 * lo storico degli alunni senza dover giocare partite reali.
 *
 * USO: eseguire UNA VOLTA prima di avviare il gioco:
 *      java InserisciPunteggiTest
 *
 * I punteggi vengono AGGIUNTI a quelli già esistenti (non li cancella).
 */
public class InserisciPunteggiTest {

    public static void main(String[] args) throws Exception {

        // Stessa cartella usata da ScoreStore
        String home    = System.getProperty("user.home");
        String sep     = File.separator;
        String dir     = home + sep + ".matematicagame";
        String filePath = dir + sep + "punteggi.dat";

        new File(dir).mkdirs();

        // Punteggi di test (nomeAlunno|operazione|punteggio|data)
        String[] righe = {
            "Studente|ADDIZIONE|850|10/05/2025 09:15",
            "Studente|SOTTRAZIONE|630|10/05/2025 09:42",
            "Studente|MOLTIPLICAZIONE|920|11/05/2025 10:05",
            "Studente|DIVISIONE|480|11/05/2025 10:33",
            "Studente|ADDIZIONE|770|12/05/2025 08:50",
            "Studente|MOLTIPLICAZIONE|1100|13/05/2025 11:20",
            "Studente|SOTTRAZIONE|550|14/05/2025 09:00",
            "a|ADDIZIONE|400|10/05/2025 10:00",
            "a|DIVISIONE|310|11/05/2025 11:15",
            "a|ADDIZIONE|520|12/05/2025 09:30",
            "a|MOLTIPLICAZIONE|670|13/05/2025 10:45",
            "a|SOTTRAZIONE|290|14/05/2025 08:20",
        };

        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(filePath, true))) {   // append=true
            for (String r : righe) {
                bw.write(r);
                bw.newLine();
            }
        }

        System.out.println("Punteggi di test inseriti in: " + filePath);
        System.out.println("Righe aggiunte: " + righe.length);
    }
}
