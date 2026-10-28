package it.manuel.ristorante.model;

/** Movimenti di magazzino. */
public enum TipoMovimento {
    CARICO("Carico", "in"),
    SCARICO_VENDITA("Scarico vendita", "out"),
    SPRECO("Spreco", "waste"),
    RETTIFICA("Rettifica inventario", "adjust");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    TipoMovimento(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }
}
