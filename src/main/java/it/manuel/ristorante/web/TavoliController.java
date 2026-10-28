package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.TavoloForm;
import it.manuel.ristorante.model.FormaTavolo;
import it.manuel.ristorante.model.Tavolo;
import it.manuel.ristorante.model.ZonaSala;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import it.manuel.ristorante.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestione/tavoli")
public class TavoliController {

    private final SalaService salaService;

    public TavoliController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public String elenco(Model model) {
        model.addAttribute("tavoli", salaService.tavoli());
        model.addAttribute("postiTotali", salaService.tavoliAttivi().stream().mapToInt(Tavolo::getPosti).sum());
        model.addAttribute("attivo", "tavoli");
        return "gestione/tavoli";
    }

    @GetMapping("/nuovo")
    public String nuovo(Model model) {
        TavoloForm form = new TavoloForm();
        form.setNumero(salaService.tavoli().stream().mapToInt(Tavolo::getNumero).max().orElse(0) + 1);
        return form(model, form, null);
    }

    @PostMapping("/nuovo")
    public String crea(@Valid @ModelAttribute("form") TavoloForm form, BindingResult errori, Model model,
                       RedirectAttributes redirect) {
        return salva(null, form, errori, model, redirect);
    }

    @GetMapping("/{id}")
    public String modifica(@PathVariable Long id, Model model) {
        Tavolo t = salaService.tavolo(id);
        return form(model, TavoloForm.da(t), t);
    }

    @PostMapping("/{id}")
    public String aggiorna(@PathVariable Long id, @Valid @ModelAttribute("form") TavoloForm form, BindingResult errori,
                           Model model, RedirectAttributes redirect) {
        return salva(id, form, errori, model, redirect);
    }

    @PostMapping("/{id}/elimina")
    public String elimina(@PathVariable Long id, RedirectAttributes redirect) {
        int numero = salaService.tavolo(id).getNumero();
        salaService.elimina(id);
        redirect.addFlashAttribute("ok", "Tavolo " + numero + " eliminato");
        return "redirect:/gestione/tavoli";
    }

    private String salva(Long id, TavoloForm form, BindingResult errori, Model model, RedirectAttributes redirect) {
        Tavolo esistente = id == null ? null : salaService.tavolo(id);
        if (errori.hasErrors()) {
            return form(model, form, esistente);
        }
        try {
            Tavolo t = salaService.salva(id, form);
            redirect.addFlashAttribute("ok", "Tavolo " + t.getNumero() + " salvato");
            return "redirect:/gestione/tavoli";
        } catch (OperazioneNonValidaException e) {
            errori.reject("tavolo", e.getMessage());
            return form(model, form, esistente);
        }
    }

    private String form(Model model, TavoloForm form, Tavolo tavolo) {
        model.addAttribute("form", form);
        model.addAttribute("tavolo", tavolo);
        model.addAttribute("zone", ZonaSala.values());
        model.addAttribute("forme", FormaTavolo.values());
        model.addAttribute("attivo", "tavoli");
        return "gestione/tavolo-form";
    }
}
