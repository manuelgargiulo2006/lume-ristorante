package it.manuel.ristorante.dto;

import java.util.List;

/** Comanda come la vede la cucina sul monitor (serializzata in JSON). */
public record TicketCucina(Long ordineId, int tavolo, int coperti, String cameriere,
                           long minutiAttesa, String urgenza, List<RigaCucina> righe) {

    public record RigaCucina(Long id, String piatto, int quantita, String note, String stato,
                             String statoLabel, int uscita, long minuti) {
    }
}
