package it.manuel.ristorante.model;

/** Forma del tavolo sulla mappa della sala. */
public enum FormaTavolo {
    ROTONDO("Rotondo", ""),
    QUADRATO("Quadrato", ""),
    RETTANGOLARE("Rettangolare", "");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    FormaTavolo(String label, String tone) {
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
