package it.manuel.ristorante.model;

/** Zone del locale. */
public enum ZonaSala {
    SALA("Sala principale", ""),
    TERRAZZA("Terrazza", ""),
    PRIVE("Privé", "");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    ZonaSala(String label, String tone) {
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
