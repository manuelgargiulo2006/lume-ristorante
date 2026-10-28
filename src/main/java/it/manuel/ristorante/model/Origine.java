package it.manuel.ristorante.model;

/** Canale da cui è arrivata la prenotazione. */
public enum Origine {
    ONLINE("Sito web", ""),
    TELEFONO("Telefono", ""),
    SALA("Di persona", "");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    Origine(String label, String tone) {
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
