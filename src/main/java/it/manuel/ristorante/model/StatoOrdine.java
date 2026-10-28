package it.manuel.ristorante.model;

/** Stato di una comanda. */
public enum StatoOrdine {
    APERTO("Aperto", "open"),
    CONTO("Conto richiesto", "bill"),
    CHIUSO("Pagato", "closed"),
    ANNULLATO("Annullato", "cancelled");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    StatoOrdine(String label, String tone) {
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
