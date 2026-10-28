package it.manuel.ristorante.model;

import jakarta.persistence.*;

/** Sezione del menù (antipasti, primi, ...). */
@Entity
@Table(name = "categorie")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(length = 200)
    private String sottotitolo;

    /** Ordine di comparsa nel menù. */
    @Column(nullable = false)
    private int posizione;

    /** Uscita predefinita in cucina (1 = antipasti, 2 = primi/secondi, 3 = dessert). */
    @Column(nullable = false)
    private int uscita = 1;

    public Categoria(String nome, String sottotitolo, int posizione, int uscita) {
        this.nome = nome;
        this.sottotitolo = sottotitolo;
        this.posizione = posizione;
        this.uscita = uscita;
    }

    public Categoria() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSottotitolo() {
        return sottotitolo;
    }

    public int getPosizione() {
        return posizione;
    }

    public int getUscita() {
        return uscita;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setSottotitolo(String sottotitolo) {
        this.sottotitolo = sottotitolo;
    }

    public void setPosizione(int posizione) {
        this.posizione = posizione;
    }

    public void setUscita(int uscita) {
        this.uscita = uscita;
    }

    /** Due categorie sono la stessa se hanno lo stesso id (serve per usarle come chiavi di una mappa). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria altra)) return false;
        return id != null && id.equals(altra.id);
    }

    @Override
    public int hashCode() {
        return Categoria.class.hashCode();
    }
}
