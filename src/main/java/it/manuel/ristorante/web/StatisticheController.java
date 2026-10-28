package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.Riepilogo;
import it.manuel.ristorante.service.AiService;
import it.manuel.ristorante.service.StatisticheService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class StatisticheController {

    private static final List<Integer> PERIODI = List.of(7, 14, 30);

    private final StatisticheService statisticheService;
    private final AiService aiService;

    public StatisticheController(StatisticheService statisticheService, AiService aiService) {
        this.statisticheService = statisticheService;
        this.aiService = aiService;
    }

    @GetMapping("/gestione/statistiche")
    public String statistiche(@RequestParam(defaultValue = "14") int giorni, Model model) {
        int g = PERIODI.contains(giorni) ? giorni : 14;
        LocalDate a = LocalDate.now();
        LocalDate da = a.minusDays(g - 1L);
        model.addAttribute("giorni", g);
        model.addAttribute("periodi", PERIODI);
        model.addAttribute("riepilogo", statisticheService.riepilogo(da, a));
        model.addAttribute("incassi", statisticheService.incassoGiornaliero(da, a));
        model.addAttribute("affluenza", statisticheService.affluenzaPerOra(da, a));
        model.addAttribute("top", statisticheService.piattiPiuVenduti(da, a, 8));
        model.addAttribute("categorie", statisticheService.incassoPerCategoria(da, a));
        model.addAttribute("pagamenti", statisticheService.metodiPagamento(da, a));
        model.addAttribute("sprechi", statisticheService.valoreSprechi(da, a));
        model.addAttribute("attivo", "statistiche");
        return "gestione/statistiche";
    }

    @PostMapping("/api/gestione/ai/analisi")
    @ResponseBody
    public Map<String, Object> analisi(@RequestParam(defaultValue = "14") int giorni) {
        int g = PERIODI.contains(giorni) ? giorni : 14;
        LocalDate a = LocalDate.now();
        LocalDate da = a.minusDays(g - 1L);
        Riepilogo r = statisticheService.riepilogo(da, a);
        String extra = "Sprechi registrati in magazzino: " + statisticheService.valoreSprechi(da, a) + " euro.";
        String testo = aiService.analisiVendite(r, statisticheService.piattiPiuVenduti(da, a, 5), g, extra);
        List<String> punti = Arrays.stream(testo.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceFirst("^[-*•]\\s*", "").replace("**", ""))
                .toList();
        return Map.of("punti", punti);
    }
}
