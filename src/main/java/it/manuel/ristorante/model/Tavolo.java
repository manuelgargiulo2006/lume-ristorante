package it.manuel.ristorante.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tavoli")
public class Tavolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false)
    private int posti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ZonaSala zona = ZonaSala.SALA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaTavolo forma = FormaTavolo.QUADRATO;

    /** Posizione sulla mappa della sala, in percentuale (0-100). */
    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(nullable = false)
    private boolean attivo = true;

    public Tavolo(int numero, int posti, ZonaSala zona, FormaTavolo forma, int posX, int posY) {
        this.numero = numero;
        this.posti = posti;
        this.zona = zona;
        this.forma = forma;
        this.posX = posX;
        this.posY = posY;
    }

    public Tavolo() {
    }

    public Long getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public int getPosti() {
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public void setPosti(int posti) {
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
