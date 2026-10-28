package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.IngredienteForm;
import it.manuel.ristorante.model.Ingrediente;
import it.manuel.ristorante.model.TipoMovimento;
import it.manuel.ristorante.model.UnitaMisura;
import it.manuel.ristorante.service.MagazzinoService;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import it.manuel.ristorante.service.StatisticheService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/gestione/magazzino")
public class MagazzinoController {

    private final MagazzinoService magazzinoService;
    private final StatisticheService statisticheService;

    public MagazzinoController(MagazzinoService magazzinoService, StatisticheService statisticheService) {
        this.magazzinoService = magazzinoService;
        this.statisticheService = statisticheService;
    }

    @GetMapping
    public String magazzino(Model model) {
        List<Ingrediente> ingredienti = magazzinoService.ingredienti();
        model.addAttribute("ingredienti", ingredienti);
        model.addAttribute("sottoScorta", ingredienti.stream().filter(Ingrediente::isSottoScorta).count());
        model.addAttribute("valore", magazzinoService.valoreMagazzino());
        model.addAttribute("sprechi", statisticheService.valoreSprechi(LocalDate.now().minusDays(29), LocalDate.now()));
        model.addAttribute("movimenti", magazzinoService.ultimiMovimenti());
        model.addAttribute("tipi", List.of(TipoMovimento.CARICO, TipoMovimento.SPRECO, TipoMovimento.RETTIFICA));
        model.addAttribute("attivo", "magazzino");
        return "gestione/magazzino";
    }

    /** Lista della spesa per i fornitori: cosa riordinare per tornare a tre volte la soglia minima. */
    @GetMapping("/ordini-fornitori")
    public String ordiniFornitori(Model model) {
        Map<String, List<Map<String, Object>>> perFornitore = new TreeMap<>();
        BigDecimal totale = BigDecimal.ZERO;
        for (Ingrediente i : magazzinoService.sottoScorta()) {
            BigDecimal daOrdinare = i.getSogliaMinima().multiply(BigDecimal.valueOf(3)).subtract(i.getScorta().max(BigDecimal.ZERO))
                    .setScale(i.getUnita() == UnitaMisura.PZ ? 0 : 1, RoundingMode.UP);
            BigDecimal costo = daOrdinare.multiply(i.getCostoUnitario()).setScale(2, RoundingMode.HALF_UP);
            totale = totale.add(costo);
            perFornitore.computeIfAbsent(i.getFornitore() == null ? "Senza fornitore" : i.getFornitore(), k -> new ArrayList<>())
                    .add(Map.of("ingrediente", i, "quantita", daOrdinare, "costo", costo));
        }
        model.addAttribute("perFornitore", perFornitore);
        model.addAttribute("totale", totale);
        model.addAttribute("attivo", "magazzino");
        return "gestione/ordini-fornitori";
    }

    @PostMapping("/{id}/movimento")
    public String movimento(@PathVariable Long id, @RequestParam TipoMovimento tipo,
                            @RequestParam(required = false) BigDecimal quantita,
                            @RequestParam(required = false) String nota, RedirectAttributes redirect) {
        var m = magazzinoService.registra(id, tipo, quantita, nota);
        redirect.addFlashAttribute("ok", tipo.getLabel() + " registrato: " + m.getIngrediente().getNome() + " ora "
                + m.getIngrediente().getScorta().stripTrailingZeros().toPlainString() + " " + m.getIngrediente().getUnita().getLabel());
        return "redirect:/gestione/magazzino";
    }

    @GetMapping("/ingredienti/nuovo")
    public String nuovo(Model model) {
        return form(model, new IngredienteForm(), null);
    }

    @PostMapping("/ingredienti/nuovo")
    public String crea(@Valid @ModelAttribute("form") IngredienteForm form, BindingResult errori, Model model,
                       RedirectAttributes redirect) {
        return salva(null, form, errori, model, redirect);
    }

    @GetMapping("/ingredienti/{id}")
    public String modifica(@PathVariable Long id, Model model) {
        Ingrediente i = magazzinoService.ingrediente(id);
        return form(model, IngredienteForm.da(i), i);
    }

    @PostMapping("/ingredienti/{id}")
    public String aggiorna(@PathVariable Long id, @Valid @ModelAttribute("form") IngredienteForm form,
                           BindingResult errori, Model model, RedirectAttributes redirect) {
        return salva(id, form, errori, model, redirect);
    }

    @PostMapping("/ingredienti/{id}/elimina")
    public String elimina(@PathVariable Long id, RedirectAttributes redirect) {
        String nome = magazzinoService.ingrediente(id).getNome();
        magazzinoService.elimina(id);
        redirect.addFlashAttribute("ok", nome + " eliminato dal magazzino");
        return "redirect:/gestione/magazzino";
    }

    private String salva(Long id, IngredienteForm form, BindingResult errori, Model model, RedirectAttributes redirect) {
        Ingrediente esistente = id == null ? null : magazzinoService.ingrediente(id);
        if (errori.hasErrors()) {
            return form(model, form, esistente);
        }
        try {
            Ingrediente i = magazzinoService.salva(id, form);
            redirect.addFlashAttribute("ok", i.getNome() + (id == null ? " aggiunto al magazzino" : " aggiornato"));
            return "redirect:/gestione/magazzino";
        } catch (OperazioneNonValidaException e) {
            errori.reject("ingrediente", e.getMessage());
            return form(model, form, esistente);
        }
    }

    private String form(Model model, IngredienteForm form, Ingrediente ingrediente) {
        model.addAttribute("form", form);
        model.addAttribute("ingrediente", ingrediente);
        model.addAttribute("unita", UnitaMisura.values());
        model.addAttribute("attivo", "magazzino");
        return "gestione/ingrediente-form";
    }
}
