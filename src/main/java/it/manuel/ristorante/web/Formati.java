package it.manuel.ristorante.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Formattazione di importi e date usata nei template: {@code ${@fmt.euro(ordine.totale)}}.
 */
@Component("fmt")
public class Formati {

    private static final DecimalFormatSymbols IT = DecimalFormatSymbols.getInstance(Locale.ITALY);

    /** 1234.5 → "€ 1.234,50" */
    public String euro(BigDecimal valore) {
        return "€ " + new DecimalFormat("#,##0.00", IT).format(valore == null ? BigDecimal.ZERO : valore);
    }

    /** 1234.5 → "€ 1.235" (per i numeri grandi delle statistiche) */
    public String euroTondo(BigDecimal valore) {
        return "€ " + new DecimalFormat("#,##0", IT).format(
                (valore == null ? BigDecimal.ZERO : valore).setScale(0, RoundingMode.HALF_UP));
    }

    /** 1234.5 → "1.234,50" */
    public String importo(BigDecimal valore) {
        return new DecimalFormat("#,##0.00", IT).format(valore == null ? BigDecimal.ZERO : valore);
    }

    /** Quota a persona per il conto "alla romana". */
    public String quota(BigDecimal totale, int persone) {
        if (totale == null || persone <= 0) {
            return euro(BigDecimal.ZERO);
        }
        return euro(totale.divide(BigDecimal.valueOf(persone), 2, RoundingMode.HALF_UP));
    }

    /** Quanto vale "parte" rispetto a "totale": "1,5%". */
    public String percentuale(BigDecimal parte, BigDecimal totale) {
        if (parte == null || totale == null || totale.signum() == 0) {
            return "0%";
        }
        BigDecimal p = parte.multiply(BigDecimal.valueOf(100)).divide(totale, 2, RoundingMode.HALF_UP);
        return new DecimalFormat("0.##", IT).format(p) + "%";
    }

    /** Quantità di magazzino senza zeri inutili: 2.500 → "2,5" */
    public String quantita(BigDecimal valore) {
        return new DecimalFormat("#,##0.###", IT).format(valore == null ? BigDecimal.ZERO : valore);
    }

    /** Minuti trascorsi in forma leggibile: 75 → "1 h 15 min" */
    public String durata(long minuti) {
        if (minuti < 60) {
            return minuti + " min";
        }
        long m = minuti % 60;
        return (minuti / 60) + " h" + (m == 0 ? "" : " " + m + " min");
    }

    /** "oggi", "domani", "ieri" oppure null. */
    public String giornoRelativo(LocalDate data) {
        long diff = ChronoUnit.DAYS.between(LocalDate.now(), data);
        return diff == 0 ? "Oggi" : diff == 1 ? "Domani" : diff == -1 ? "Ieri" : null;
    }

    /** Tempo trascorso da un istante: "3 min fa", "2 h fa". */
    public String fa(LocalDateTime quando) {
        if (quando == null) {
            return "";
        }
        long minuti = ChronoUnit.MINUTES.between(quando, LocalDateTime.now());
        if (minuti < 1) {
            return "adesso";
        }
        if (minuti < 60) {
            return minuti + " min fa";
        }
        long ore = minuti / 60;
        return ore < 24 ? ore + " h fa" : (ore / 24) + " g fa";
    }
}
