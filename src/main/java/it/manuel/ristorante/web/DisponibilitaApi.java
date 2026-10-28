package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.FasciaOraria;
import it.manuel.ristorante.service.PrenotazioneService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Disponibilità in tempo reale per il modulo di prenotazione del sito. */
@RestController
public class DisponibilitaApi {

    private final PrenotazioneService prenotazioneService;

    public DisponibilitaApi(PrenotazioneService prenotazioneService) {
        this.prenotazioneService = prenotazioneService;
    }

    @GetMapping("/api/public/disponibilita")
    public Map<String, Object> disponibilita(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                             @RequestParam(defaultValue = "2") int persone) {
        List<FasciaOraria> fasce = prenotazioneService.fasce(data, Math.max(1, Math.min(12, persone)), null);
        return Map.of("data", data.toString(),
                "chiuso", prenotazioneService.isChiuso(data),
                "fasce", fasce.stream().map(f -> Map.of(
                        "ora", f.getEtichetta(), "servizio", f.servizio(),
                        "postiLiberi", f.postiLiberi(), "disponibile", f.disponibile())).toList());
    }
}
