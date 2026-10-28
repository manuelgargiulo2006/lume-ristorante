package it.manuel.ristorante.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "piatti")
public class Piatto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 600)
    private String descrizione;

    /** Vino consigliato in abbinamento. */
    @Column(length = 200)
    private String abbinamento;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal prezzo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "piatto_allergeni", joinColumns = @JoinColumn(name = "piatto_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergene", length = 30)
    private Set<Allergene> allergeni = EnumSet.noneOf(Allergene.class);

    @Column(nullable = false)
    private boolean vegetariano;

    @Column(nullable = false)
    private boolean vegano;

    /** false = esaurito per oggi: non si può ordinare. */
    @Column(nullable = false)
    private boolean disponibile = true;

    /** Piatto firma dello chef, messo in risalto nel menù. */
    @Column(name = "in_evidenza", nullable = false)
    private boolean inEvidenza;

    @Column(name = "tempo_preparazione", nullable = false)
    private int tempoPreparazione = 15;

    @Column(nullable = false)
    private int posizione;

    /** Ingredienti scalati dal magazzino a ogni porzione venduta. */
    @OneToMany(mappedBy = "piatto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PiattoIngrediente> ricetta = new ArrayList<>();

    public void aggiungiIngrediente(Ingrediente ingrediente, BigDecimal quantita) {
        PiattoIngrediente pi = new PiattoIngrediente();
        pi.setPiatto(this);
        pi.setIngrediente(ingrediente);
        pi.setQuantita(quantita);
        ricetta.add(pi);
    }

    /** Costo delle materie prime per porzione. */
    public BigDecimal getFoodCost() {
        BigDecimal totale = BigDecimal.ZERO;
        for (PiattoIngrediente pi : ricetta) {
            totale = totale.add(pi.getIngrediente().getCostoUnitario().multiply(pi.getQuantita()));
        }
        return totale.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Incidenza del costo delle materie prime sul prezzo (in percentuale). */
    public int getFoodCostPercentuale() {
        if (prezzo == null || prezzo.signum() == 0) {
            return 0;
        }
        return getFoodCost().multiply(BigDecimal.valueOf(100))
                .divide(prezzo, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public Piatto() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getAbbinamento() {
        return abbinamento;
    }

    public BigDecimal getPrezzo() {
        return prezzo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Set<Allergene> getAllergeni() {
        return allergeni;
    }

    public boolean isVegetariano() {
        return vegetariano;
    }

    public boolean isVegano() {
        return vegano;
    }

    public boolean isDisponibile() {
        return disponibile;
    }

    public boolean isInEvidenza() {
        return inEvidenza;
    }

    public int getTempoPreparazione() {
        return tempoPreparazione;
    }

    public int getPosizione() {
        return posizione;
    }

    public List<PiattoIngrediente> getRicetta() {
        return ricetta;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public void setAbbinamento(String abbinamento) {
        this.abbinamento = abbinamento;
    }

    public void setPrezzo(BigDecimal prezzo) {
        this.prezzo = prezzo;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setAllergeni(Set<Allergene> allergeni) {
        this.allergeni = allergeni;
    }

    public void setVegetariano(boolean vegetariano) {
        this.vegetariano = vegetariano;
    }

    public void setVegano(boolean vegano) {
        this.vegano = vegano;
    }

    public void setDisponibile(boolean disponibile) {
        this.disponibile = disponibile;
    }

    public void setInEvidenza(boolean inEvidenza) {
        this.inEvidenza = inEvidenza;
    }

    public void setTempoPreparazione(int tempoPreparazione) {
        this.tempoPreparazione = tempoPreparazione;
    }

    public void setPosizione(int posizione) {
        this.posizione = posizione;
    }

    public void setRicetta(List<PiattoIngrediente> ricetta) {
        this.ricetta = ricetta;
    }

    /** Due piatti sono lo stesso se hanno lo stesso id (servono come chiavi nelle statistiche). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piatto altro)) return false;
        return id != null && id.equals(altro.id);
    }

    @Override
    public int hashCode() {
        return Piatto.class.hashCode();
    }
}
