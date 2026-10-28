package it.manuel.ristorante.model;

/** Ruoli dello staff: decidono quali sezioni del gestionale si vedono. */
public enum Ruolo {
    ADMIN("Amministratore", "wine"),
    SALA("Sala", "brass"),
    CUCINA("Cucina", "sage");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    Ruolo(String label, String tone) {
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
