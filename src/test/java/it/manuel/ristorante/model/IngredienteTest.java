package it.manuel.ristorante.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Soglie di riordino e valore della merce in magazzino. */
class IngredienteTest {

    @Test
    void sottoScortaEdEsaurito() {
        Ingrediente polpo = new Ingrediente("Polpo", UnitaMisura.KG, "1.4", "3", "22", "Pescheria");
        assertTrue(polpo.isSottoScorta());
        assertFalse(polpo.isEsaurito());

        Ingrediente finito = new Ingrediente("Vongole", UnitaMisura.KG, "0", "2", "16", null);
        assertTrue(finito.isEsaurito());
    }

    @Test
    void livelloRispettoATreVolteLaSoglia() {
        assertEquals(50, new Ingrediente("Riso", UnitaMisura.KG, "3", "2", "5", null).getLivello());
        assertEquals(100, new Ingrediente("Farina", UnitaMisura.KG, "50", "2", "1", null).getLivello());
        assertEquals(0, new Ingrediente("Uova", UnitaMisura.PZ, "0", "60", "0.35", null).getLivello());
    }

    @Test
    void valoreDellaScorta() {
        Ingrediente olio = new Ingrediente("Olio", UnitaMisura.L, "12", "6", "12.00", null);
        assertEquals(0, new BigDecimal("144").compareTo(olio.getValore()));
    }
}
