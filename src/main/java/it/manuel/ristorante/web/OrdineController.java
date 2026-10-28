package it.manuel.ristorante.web;

import it.manuel.ristorante.model.MetodoPagamento;
import it.manuel.ristorante.model.Ordine;
import it.manuel.ristorante.model.RigaOrdine;
import it.manuel.ristorante.model.StatoRiga;
import it.manuel.ristorante.service.MenuService;
import it.manuel.ristorante.service.OrdineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Comanda di un tavolo: tutte le azioni tornano alla stessa pagina. */
@Controller
@RequestMapping("/gestione/ordini/{id}")
public class OrdineController {

    private final OrdineService ordineService;
    private final MenuService menuService;

    public OrdineController(OrdineService ordineService, MenuService menuService) {
        this.ordineService = ordineService;
        this.menuService = menuService;
    }

    @GetMapping
    public String comanda(@PathVariable Long id, Model model) {
        Ordine ordine = ordineService.ordine(id);
        Map<Integer, List<RigaOrdine>> perUscita = ordine.getRighe().stream()
                .sorted(Comparator.comparing(RigaOrdine::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(RigaOrdine::getUscita, TreeMap::new,
                        Collectors.toList()));
        model.addAttribute("ordine", ordine);
        model.addAttribute("perUscita", perUscita);
        model.addAttribute("daInviare", ordine.getRighe().stream()
                .filter(r -> r.getStato() == StatoRiga.IN_ATTESA).mapToInt(RigaOrdine::getQuantita).sum());
        model.addAttribute("pronti", ordine.getRighe().stream()
                .filter(r -> r.getStato() == StatoRiga.PRONTO).mapToInt(RigaOrdine::getQuantita).sum());
        model.addAttribute("menu", menuService.menuPerCategoria(false));
        model.addAttribute("metodi", MetodoPagamento.values());
        model.addAttribute("attivo", "sala");
        return "gestione/ordine";
    }

    /** Preconto stampabile da portare al tavolo. */
    @GetMapping("/preconto")
    public String preconto(@PathVariable Long id, Model model) {
        model.addAttribute("ordine", ordineService.ordine(id));
        return "gestione/preconto";
    }

    @PostMapping("/aggiungi")
    public String aggiungi(@PathVariable Long id, @RequestParam Long piattoId,
                           @RequestParam(defaultValue = "1") int quantita,
                           @RequestParam(required = false) String note, RedirectAttributes redirect) {
        var riga = ordineService.aggiungi(id, piattoId, quantita, note);
        redirect.addFlashAttribute("ok", riga.getPiatto().getNome() + " aggiunto");
        return torna(id);
    }

    @PostMapping("/righe/{rigaId}/piu")
    public String piu(@PathVariable Long id, @PathVariable Long rigaId) {
        ordineService.cambiaQuantita(id, rigaId, 1);
        return torna(id);
    }

    @PostMapping("/righe/{rigaId}/meno")
    public String meno(@PathVariable Long id, @PathVariable Long rigaId) {
        ordineService.cambiaQuantita(id, rigaId, -1);
        return torna(id);
    }

    @PostMapping("/righe/{rigaId}/rimuovi")
    public String rimuovi(@PathVariable Long id, @PathVariable Long rigaId) {
        ordineService.rimuovi(id, rigaId);
        return torna(id);
    }

    @PostMapping("/righe/{rigaId}/nota")
    public String nota(@PathVariable Long id, @PathVariable Long rigaId, @RequestParam(required = false) String nota) {
        ordineService.nota(id, rigaId, nota);
        return torna(id);
    }

    @PostMapping("/righe/{rigaId}/uscita")
    public String uscita(@PathVariable Long id, @PathVariable Long rigaId, @RequestParam int uscita) {
        ordineService.cambiaUscita(id, rigaId, uscita);
        return torna(id);
    }

    @PostMapping("/invia")
    public String invia(@PathVariable Long id, RedirectAttributes redirect) {
        int n = ordineService.inviaInCucina(id);
        redirect.addFlashAttribute("ok", n == 1 ? "1 piatto inviato in cucina" : n + " piatti inviati in cucina");
        return torna(id);
    }

    @PostMapping("/servi")
    public String servi(@PathVariable Long id, RedirectAttributes redirect) {
        int n = ordineService.serviPronti(id);
        redirect.addFlashAttribute("ok", n == 0 ? "Nessun piatto pronto da servire"
                : n == 1 ? "1 piatto servito" : n + " piatti serviti");
        return torna(id);
    }

    @PostMapping("/coperti")
    public String coperti(@PathVariable Long id, @RequestParam int coperti) {
        ordineService.aggiornaCoperti(id, coperti);
        return torna(id);
    }

    @PostMapping("/conto")
    public String conto(@PathVariable Long id, RedirectAttributes redirect) {
        ordineService.richiediConto(id);
        redirect.addFlashAttribute("ok", "Conto richiesto: il tavolo è segnalato in sala");
        return torna(id);
    }

    @PostMapping("/riapri")
    public String riapri(@PathVariable Long id) {
        ordineService.riapri(id);
        return torna(id);
    }

    @PostMapping("/chiudi")
    public String chiudi(@PathVariable Long id, @RequestParam(required = false) MetodoPagamento metodo,
                         @RequestParam(defaultValue = "0") int sconto, RedirectAttributes redirect) {
        Ordine ordine = ordineService.chiudi(id, metodo, sconto);
        redirect.addFlashAttribute("ok", "Tavolo " + ordine.getTavolo().getNumero() + " chiuso: incassati "
                + String.format(java.util.Locale.ITALY, "€ %,.2f", ordine.getTotale()));
        return "redirect:/gestione/sala";
    }

    @PostMapping("/annulla")
    public String annulla(@PathVariable Long id, RedirectAttributes redirect) {
        ordineService.annulla(id);
        redirect.addFlashAttribute("ok", "Comanda annullata, il tavolo è di nuovo libero");
        return "redirect:/gestione/sala";
    }

    private String torna(Long id) {
        return "redirect:/gestione/ordini/" + id;
    }
}
