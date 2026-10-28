package it.manuel.ristorante.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calcolo del conto: piatti, coperto e sconto. */
class OrdineTest {

    private static RigaOrdine riga(String prezzo, int quantita, StatoRiga stato) {
        RigaOrdine r = new RigaOrdine();
        r.setPrezzoUnitario(new BigDecimal(prezzo));
        r.setQuantita(quantita);
        r.setStato(stato);
        return r;
    }

    private static Ordine ordine(int coperti) {
        Ordine o = new Ordine();
        o.setCoperti(coperti);
        o.setCopertoUnitario(new BigDecimal("3.50"));
        return o;
    }

    @Test
    void totaleConCoperto() {
        Ordine o = ordine(2);
        o.aggiungiRiga(riga("19.00", 2, StatoRiga.SERVITO));
        o.aggiungiRiga(riga("12.00", 1, StatoRiga.SERVITO));

        assertEquals(new BigDecimal("50.00"), o.getSubtotale());
        assertEquals(new BigDecimal("7.00"), o.getImportoCoperti());
        assertEquals(new BigDecimal("57.00"), o.getTotaleCorrente());
        assertEquals(3, o.getNumeroPiatti());
    }

    @Test
    void scontoSuPiattiECoperto() {
        Ordine o = ordine(4);
        o.aggiungiRiga(riga("25.00", 4, StatoRiga.SERVITO));
        o.setScontoPercentuale(10);

        // (100 + 14) - 10% = 102,60
        assertEquals(new BigDecimal("11.40"), o.getImportoSconto());
        assertEquals(new BigDecimal("102.60"), o.getTotaleCorrente());
    }

    @Test
    void piattiDaInviareEPronti() {
        Ordine o = ordine(2);
        o.aggiungiRiga(riga("10.00", 1, StatoRiga.IN_ATTESA));
        assertTrue(o.haRigheDaInviare());
        assertFalse(o.haPiattiPronti());

        o.aggiungiRiga(riga("10.00", 1, StatoRiga.PRONTO));
        assertTrue(o.haPiattiPronti());
    }
}
