package it.manuel.ristorante.model;

/** Avanzamento di un piatto in cucina. */
public enum StatoRiga {
    IN_ATTESA("Da inviare", "draft"),
    INVIATO("In coda", "queued"),
    IN_PREPARAZIONE("In preparazione", "cooking"),
    PRONTO("Pronto", "ready"),
    SERVITO("Servito", "served");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    StatoRiga(String label, String tone) {
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
