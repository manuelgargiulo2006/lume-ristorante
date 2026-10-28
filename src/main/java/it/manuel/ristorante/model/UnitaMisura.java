package it.manuel.ristorante.model;

/** Unità di misura degli ingredienti. */
public enum UnitaMisura {
    KG("kg", ""),
    G("g", ""),
    L("l", ""),
    ML("ml", ""),
    PZ("pz", "");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    UnitaMisura(String label, String tone) {
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
