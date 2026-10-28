package it.manuel.ristorante.dto;

import java.math.BigDecimal;

/**
 * Numeri principali di un periodo.
 *
 * @param variazione variazione percentuale dell'incasso rispetto al periodo precedente (null se non calcolabile)
 */
public record Riepilogo(BigDecimal incasso, int conti, int coperti, BigDecimal scontrinoMedio,
                        BigDecimal spesaPerCoperto, Integer variazione) {
}
