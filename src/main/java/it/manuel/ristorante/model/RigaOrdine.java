package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "righe_ordine")
public class RigaOrdine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordine_id", nullable = false)
    private Ordine ordine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "piatto_id", nullable = false)
    private Piatto piatto;

    @Column(nullable = false)
    private int quantita = 1;

    /** Prezzo al momento dell'ordine: se poi cambia nel menù il conto non cambia. */
    @Column(name = "prezzo_unitario", nullable = false, precision = 8, scale = 2)
    private BigDecimal prezzoUnitario;

    @Column(length = 200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatoRiga stato = StatoRiga.IN_ATTESA;

    /** Uscita (1 = prima portata, 2 = seconda, 3 = dessert). */
    @Column(nullable = false)
    private int uscita = 1;

    @Column(name = "inviato_il")
    private LocalDateTime inviatoIl;

    @Column(name = "pronto_il")
    private LocalDateTime prontoIl;

    public BigDecimal getImporto() {
        return prezzoUnitario.multiply(BigDecimal.valueOf(quantita));
    }

    public RigaOrdine() {
    }

    public Long getId() {
        return id;
    }

    public Ordine getOrdine() {
        return ordine;
    }

    public Piatto getPiatto() {
        return piatto;
    }

    public int getQuantita() {
        return quantita;
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }

    public String getNote() {
        return note;
    }

    public StatoRiga getStato() {
        return stato;
    }

    public int getUscita() {
        return uscita;
    }

    public LocalDateTime getInviatoIl() {
        return inviatoIl;
    }

    public LocalDateTime getProntoIl() {
        return prontoIl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrdine(Ordine ordine) {
        this.ordine = ordine;
    }

    public void setPiatto(Piatto piatto) {
        this.piatto = piatto;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    public void setPrezzoUnitario(BigDecimal prezzoUnitario) {
        this.prezzoUnitario = prezzoUnitario;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setStato(StatoRiga stato) {
        this.stato = stato;
    }

    public void setUscita(int uscita) {
        this.uscita = uscita;
    }

    public void setInviatoIl(LocalDateTime inviatoIl) {
        this.inviatoIl = inviatoIl;
    }

    public void setProntoIl(LocalDateTime prontoIl) {
        this.prontoIl = prontoIl;
    }
}
