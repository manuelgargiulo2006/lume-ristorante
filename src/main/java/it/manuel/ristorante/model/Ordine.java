package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Comanda di un tavolo: dall'apertura al pagamento del conto. */
@Entity
@Table(name = "ordini")
public class Ordine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tavolo_id", nullable = false)
    private Tavolo tavolo;

    @Column(nullable = false)
    private int coperti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatoOrdine stato = StatoOrdine.APERTO;

    @Column(name = "aperto_il", nullable = false)
    private LocalDateTime apertoIl = LocalDateTime.now();

    @Column(name = "chiuso_il")
    private LocalDateTime chiusoIl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cameriere_id")
    private Staff cameriere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prenotazione_id")
    private Prenotazione prenotazione;

    /** Prezzo del coperto al momento dell'apertura. */
    @Column(name = "coperto_unitario", nullable = false, precision = 6, scale = 2)
    private BigDecimal copertoUnitario = BigDecimal.ZERO;

    @Column(name = "sconto_percentuale", nullable = false)
    private int scontoPercentuale;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", length = 20)
    private MetodoPagamento metodoPagamento;

    /** Totale incassato, salvato alla chiusura. */
    @Column(precision = 10, scale = 2)
    private BigDecimal totale;

    @Column(length = 300)
    private String note;

    @OneToMany(mappedBy = "ordine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RigaOrdine> righe = new ArrayList<>();

    public void aggiungiRiga(RigaOrdine riga) {
        riga.setOrdine(this);
        righe.add(riga);
    }

    public BigDecimal getSubtotale() {
        return righe.stream()
                .map(RigaOrdine::getImporto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getImportoCoperti() {
        return copertoUnitario.multiply(BigDecimal.valueOf(coperti));
    }

    public BigDecimal getImportoSconto() {
        return getSubtotale().add(getImportoCoperti())
                .multiply(BigDecimal.valueOf(scontoPercentuale))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Totale da pagare in questo momento. */
    public BigDecimal getTotaleCorrente() {
        return getSubtotale().add(getImportoCoperti()).subtract(getImportoSconto())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int getNumeroPiatti() {
        return righe.stream().mapToInt(RigaOrdine::getQuantita).sum();
    }

    public long getMinutiAlTavolo() {
        LocalDateTime fine = chiusoIl != null ? chiusoIl : LocalDateTime.now();
        return Duration.between(apertoIl, fine).toMinutes();
    }

    public boolean isAttivo() {
        return stato == StatoOrdine.APERTO || stato == StatoOrdine.CONTO;
    }

    public boolean haRigheDaInviare() {
        return righe.stream().anyMatch(r -> r.getStato() == StatoRiga.IN_ATTESA);
    }

    public boolean haPiattiPronti() {
        return righe.stream().anyMatch(r -> r.getStato() == StatoRiga.PRONTO);
    }

    public Ordine() {
    }

    public Long getId() {
        return id;
    }

    public Tavolo getTavolo() {
        return tavolo;
    }

    public int getCoperti() {
        return coperti;
    }

    public StatoOrdine getStato() {
        return stato;
    }

    public LocalDateTime getApertoIl() {
        return apertoIl;
    }

    public LocalDateTime getChiusoIl() {
        return chiusoIl;
    }

    public Staff getCameriere() {
        return cameriere;
    }

    public Prenotazione getPrenotazione() {
        return prenotazione;
    }

    public BigDecimal getCopertoUnitario() {
        return copertoUnitario;
    }

    public int getScontoPercentuale() {
        return scontoPercentuale;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public BigDecimal getTotale() {
        return totale;
    }

    public String getNote() {
        return note;
    }

    public List<RigaOrdine> getRighe() {
        return righe;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTavolo(Tavolo tavolo) {
        this.tavolo = tavolo;
    }

    public void setCoperti(int coperti) {
        this.coperti = coperti;
    }

    public void setStato(StatoOrdine stato) {
        this.stato = stato;
    }

    public void setApertoIl(LocalDateTime apertoIl) {
        this.apertoIl = apertoIl;
    }

    public void setChiusoIl(LocalDateTime chiusoIl) {
        this.chiusoIl = chiusoIl;
    }

    public void setCameriere(Staff cameriere) {
        this.cameriere = cameriere;
    }

    public void setPrenotazione(Prenotazione prenotazione) {
        this.prenotazione = prenotazione;
    }

    public void setCopertoUnitario(BigDecimal copertoUnitario) {
        this.copertoUnitario = copertoUnitario;
    }

    public void setScontoPercentuale(int scontoPercentuale) {
        this.scontoPercentuale = scontoPercentuale;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public void setTotale(BigDecimal totale) {
        this.totale = totale;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setRighe(List<RigaOrdine> righe) {
        this.righe = righe;
    }
}
