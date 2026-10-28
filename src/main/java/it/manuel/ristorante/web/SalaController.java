package it.manuel.ristorante.web;

import it.manuel.ristorante.config.StaffUserDetails;
import it.manuel.ristorante.dto.TavoloVista;
import it.manuel.ristorante.model.Ordine;
import it.manuel.ristorante.service.OrdineService;
import it.manuel.ristorante.service.SalaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SalaController {

    private final SalaService salaService;
    private final OrdineService ordineService;

    public SalaController(SalaService salaService, OrdineService ordineService) {
        this.salaService = salaService;
        this.ordineService = ordineService;
    }

    @GetMapping("/gestione/sala")
    public String sala(Model model) {
        List<TavoloVista> vista = salaService.vista();
        model.addAttribute("sala", vista);
        model.addAttribute("riepilogo", salaService.riepilogo(vista));
        model.addAttribute("attivo", "sala");
        return "gestione/sala";
    }

    @PostMapping("/gestione/sala/tavoli/{id}/apri")
    public String apri(@PathVariable Long id, @RequestParam(defaultValue = "2") int coperti,
                       @AuthenticationPrincipal StaffUserDetails utente) {
        Ordine ordine = ordineService.apri(id, coperti, utente.getId(), null);
        return "redirect:/gestione/ordini/" + ordine.getId();
    }

    /** Stato dei tavoli per l'aggiornamento automatico della mappa. */
    @GetMapping("/api/gestione/sala")
    @ResponseBody
    public List<Map<String, Object>> statoSala() {
        return salaService.vista().stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.tavolo().getId());
            m.put("numero", v.tavolo().getNumero());
            m.put("stato", v.stato());
            m.put("etichetta", v.etichettaStato());
            m.put("ordineId", v.ordine() == null ? null : v.ordine().getId());
            m.put("coperti", v.ordine() == null ? null : v.ordine().getCoperti());
            m.put("minuti", v.ordine() == null ? null : v.ordine().getMinutiAlTavolo());
            m.put("totale", v.ordine() == null ? null : v.ordine().getTotaleCorrente());
            m.put("prenotazione", v.prossima() == null ? null
                    : v.prossima().getOra().toString() + " " + v.prossima().getNome());
            return m;
        }).toList();
    }

    /** Trascinamento dei tavoli sulla mappa (solo amministratore). */
    @PostMapping("/api/gestione/tavoli/{id}/posizione")
    @ResponseBody
    public Map<String, Object> sposta(@PathVariable Long id, @RequestParam int x, @RequestParam int y) {
        salaService.sposta(id, x, y);
        return Map.of("ok", true);
    }
}
