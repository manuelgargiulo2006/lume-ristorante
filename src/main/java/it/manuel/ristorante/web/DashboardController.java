package it.manuel.ristorante.web;

import it.manuel.ristorante.config.StaffUserDetails;
import it.manuel.ristorante.dto.TavoloVista;
import it.manuel.ristorante.model.Prenotazione;
import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.StatoPrenotazione;
import it.manuel.ristorante.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class DashboardController {

    private final StatisticheService statisticheService;
    private final SalaService salaService;
    private final PrenotazioneService prenotazioneService;
    private final CucinaService cucinaService;
    private final MagazzinoService magazzinoService;
    private final MenuService menuService;

    public DashboardController(StatisticheService statisticheService, SalaService salaService,
                               PrenotazioneService prenotazioneService, CucinaService cucinaService,
                               MagazzinoService magazzinoService, MenuService menuService) {
        this.statisticheService = statisticheService;
        this.salaService = salaService;
        this.prenotazioneService = prenotazioneService;
        this.cucinaService = cucinaService;
        this.magazzinoService = magazzinoService;
        this.menuService = menuService;
    }

    @GetMapping("/gestione")
    public String dashboard(@AuthenticationPrincipal StaffUserDetails utente, Model model) {
        LocalDate oggi = LocalDate.now();
        List<TavoloVista> sala = salaService.vista();
        List<Prenotazione> prenotazioni = prenotazioneService.delGiorno(oggi);
        LocalTime adesso = LocalTime.now().minusMinutes(30);

        model.addAttribute("oggi", statisticheService.riepilogoOggi());
        model.addAttribute("settimana", statisticheService.riepilogo(oggi.minusDays(6), oggi));
        model.addAttribute("incassi", statisticheService.incassoGiornaliero(oggi.minusDays(13), oggi));
        model.addAttribute("sala", sala);
        model.addAttribute("riepilogoSala", salaService.riepilogo(sala));
        model.addAttribute("tavoliOccupati", sala.stream().filter(v -> v.ordine() != null).count());
        model.addAttribute("copertiInSala", sala.stream().filter(v -> v.ordine() != null)
                .mapToInt(v -> v.ordine().getCoperti()).sum());
        model.addAttribute("arrivi", prenotazioni.stream()
                .filter(p -> (p.getStato() == StatoPrenotazione.CONFERMATA || p.getStato() == StatoPrenotazione.RICHIESTA)
                        && !p.getOra().isBefore(adesso))
                .limit(6).toList());
        model.addAttribute("copertiPrenotati", prenotazioni.stream().filter(Prenotazione::isAttiva)
                .mapToInt(Prenotazione::getPersone).sum());
        model.addAttribute("tickets", cucinaService.tickets());
        model.addAttribute("sottoScorta", magazzinoService.sottoScorta());
        model.addAttribute("esauriti", menuService.contaEsauriti());
        model.addAttribute("top", statisticheService.piattiPiuVenduti(oggi.minusDays(6), oggi, 5));
        model.addAttribute("isCucina", utente.getRuolo() == Ruolo.CUCINA);
        model.addAttribute("attivo", "dashboard");
        return "gestione/dashboard";
    }
}
