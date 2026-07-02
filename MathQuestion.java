import java.util.Random;

/**
 * MathQuestion
 * ------------
 * Genera una singola operazione matematica, sia in forma diretta
 * ("a op b = ?") che con incognita ("a op ? = c").
 *
 * La difficolta' controlla il range dei numeri coinvolti.
 */
public class MathQuestion {

    public int    answer;        // risposta corretta
    public String questionText;  // testo da mostrare in alto

    private static final Random rand = new Random();

    /**
     * @param operation  0=addizione, 1=sottrazione, 2=moltiplicazione, 3=divisione
     * @param difficulty 0=facile, 1=medio, 2=difficile
     */
    public MathQuestion(int operation, int difficulty) {

        // Range numeri in base alla difficolta' (risultati fino a 100)
        int max;
        switch (difficulty) {
            case 0:  max = 10; break;  // facile:   1-10  (addizione max 20, ecc.)
            case 1:  max = 50; break;  // medio:    1-50
            default: max = 100; break; // difficile:1-100
        }

        // Per moltiplicazione e divisione limita sempre a 10 per evitare numeri troppo grandi
        int maxMolDiv = Math.min(max, 10);

        int a = rand.nextInt(max) + 1;
        int b = rand.nextInt(max) + 1;

        // 50% di probabilita' di domanda con incognita
        boolean withUnknown = rand.nextBoolean();

        switch (operation) {

            case 0: // --- Addizione ---
                answer = a + b;
                if (withUnknown) {
                    questionText = a + " + ? = " + answer;
                    answer = b;
                } else {
                    questionText = a + " + " + b + " = ?";
                }
                break;

            case 1: // --- Sottrazione (risultato sempre >= 0) ---
                if (a < b) { int tmp = a; a = b; b = tmp; }
                answer = a - b;
                if (withUnknown) {
                    questionText = a + " - ? = " + answer;
                    answer = b;
                } else {
                    questionText = a + " - " + b + " = ?";
                }
                break;

            case 2: // --- Moltiplicazione (max 10 per fattore) ---
                a = rand.nextInt(maxMolDiv) + 1;
                b = rand.nextInt(maxMolDiv) + 1;
                answer = a * b;
                if (withUnknown) {
                    questionText = a + " x ? = " + answer;
                    answer = b;
                } else {
                    questionText = a + " x " + b + " = ?";
                }
                break;

            case 3: // --- Divisione (sempre esatta, max 10) ---
                a = rand.nextInt(maxMolDiv) + 1;
                b = rand.nextInt(maxMolDiv) + 1;
                // dividendo = a * b, divisore = b, quoziente = a
                int dividend = a * b;
                if (withUnknown) {
                    // dividend / ? = a  =>  ? = b
                    questionText = dividend + " / ? = " + a;
                    answer = b;
                } else {
                    questionText = dividend + " / " + b + " = ?";
                    answer = a;
                }
                break;

            default:
                questionText = "1 + 1 = ?";
                answer = 2;
        }
    }
}
