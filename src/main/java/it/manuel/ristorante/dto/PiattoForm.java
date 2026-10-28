package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.Allergene;
import it.manuel.ristorante.model.Piatto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PiattoForm {

    @NotBlank(message = "Inserisci il nome del piatto")
    @Size(max = 100, message = "Massimo 100 caratteri")
    private String nome;

    @Size(max = 600, message = "Massimo 600 caratteri")
    private String descrizione;

    @Size(max = 200, message = "Massimo 200 caratteri")
    private String abbinamento;

    @NotNull(message = "Inserisci il prezzo")
    @DecimalMin(value = "0.50", message = "Il prezzo minimo è 0,50 €")
    @DecimalMax(value = "999.00", message = "Il prezzo massimo è 999 €")
    @Digits(integer = 3, fraction = 2, message = "Al massimo due decimali")
    private BigDecimal prezzo;

    @NotNull(message = "Scegli la categoria")
    private Long categoriaId;

    private Set<Allergene> allergeni = EnumSet.noneOf(Allergene.class);

    private boolean vegetariano;

    private boolean vegano;

    private boolean disponibile = true;

    private boolean inEvidenza;

    @Min(value = 1, message = "Almeno 1 minuto")
    @Max(value = 240, message = "Al massimo 240 minuti")
    private int tempoPreparazione = 15;

    /** Ricetta: righe ingrediente + quantità per porzione. */
    private List<RigaRicetta> ricetta = new ArrayList<>();

    public static class RigaRicetta {
        private Long ingredienteId;
        private BigDecimal quantita;

        public Long getIngredienteId() {
            return ingredienteId;
        }

        public void setIngredienteId(Long ingredienteId) {
            this.ingredienteId = ingredienteId;
        }

        public BigDecimal getQuantita() {
            return quantita;
        }

        public void setQuantita(BigDecimal quantita) {
            this.quantita = quantita;
        }
    }

    public static PiattoForm da(Piatto p) {
        PiattoForm f = new PiattoForm();
        f.setNome(p.getNome());
        f.setDescrizione(p.getDescrizione());
        f.setAbbinamento(p.getAbbinamento());
        f.setPrezzo(p.getPrezzo());
        f.setCategoriaId(p.getCategoria().getId());
        f.setAllergeni(p.getAllergeni().isEmpty() ? EnumSet.noneOf(Allergene.class) : EnumSet.copyOf(p.getAllergeni()));
        f.setVegetariano(p.isVegetariano());
        f.setVegano(p.isVegano());
        f.setDisponibile(p.isDisponibile());
        f.setInEvidenza(p.isInEvidenza());
        f.setTempoPreparazione(p.getTempoPreparazione());
        p.getRicetta().forEach(pi -> {
            RigaRicetta r = new RigaRicetta();
            r.setIngredienteId(pi.getIngrediente().getId());
            r.setQuantita(pi.getQuantita());
            f.getRicetta().add(r);
        });
        return f;
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

    public Long getCategoriaId() {
        return categoriaId;
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

    public List<RigaRicetta> getRicetta() {
        return ricetta;
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

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
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

    public void setRicetta(List<RigaRicetta> ricetta) {
        this.ricetta = ricetta;
    }
}
