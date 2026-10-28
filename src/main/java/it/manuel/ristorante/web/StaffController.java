package it.manuel.ristorante.web;

import it.manuel.ristorante.config.StaffUserDetails;
import it.manuel.ristorante.dto.StaffForm;
import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.Staff;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import it.manuel.ristorante.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestione/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public String elenco(Model model) {
        model.addAttribute("staff", staffService.tutti());
        model.addAttribute("attivo", "staff");
        return "gestione/staff";
    }

    @GetMapping("/nuovo")
    public String nuovo(Model model) {
        return form(model, new StaffForm(), null);
    }

    @PostMapping("/nuovo")
    public String crea(@Valid @ModelAttribute("form") StaffForm form, BindingResult errori,
                       @AuthenticationPrincipal StaffUserDetails utente, Model model, RedirectAttributes redirect) {
        return salva(null, form, errori, utente, model, redirect);
    }

    @GetMapping("/{id}")
    public String modifica(@PathVariable Long id, Model model) {
        Staff s = staffService.staff(id);
        return form(model, StaffForm.da(s), s);
    }

    @PostMapping("/{id}")
    public String aggiorna(@PathVariable Long id, @Valid @ModelAttribute("form") StaffForm form, BindingResult errori,
                           @AuthenticationPrincipal StaffUserDetails utente, Model model, RedirectAttributes redirect) {
        return salva(id, form, errori, utente, model, redirect);
    }

    private String salva(Long id, StaffForm form, BindingResult errori, StaffUserDetails utente, Model model,
                         RedirectAttributes redirect) {
        Staff esistente = id == null ? null : staffService.staff(id);
        if (errori.hasErrors()) {
            return form(model, form, esistente);
        }
        try {
            Staff s = staffService.salva(id, form, utente.getId());
            redirect.addFlashAttribute("ok", s.getNomeCompleto() + (id == null ? " aggiunto allo staff" : " aggiornato"));
            return "redirect:/gestione/staff";
        } catch (OperazioneNonValidaException e) {
            errori.reject("staff", e.getMessage());
            return form(model, form, esistente);
        }
    }

    private String form(Model model, StaffForm form, Staff staff) {
        model.addAttribute("form", form);
        model.addAttribute("membro", staff);
        model.addAttribute("ruoli", Ruolo.values());
        model.addAttribute("attivo", "staff");
        return "gestione/staff-form";
    }
}
