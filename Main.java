import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("=== ERRORE FATALE ===");
                e.printStackTrace();
                try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameWindow();
            }
        });
    }
}
