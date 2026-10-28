package it.manuel.ristorante.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Formati italiani usati nelle pagine. */
class FormatiTest {

    private final Formati fmt = new Formati();

    @Test
    void importi() {
        assertEquals("€ 1.234,50", fmt.euro(new BigDecimal("1234.5")));
        assertEquals("€ 1.235", fmt.euroTondo(new BigDecimal("1234.5")));
        assertEquals("€ 0,00", fmt.euro(null));
        assertEquals("€ 28,75", fmt.quota(new BigDecimal("115.00"), 4));
    }

    @Test
    void quantitaEDurate() {
        assertEquals("2,5", fmt.quantita(new BigDecimal("2.500")));
        assertEquals("45 min", fmt.durata(45));
        assertEquals("1 h 15 min", fmt.durata(75));
        assertEquals("2 h", fmt.durata(120));
    }

    @Test
    void percentuali() {
        assertEquals("12,5%", fmt.percentuale(new BigDecimal("25"), new BigDecimal("200")));
        assertEquals("0%", fmt.percentuale(BigDecimal.ONE, BigDecimal.ZERO));
    }
}
