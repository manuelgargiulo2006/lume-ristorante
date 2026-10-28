package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimenti_magazzino")
public class MovimentoMagazzino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimento tipo;

    /** Positiva per i carichi, negativa per scarichi e sprechi. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantita;

    @Column(nullable = false)
    private LocalDateTime data = LocalDateTime.now();

    @Column(length = 200)
    private String nota;

    public MovimentoMagazzino(Ingrediente ingrediente, TipoMovimento tipo, BigDecimal quantita, String nota) {
        this.ingrediente = ingrediente;
        this.tipo = tipo;
        this.quantita = quantita;
        this.nota = nota;
    }

    public MovimentoMagazzino() {
    }

    public Long getId() {
        return id;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public BigDecimal getQuantita() {
        return quantita;
    }

    public LocalDateTime getData() {
        return data;
    }

    public String getNota() {
        return nota;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public void setQuantita(BigDecimal quantita) {
        this.quantita = quantita;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
