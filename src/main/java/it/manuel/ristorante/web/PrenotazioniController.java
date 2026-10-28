package it.manuel.ristorante.web;

import it.manuel.ristorante.config.LumeProperties;
import it.manuel.ristorante.config.StaffUserDetails;
import it.manuel.ristorante.dto.PrenotazioneForm;
import it.manuel.ristorante.model.Origine;
import it.manuel.ristorante.model.Prenotazione;
import it.manuel.ristorante.model.StatoPrenotazione;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import it.manuel.ristorante.service.PrenotazioneService;
import it.manuel.ristorante.service.SalaService;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/gestione/prenotazioni")
public class PrenotazioniController {

    private final PrenotazioneService prenotazioneService;
    private final SalaService salaService;
    private final LumeProperties properties;

    public PrenotazioniController(PrenotazioneService prenotazioneService, SalaService salaService,
                                  LumeProperties properties) {
        this.prenotazioneService = prenotazioneService;
        this.salaService = salaService;
        this.properties = properties;
    }

    @GetMapping
    public String elenco(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model model) {
        LocalDate giorno = data != null ? data : LocalDate.now();
        List<Prenotazione> lista = prenotazioneService.delGiorno(giorno);
        LocalTime limitePranzo = LocalTime.of(17, 0);
        model.addAttribute("giorno", giorno);
        model.addAttribute("oggi", LocalDate.now());
        model.addAttribute("calendario", prenotazioneService.copertiPerGiorno(LocalDate.now().minusDays(1), 15));
        model.addAttribute("pranzo", lista.stream().filter(p -> p.getOra().isBefore(limitePranzo)).toList());
        model.addAttribute("cena", lista.stream().filter(p -> !p.getOra().isBefore(limitePranzo)).toList());
        model.addAttribute("coperti", lista.stream().filter(Prenotazione::isAttiva).mapToInt(Prenotazione::getPersone).sum());
        model.addAttribute("totali", lista.stream().filter(Prenotazione::isAttiva).count());
        model.addAttribute("daConfermareGiorno", lista.stream().filter(p -> p.getStato() == StatoPrenotazione.RICHIESTA).count());
        model.addAttribute("chiuso", prenotazioneService.isChiuso(giorno));
        model.addAttribute("capienza", properties.capienzaPerTurno() * 2);
        model.addAttribute("attivo", "prenotazioni");
        return "gestione/prenotazioni";
    }

    @GetMapping("/nuova")
    public String nuova(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                        Model model) {
        PrenotazioneForm form = new PrenotazioneForm();
        form.setData(data != null ? data : LocalDate.now());
        form.setOra(LocalTime.of(20, 30));
        return form(model, form, null);
    }

    @PostMapping("/nuova")
    public String crea(@Valid @ModelAttribute("form") PrenotazioneForm form, BindingResult errori,
                       @RequestParam(defaultValue = "TELEFONO") Origine origine, Model model,
                       RedirectAttributes redirect) {
        return salva(null, form, errori, origine, model, redirect);
    }

    @GetMapping("/{id}")
    public String modifica(@PathVariable Long id, Model model) {
        Prenotazione p = prenotazioneService.prenotazione(id);
        return form(model, PrenotazioneForm.da(p), p);
    }

    @PostMapping("/{id}")
    public String aggiorna(@PathVariable Long id, @Valid @ModelAttribute("form") PrenotazioneForm form,
                           BindingResult errori, Model model, RedirectAttributes redirect) {
        return salva(id, form, errori, null, model, redirect);
    }

    private String salva(Long id, PrenotazioneForm form, BindingResult errori, Origine origine, Model model,
                         RedirectAttributes redirect) {
        Prenotazione esistente = id == null ? null : prenotazioneService.prenotazione(id);
        if (errori.hasErrors()) {
            return form(model, form, esistente);
        }
        try {
            Prenotazione p = prenotazioneService.salvaDaStaff(id, form, origine == null ? Origine.TELEFONO : origine);
            redirect.addFlashAttribute("ok", "Prenotazione di " + p.getNome() + " salvata"
                    + (p.getTavolo() == null ? "" : ", tavolo " + p.getTavolo().getNumero()));
            return "redirect:/gestione/prenotazioni?data=" + p.getData();
        } catch (OperazioneNonValidaException e) {
            errori.reject("prenotazione", e.getMessage());
            return form(model, form, esistente);
        }
    }

    private String form(Model model, PrenotazioneForm form, Prenotazione p) {
        model.addAttribute("form", form);
        model.addAttribute("p", p);
        model.addAttribute("tavoli", salaService.tavoliAttivi());
        model.addAttribute("orari", PrenotazioneService.orari());
        model.addAttribute("origini", Origine.values());
        model.addAttribute("attivo", "prenotazioni");
        return "gestione/prenotazione-form";
    }

    @PostMapping("/{id}/stato")
    public String stato(@PathVariable Long id, @RequestParam StatoPrenotazione stato, RedirectAttributes redirect) {
        Prenotazione p = prenotazioneService.cambiaStato(id, stato);
        redirect.addFlashAttribute("ok", p.getNome() + ": " + stato.getLabel().toLowerCase());
        return "redirect:/gestione/prenotazioni?data=" + p.getData();
    }

    @PostMapping("/{id}/accomoda")
    public String accomoda(@PathVariable Long id, @AuthenticationPrincipal StaffUserDetails utente,
                           RedirectAttributes redirect) {
        var ordine = prenotazioneService.faiAccomodare(id, utente.getId());
        redirect.addFlashAttribute("ok", "Tavolo " + ordine.getTavolo().getNumero() + " aperto per "
                + ordine.getPrenotazione().getNome());
        return "redirect:/gestione/ordini/" + ordine.getId();
    }
}
