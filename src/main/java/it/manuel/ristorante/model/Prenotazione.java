package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "prenotazioni", indexes = @Index(name = "idx_prenotazioni_data", columnList = "data"))
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codice da comunicare al cliente per gestire la prenotazione (es. LM-7K2Q). */
    @Column(nullable = false, unique = true, length = 12)
    private String codice;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime ora;

    @Column(nullable = false)
    private int persone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tavolo_id")
    private Tavolo tavolo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatoPrenotazione stato = StatoPrenotazione.RICHIESTA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origine origine = Origine.ONLINE;

    /** Allergie, occasioni speciali, richieste. */
    @Column(length = 400)
    private String note;

    @Column(name = "creata_il", nullable = false)
    private LocalDateTime creataIl = LocalDateTime.now();

    public LocalDateTime getInizio() {
        return LocalDateTime.of(data, ora);
    }

    public boolean isAttiva() {
        return stato == StatoPrenotazione.RICHIESTA || stato == StatoPrenotazione.CONFERMATA
                || stato == StatoPrenotazione.ARRIVATA;
    }

    public Prenotazione() {
    }

    public Long getId() {
        return id;
    }

    public String getCodice() {
        return codice;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getOra() {
        return ora;
    }

    public int getPersone() {
        return persone;
    }

    public Tavolo getTavolo() {
        return tavolo;
    }

    public StatoPrenotazione getStato() {
        return stato;
    }

    public Origine getOrigine() {
        return origine;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreataIl() {
        return creataIl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public void setOra(LocalTime ora) {
        this.ora = ora;
    }

    public void setPersone(int persone) {
        this.persone = persone;
    }

    public void setTavolo(Tavolo tavolo) {
        this.tavolo = tavolo;
    }

    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    public void setOrigine(Origine origine) {
        this.origine = origine;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setCreataIl(LocalDateTime creataIl) {
        this.creataIl = creataIl;
    }
}
