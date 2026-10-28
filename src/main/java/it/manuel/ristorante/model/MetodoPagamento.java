package it.manuel.ristorante.model;

/** Metodi di pagamento accettati. */
public enum MetodoPagamento {
    CARTA("Carta", ""),
    CONTANTI("Contanti", ""),
    SATISPAY("Satispay", ""),
    BUONO("Buono pasto", "");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    MetodoPagamento(String label, String tone) {
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
