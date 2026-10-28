package it.manuel.ristorante.model;

/** Ciclo di vita di una prenotazione. */
public enum StatoPrenotazione {
    RICHIESTA("Da confermare", "pending"),
    CONFERMATA("Confermata", "confirmed"),
    ARRIVATA("Al tavolo", "seated"),
    COMPLETATA("Completata", "done"),
    NO_SHOW("Non presentato", "noshow"),
    CANCELLATA("Cancellata", "cancelled");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    StatoPrenotazione(String label, String tone) {
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
