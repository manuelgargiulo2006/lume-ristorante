package it.manuel.ristorante.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Impostazioni del ristorante lette da application.properties (prefisso "lume").
 *
 * @param coperto            prezzo del coperto a persona
 * @param capienzaPerTurno   numero massimo di persone presenti contemporaneamente
 * @param durataTavoloMinuti durata media di una cena, usata per calcolare la disponibilità
 */
@ConfigurationProperties(prefix = "lume")
public record LumeProperties(BigDecimal coperto, int capienzaPerTurno, int durataTavoloMinuti) {

    public LumeProperties {
        if (coperto == null) {
            coperto = new BigDecimal("3.50");
        }
        if (capienzaPerTurno <= 0) {
            capienzaPerTurno = 40;
        }
        if (durataTavoloMinuti <= 0) {
            durataTavoloMinuti = 120;
        }
    }
}
