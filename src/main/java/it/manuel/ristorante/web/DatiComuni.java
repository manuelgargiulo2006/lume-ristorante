package it.manuel.ristorante.web;

import it.manuel.ristorante.config.StaffUserDetails;
import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.StatoRiga;
import it.manuel.ristorante.service.CucinaService;
import it.manuel.ristorante.service.MagazzinoService;
import it.manuel.ristorante.service.PrenotazioneService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Dati che servono in tutte le pagine del gestionale: utente e notifiche della barra in alto. */
@ControllerAdvice(basePackages = "it.manuel.ristorante.web")
public class DatiComuni {

    private final PrenotazioneService prenotazioneService;
    private final MagazzinoService magazzinoService;
    private final CucinaService cucinaService;

    public DatiComuni(PrenotazioneService prenotazioneService, MagazzinoService magazzinoService,
                      CucinaService cucinaService) {
        this.prenotazioneService = prenotazioneService;
        this.magazzinoService = magazzinoService;
        this.cucinaService = cucinaService;
    }

    @ModelAttribute
    public void datiComuni(@AuthenticationPrincipal StaffUserDetails utente, HttpServletRequest request, Model model) {
        if (utente == null || !request.getRequestURI().startsWith(request.getContextPath() + "/gestione")) {
            return;
        }
        model.addAttribute("utente", utente);
        long daConfermare = utente.getRuolo() == Ruolo.CUCINA ? 0 : prenotazioneService.daConfermare();
        long sottoScorta = utente.getRuolo() == Ruolo.SALA ? 0 : magazzinoService.sottoScorta().size();
        long pronti = utente.getRuolo() == Ruolo.CUCINA ? 0 : cucinaService.tickets().stream()
                .flatMap(t -> t.righe().stream())
                .filter(r -> r.stato().equals(StatoRiga.PRONTO.name()))
                .count();
        model.addAttribute("notificaPrenotazioni", daConfermare);
        model.addAttribute("notificaScorte", sottoScorta);
        model.addAttribute("notificaPronti", pronti);
        model.addAttribute("notificheTotali", daConfermare + sottoScorta + pronti);
    }
}
