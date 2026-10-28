package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.AiPiatto;
import it.manuel.ristorante.dto.PiattoForm;
import it.manuel.ristorante.model.Allergene;
import it.manuel.ristorante.model.Piatto;
import it.manuel.ristorante.service.AiService;
import it.manuel.ristorante.service.MagazzinoService;
import it.manuel.ristorante.service.MenuService;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Gestione del menù (solo amministratore). */
@Controller
public class MenuAdminController {

    private final MenuService menuService;
    private final MagazzinoService magazzinoService;
    private final AiService aiService;

    public MenuAdminController(MenuService menuService, MagazzinoService magazzinoService, AiService aiService) {
        this.menuService = menuService;
        this.magazzinoService = magazzinoService;
        this.aiService = aiService;
    }

    @GetMapping("/gestione/menu")
    public String menu(Model model) {
        model.addAttribute("menu", menuService.menuPerCategoria(false));
        model.addAttribute("categorie", menuService.categorie());
        model.addAttribute("piattiTotali", menuService.piatti().size());
        model.addAttribute("esauriti", menuService.contaEsauriti());
        model.addAttribute("attivo", "menu");
        return "gestione/menu";
    }

    @GetMapping("/gestione/menu/piatti/nuovo")
    public String nuovo(@RequestParam(required = false) Long categoria, Model model) {
        PiattoForm form = new PiattoForm();
        form.setCategoriaId(categoria);
        return form(model, form, null);
    }

    @PostMapping("/gestione/menu/piatti/nuovo")
    public String crea(@Valid @ModelAttribute("form") PiattoForm form, BindingResult errori, Model model,
                       RedirectAttributes redirect) {
        return salva(null, form, errori, model, redirect);
    }

    @GetMapping("/gestione/menu/piatti/{id}")
    public String modifica(@PathVariable Long id, Model model) {
        Piatto piatto = menuService.piatto(id);
        return form(model, PiattoForm.da(piatto), piatto);
    }

    @PostMapping("/gestione/menu/piatti/{id}")
    public String aggiorna(@PathVariable Long id, @Valid @ModelAttribute("form") PiattoForm form, BindingResult errori,
                           Model model, RedirectAttributes redirect) {
        return salva(id, form, errori, model, redirect);
    }

    private String salva(Long id, PiattoForm form, BindingResult errori, Model model, RedirectAttributes redirect) {
        Piatto esistente = id == null ? null : menuService.piatto(id);
        if (errori.hasErrors()) {
            return form(model, form, esistente);
        }
        try {
            Piatto salvato = menuService.salvaPiatto(id, form);
            redirect.addFlashAttribute("ok", "\"" + salvato.getNome() + "\" " + (id == null ? "aggiunto al menù" : "aggiornato"));
            return "redirect:/gestione/menu#categoria-" + salvato.getCategoria().getId();
        } catch (OperazioneNonValidaException e) {
            errori.reject("piatto", e.getMessage());
            return form(model, form, esistente);
        }
    }

    private String form(Model model, PiattoForm form, Piatto piatto) {
        model.addAttribute("form", form);
        model.addAttribute("piatto", piatto);
        model.addAttribute("categorie", menuService.categorie());
        model.addAttribute("ingredienti", magazzinoService.ingredienti());
        model.addAttribute("allergeni", Allergene.values());
        model.addAttribute("attivo", "menu");
        return "gestione/piatto-form";
    }

    @PostMapping("/gestione/menu/piatti/{id}/disponibilita")
    public Object disponibilita(@PathVariable Long id, @RequestHeader(value = "Accept", defaultValue = "") String accept,
                                RedirectAttributes redirect) {
        Piatto p = menuService.cambiaDisponibilita(id);
        if (accept.contains("application/json")) {
            return ResponseEntity.ok(Map.of("disponibile", p.isDisponibile(), "nome", p.getNome()));
        }
        redirect.addFlashAttribute("ok", p.getNome() + (p.isDisponibile() ? " di nuovo disponibile" : " segnato come esaurito"));
        return "redirect:/gestione/menu";
    }

    @PostMapping("/gestione/menu/piatti/{id}/elimina")
    public String elimina(@PathVariable Long id, RedirectAttributes redirect) {
        String nome = menuService.piatto(id).getNome();
        menuService.eliminaPiatto(id);
        redirect.addFlashAttribute("ok", "\"" + nome + "\" eliminato dal menù");
        return "redirect:/gestione/menu";
    }

    // ------------------------------------------------------------------ categorie

    @PostMapping("/gestione/menu/categorie")
    public String nuovaCategoria(@RequestParam String nome, @RequestParam(required = false) String sottotitolo,
                                 @RequestParam(defaultValue = "1") int uscita, RedirectAttributes redirect) {
        var c = menuService.salvaCategoria(null, nome, sottotitolo, uscita);
        redirect.addFlashAttribute("ok", "Categoria \"" + c.getNome() + "\" creata");
        return "redirect:/gestione/menu";
    }

    @PostMapping("/gestione/menu/categorie/{id}")
    public String aggiornaCategoria(@PathVariable Long id, @RequestParam String nome,
                                    @RequestParam(required = false) String sottotitolo,
                                    @RequestParam(defaultValue = "1") int uscita, RedirectAttributes redirect) {
        var c = menuService.salvaCategoria(id, nome, sottotitolo, uscita);
        redirect.addFlashAttribute("ok", "Categoria \"" + c.getNome() + "\" aggiornata");
        return "redirect:/gestione/menu";
    }

    @PostMapping("/gestione/menu/categorie/{id}/sposta")
    public String sposta(@PathVariable Long id, @RequestParam int direzione) {
        menuService.spostaCategoria(id, direzione);
        return "redirect:/gestione/menu";
    }

    @PostMapping("/gestione/menu/categorie/{id}/elimina")
    public String eliminaCategoria(@PathVariable Long id, RedirectAttributes redirect) {
        String nome = menuService.categoria(id).getNome();
        menuService.eliminaCategoria(id);
        redirect.addFlashAttribute("ok", "Categoria \"" + nome + "\" eliminata");
        return "redirect:/gestione/menu";
    }

    // ------------------------------------------------------------------ AI

    /** Genera descrizione e abbinamento dai dati del modulo (anche per un piatto non ancora salvato). */
    @PostMapping("/api/gestione/ai/piatto")
    @ResponseBody
    public AiPiatto testiAi(@RequestParam String nome, @RequestParam(required = false) Long categoriaId,
                            @RequestParam(required = false) String ingredienti,
                            @RequestParam(required = false) String descrizione) {
        if (nome == null || nome.isBlank()) {
            throw new OperazioneNonValidaException("Scrivi prima il nome del piatto");
        }
        String categoria = categoriaId == null ? "" : menuService.categoria(categoriaId).getNome();
        return aiService.testiPiatto(nome.trim(), categoria, ingredienti, descrizione);
    }
}
