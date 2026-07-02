/**
 * User
 * ----
 * Rappresenta un utente del sistema con nome, password e ruolo.
 * Gli alunni possono avere un docente assegnato.
 */
public class User {

    public enum Ruolo { PRESIDE, DOCENTE, ALUNNO }

    private final String nome;
    private final String password;
    private final Ruolo  ruolo;

    // Solo per gli alunni: nome del docente assegnato (null = nessuno)
    private String docenteAssegnato;

    public User(String nome, String password, Ruolo ruolo) {
        this.nome     = nome.trim();
        this.password = password;
        this.ruolo    = ruolo;
        this.docenteAssegnato = null;
    }

    public String getNome()     { return nome; }
    public String getPassword() { return password; }
    public Ruolo  getRuolo()    { return ruolo; }

    public String  getDocenteAssegnato()         { return docenteAssegnato; }
    public void    setDocenteAssegnato(String d) { this.docenteAssegnato = d; }
    public boolean hasDocente()                  { return docenteAssegnato != null && !docenteAssegnato.isEmpty(); }

    public boolean matchesNome(String n) {
        return nome.equalsIgnoreCase(n == null ? "" : n.trim());
    }
    public boolean matchesPassword(String pw) { return password.equals(pw); }

    @Override
    public String toString() {
        return ruolo + ":" + nome + (docenteAssegnato != null ? "(@" + docenteAssegnato + ")" : "");
    }
}
