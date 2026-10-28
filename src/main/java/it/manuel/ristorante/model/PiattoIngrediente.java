package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Quantità di un ingrediente usata per una porzione di un piatto. */
@Entity
@Table(name = "piatto_ingredienti")
public class PiattoIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piatto_id", nullable = false)
    private Piatto piatto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    /** Quantità per porzione, nell'unità di misura dell'ingrediente. */
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantita;

    public PiattoIngrediente() {
    }

    public Long getId() {
        return id;
    }

    public Piatto getPiatto() {
        return piatto;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public BigDecimal getQuantita() {
        return quantita;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPiatto(Piatto piatto) {
        this.piatto = piatto;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public void setQuantita(BigDecimal quantita) {
        this.quantita = quantita;
    }
}
