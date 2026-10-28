package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Membro dello staff che accede al gestionale. */
@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(name = "nome_completo", nullable = false, length = 80)
    private String nomeCompleto;

    /** Password cifrata con BCrypt. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ruolo ruolo;

    @Column(nullable = false)
    private boolean attivo = true;

    @Column(name = "creato_il", nullable = false)
    private LocalDateTime creatoIl = LocalDateTime.now();

    public Staff(String username, String nomeCompleto, String password, Ruolo ruolo) {
        this.username = username;
        this.nomeCompleto = nomeCompleto;
        this.password = password;
        this.ruolo = ruolo;
    }

    /** Iniziali per l'avatar (es. "Giulia Russo" -> "GR"). */
    public String getIniziali() {
        String[] parti = nomeCompleto.trim().split("\\s+");
        String iniziali = parti[0].substring(0, 1)
                + (parti.length > 1 ? parti[parti.length - 1].substring(0, 1) : "");
        return iniziali.toUpperCase();
    }

    public Staff() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getPassword() {
        return password;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public void setCreatoIl(LocalDateTime creatoIl) {
        this.creatoIl = creatoIl;
    }
}
