package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Materia prima in magazzino. */
@Entity
@Table(name = "ingredienti")
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnitaMisura unita;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal scorta = BigDecimal.ZERO;

    @Column(name = "soglia_minima", nullable = false, precision = 12, scale = 3)
    private BigDecimal sogliaMinima = BigDecimal.ZERO;

    /** Costo per unità di misura (es. euro al kg). */
    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(length = 80)
    private String fornitore;

    public Ingrediente(String nome, UnitaMisura unita, String scorta, String sogliaMinima,
                       String costoUnitario, String fornitore) {
        this.nome = nome;
        this.unita = unita;
        this.scorta = new BigDecimal(scorta);
        this.sogliaMinima = new BigDecimal(sogliaMinima);
        this.costoUnitario = new BigDecimal(costoUnitario);
        this.fornitore = fornitore;
    }

    public boolean isSottoScorta() {
        return scorta.compareTo(sogliaMinima) < 0;
    }

    public boolean isEsaurito() {
        return scorta.signum() <= 0;
    }

    /** Livello della scorta rispetto a tre volte la soglia, per la barra grafica (0-100). */
    public int getLivello() {
        BigDecimal pieno = sogliaMinima.multiply(BigDecimal.valueOf(3));
        if (pieno.signum() == 0) {
            return 100;
        }
        int valore = scorta.multiply(BigDecimal.valueOf(100)).divide(pieno, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(100, valore));
    }

    /** Valore economico della scorta. */
    public BigDecimal getValore() {
        return scorta.max(BigDecimal.ZERO).multiply(costoUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public Ingrediente() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public UnitaMisura getUnita() {
        return unita;
    }

    public BigDecimal getScorta() {
        return scorta;
    }

    public BigDecimal getSogliaMinima() {
        return sogliaMinima;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public String getFornitore() {
        return fornitore;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setUnita(UnitaMisura unita) {
        this.unita = unita;
    }

    public void setScorta(BigDecimal scorta) {
        this.scorta = scorta;
    }

    public void setSogliaMinima(BigDecimal sogliaMinima) {
        this.sogliaMinima = sogliaMinima;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public void setFornitore(String fornitore) {
        this.fornitore = fornitore;
    }
}
