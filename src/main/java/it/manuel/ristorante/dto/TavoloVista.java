package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.Ordine;
import it.manuel.ristorante.model.Prenotazione;
import it.manuel.ristorante.model.StatoOrdine;
import it.manuel.ristorante.model.Tavolo;

/**
 * Tavolo con il suo stato in tempo reale, per la mappa della sala.
 *
 * @param ordine   comanda aperta (null se il tavolo è libero)
 * @param prossima prossima prenotazione di oggi assegnata al tavolo (può essere null)
 */
public record TavoloVista(Tavolo tavolo, Ordine ordine, Prenotazione prossima) {

    /** libero, occupato, conto, pronto (piatti pronti da servire), prenotato. */
    public String stato() {
        if (ordine != null) {
            if (ordine.getStato() == StatoOrdine.CONTO) {
                return "conto";
            }
            return ordine.haPiattiPronti() ? "pronto" : "occupato";
        }
        return prossima != null ? "prenotato" : "libero";
    }

    public String etichettaStato() {
        return switch (stato()) {
            case "conto" -> "Conto richiesto";
            case "pronto" -> "Piatti pronti";
            case "occupato" -> "Occupato";
            case "prenotato" -> "Prenotato";
            default -> "Libero";
        };
    }
}
