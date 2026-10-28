package it.manuel.ristorante.model;

/** I 14 allergeni del Reg. UE 1169/2011. */
public enum Allergene {
    GLUTINE("Glutine", "G"),
    CROSTACEI("Crostacei", "Cr"),
    UOVA("Uova", "U"),
    PESCE("Pesce", "P"),
    ARACHIDI("Arachidi", "A"),
    SOIA("Soia", "S"),
    LATTE("Latte", "L"),
    FRUTTA_A_GUSCIO("Frutta a guscio", "FG"),
    SEDANO("Sedano", "Se"),
    SENAPE("Senape", "Sn"),
    SESAMO("Sesamo", "Ss"),
    SOLFITI("Solfiti", "So"),
    LUPINI("Lupini", "Lu"),
    MOLLUSCHI("Molluschi", "M");

    private final String label;
    /** Classe CSS o informazione aggiuntiva usata nelle pagine. */
    private final String tone;

    Allergene(String label, String tone) {
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
