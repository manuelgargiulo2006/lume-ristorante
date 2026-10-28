package it.manuel.ristorante.service;

/** Errore con un messaggio da mostrare all'utente (es. "il tavolo 4 è già occupato"). */
public class OperazioneNonValidaException extends RuntimeException {

    public OperazioneNonValidaException(String message) {
        super(message);
    }
}
