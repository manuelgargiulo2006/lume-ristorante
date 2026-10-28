package it.manuel.ristorante.service;

import java.util.Locale;

/** Piccole utility per i testi inseriti dagli utenti. */
public final class Testi {

    private Testi() {
    }

    /** Toglie spazi superflui e mette la maiuscola iniziale: "  tartare di  ricciola" -> "Tartare di ricciola". */
    public static String nome(String valore) {
        String pulito = valore == null ? "" : valore.trim().replaceAll("\\s+", " ");
        if (pulito.isEmpty()) {
            throw new OperazioneNonValidaException("Il nome non può essere vuoto");
        }
        return pulito.substring(0, 1).toUpperCase(Locale.ITALIAN) + pulito.substring(1);
    }

    /** Restituisce null per le stringhe vuote, altrimenti il testo senza spazi agli estremi. */
    public static String facoltativo(String valore) {
        if (valore == null || valore.isBlank()) {
            return null;
        }
        return valore.trim();
    }

    /** Ricerca "morbida": ignora maiuscole e accenti. */
    public static String normalizza(String valore) {
        if (valore == null) {
            return "";
        }
        return java.text.Normalizer.normalize(valore.toLowerCase(Locale.ITALIAN), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }
}
