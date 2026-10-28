package it.manuel.ristorante.dto;

import java.time.LocalTime;

/** Orario prenotabile con i posti ancora liberi. */
public record FasciaOraria(LocalTime ora, String servizio, int postiLiberi, boolean disponibile) {

    public String getEtichetta() {
        return String.format("%02d:%02d", ora.getHour(), ora.getMinute());
    }
}
