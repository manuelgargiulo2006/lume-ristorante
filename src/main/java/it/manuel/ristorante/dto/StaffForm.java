package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.Staff;
import jakarta.validation.constraints.*;

public class StaffForm {

    @NotBlank(message = "Inserisci il nome utente")
    @Size(min = 3, max = 40, message = "Da 3 a 40 caratteri")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Solo lettere, numeri, punto e trattini")
    private String username;

    @NotBlank(message = "Inserisci nome e cognome")
    @Size(max = 80, message = "Massimo 80 caratteri")
    private String nomeCompleto;

    @NotNull(message = "Scegli il ruolo")
    private Ruolo ruolo = Ruolo.SALA;

    /** Obbligatoria solo alla creazione: in modifica, se vuota, resta quella attuale. */
    @Size(max = 72, message = "Massimo 72 caratteri")
    private String password;

    private boolean attivo = true;

    public static StaffForm da(Staff s) {
        StaffForm f = new StaffForm();
        f.setUsername(s.getUsername());
        f.setNomeCompleto(s.getNomeCompleto());
        f.setRuolo(s.getRuolo());
        f.setAttivo(s.isAttivo());
        return f;
    }

    public String getUsername() {
        return username;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }
}
