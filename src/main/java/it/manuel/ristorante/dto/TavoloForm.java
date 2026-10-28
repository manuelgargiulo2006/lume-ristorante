package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.FormaTavolo;
import it.manuel.ristorante.model.Tavolo;
import it.manuel.ristorante.model.ZonaSala;
import jakarta.validation.constraints.*;

public class TavoloForm {

    @NotNull(message = "Indica il numero")
    @Min(value = 1, message = "Almeno 1")
    @Max(value = 999, message = "Al massimo 999")
    private Integer numero;

    @NotNull(message = "Indica i posti")
    @Min(value = 1, message = "Almeno 1 posto")
    @Max(value = 20, message = "Al massimo 20 posti")
    private Integer posti = 4;

    @NotNull
    private ZonaSala zona = ZonaSala.SALA;

    @NotNull
    private FormaTavolo forma = FormaTavolo.QUADRATO;

    @Min(0)
    @Max(100)
    private int posX = 50;

    @Min(0)
    @Max(100)
    private int posY = 50;

    private boolean attivo = true;

    public static TavoloForm da(Tavolo t) {
        TavoloForm f = new TavoloForm();
        f.setNumero(t.getNumero());
        f.setPosti(t.getPosti());
        f.setZona(t.getZona());
        f.setForma(t.getForma());
        f.setPosX(t.getPosX());
        f.setPosY(t.getPosY());
        f.setAttivo(t.isAttivo());
        return f;
    }

    public Integer getNumero() {
        return numero;
    }

    public Integer getPosti() {
        return posti;
    }

    public ZonaSala getZona() {
        return zona;
    }

    public FormaTavolo getForma() {
        return forma;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public void setPosti(Integer posti) {
        this.posti = posti;
    }

    public void setZona(ZonaSala zona) {
        this.zona = zona;
    }

    public void setForma(FormaTavolo forma) {
        this.forma = forma;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }
}
