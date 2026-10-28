package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.Ingrediente;
import it.manuel.ristorante.model.UnitaMisura;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class IngredienteForm {

    @NotBlank(message = "Inserisci il nome")
    @Size(max = 80, message = "Massimo 80 caratteri")
    private String nome;

    @NotNull(message = "Scegli l’unità di misura")
    private UnitaMisura unita = UnitaMisura.KG;

    @NotNull(message = "Indica la scorta attuale")
    @DecimalMin(value = "0", message = "La scorta non può essere negativa")
    private BigDecimal scorta = BigDecimal.ZERO;

    @NotNull(message = "Indica la soglia minima")
    @DecimalMin(value = "0", message = "La soglia non può essere negativa")
    private BigDecimal sogliaMinima = BigDecimal.ONE;

    @NotNull(message = "Indica il costo")
    @DecimalMin(value = "0", message = "Il costo non può essere negativo")
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Size(max = 80, message = "Massimo 80 caratteri")
    private String fornitore;

    public static IngredienteForm da(Ingrediente i) {
        IngredienteForm f = new IngredienteForm();
        f.setNome(i.getNome());
        f.setUnita(i.getUnita());
        f.setScorta(i.getScorta());
        f.setSogliaMinima(i.getSogliaMinima());
        f.setCostoUnitario(i.getCostoUnitario());
        f.setFornitore(i.getFornitore());
        return f;
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
