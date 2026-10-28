package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.TicketCucina;
import it.manuel.ristorante.service.CucinaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Monitor della cucina: pagina a tutto schermo aggiornata ogni pochi secondi. */
@Controller
public class CucinaController {

    private final CucinaService cucinaService;

    public CucinaController(CucinaService cucinaService) {
        this.cucinaService = cucinaService;
    }

    @GetMapping("/gestione/cucina")
    public String cucina(Model model) {
        model.addAttribute("tickets", cucinaService.tickets());
        model.addAttribute("attivo", "cucina");
        return "gestione/cucina";
    }

    @GetMapping("/api/gestione/cucina")
    @ResponseBody
    public Map<String, Object> tickets() {
        List<TicketCucina> tickets = cucinaService.tickets();
        return Map.of("ora", LocalTime.now().withNano(0).toString(), "tickets", tickets);
    }

    @PostMapping("/api/gestione/cucina/{ordineId}/righe/{rigaId}/avanti")
    @ResponseBody
    public Map<String, Object> avanti(@PathVariable Long ordineId, @PathVariable Long rigaId) {
        cucinaService.avanza(ordineId, rigaId);
        return tickets();
    }

    @PostMapping("/api/gestione/cucina/{ordineId}/righe/{rigaId}/indietro")
    @ResponseBody
    public Map<String, Object> indietro(@PathVariable Long ordineId, @PathVariable Long rigaId) {
        cucinaService.indietro(ordineId, rigaId);
        return tickets();
    }

    @PostMapping("/api/gestione/cucina/{ordineId}/pronto")
    @ResponseBody
    public Map<String, Object> tuttoPronto(@PathVariable Long ordineId) {
        cucinaService.tuttoPronto(ordineId);
        return tickets();
    }
}
