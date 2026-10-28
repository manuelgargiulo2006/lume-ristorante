package it.manuel.ristorante.config;

import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.Staff;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Utente autenticato: oltre alle credenziali conserva id, nome e ruolo per le pagine. */
public class StaffUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String nomeCompleto;
    private final String iniziali;
    private final Ruolo ruolo;
    private final boolean attivo;

    public StaffUserDetails(Staff staff) {
        this.id = staff.getId();
        this.username = staff.getUsername();
        this.password = staff.getPassword();
        this.nomeCompleto = staff.getNomeCompleto();
        this.iniziali = staff.getIniziali();
        this.ruolo = staff.getRuolo();
        this.attivo = staff.isAttivo();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + ruolo.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return attivo;
    }

    public Long getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getIniziali() {
        return iniziali;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public boolean isAdmin() {
        return ruolo == Ruolo.ADMIN;
    }
}
