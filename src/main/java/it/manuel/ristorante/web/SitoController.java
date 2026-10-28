package it.manuel.ristorante.web;

import it.manuel.ristorante.dto.FasciaOraria;
import it.manuel.ristorante.dto.PrenotazioneForm;
import it.manuel.ristorante.model.Allergene;
import it.manuel.ristorante.model.Prenotazione;
import it.manuel.ristorante.service.MenuService;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import it.manuel.ristorante.service.PrenotazioneService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Sito pubblico del ristorante: home, menù e prenotazione online. */
@Controller
public class SitoController {

    private final MenuService menuService;
    private final PrenotazioneService prenotazioneService;

    public SitoController(MenuService menuService, PrenotazioneService prenotazioneService) {
        this.menuService = menuService;
        this.prenotazioneService = prenotazioneService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("firma", menuService.piattiFirma());
        model.addAttribute("menu", menuService.menuPerCategoria(true));
        return "public/home";
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        model.addAttribute("menu", menuService.menuPerCategoria(true));
        model.addAttribute("allergeni", Allergene.values());
        return "public/menu";
    }

    // ------------------------------------------------------------------ prenotazione

    @GetMapping("/prenota")
    public String prenota(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                          @RequestParam(defaultValue = "2") int persone,
                          Model model) {
        PrenotazioneForm form = new PrenotazioneForm();
        form.setPersone(Math.max(1, Math.min(12, persone)));
        form.setData(data != null ? data : primoGiornoUtile());
        return mostraPrenota(form, model);
    }

    @PostMapping("/prenota")
    public String invia(@Valid @ModelAttribute("form") PrenotazioneForm form, BindingResult errori,
                        Model model, RedirectAttributes redirect) {
        if (errori.hasErrors()) {
            return mostraPrenota(form, model);
        }
        try {
            Prenotazione p = prenotazioneService.prenotaOnline(form);
            redirect.addFlashAttribute("nuova", true);
            return "redirect:/prenotazione/" + p.getCodice();
        } catch (OperazioneNonValidaException e) {
            errori.reject("prenotazione", e.getMessage());
            return mostraPrenota(form, model);
        }
    }

    private String mostraPrenota(PrenotazioneForm form, Model model) {
        LocalDate oggi = LocalDate.now();
        List<LocalDate> giorni = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            giorni.add(oggi.plusDays(i));
        }
        int persone = form.getPersone() == null ? 2 : form.getPersone();
        List<FasciaOraria> fasce = form.getData() == null ? List.of()
                : prenotazioneService.fasce(form.getData(), persone, null);
        model.addAttribute("form", form);
        model.addAttribute("giorni", giorni);
        model.addAttribute("chiuso", form.getData() != null && prenotazioneService.isChiuso(form.getData()));
        model.addAttribute("pranzo", fasce.stream().filter(f -> f.servizio().equals("Pranzo")).toList());
        model.addAttribute("cena", fasce.stream().filter(f -> f.servizio().equals("Cena")).toList());
        model.addAttribute("nessunaFascia", fasce.stream().noneMatch(FasciaOraria::disponibile));
        return "public/prenota";
    }

    private LocalDate primoGiornoUtile() {
        LocalDate d = LocalDate.now();
        while (prenotazioneService.isChiuso(d)
                || prenotazioneService.fasce(d, 2, null).stream().noneMatch(FasciaOraria::disponibile)) {
            d = d.plusDays(1);
            if (d.isAfter(LocalDate.now().plusDays(14))) {
                return LocalDate.now().plusDays(1);
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ gestione da parte del cliente

    @GetMapping("/prenotazione")
    public String cerca(@RequestParam(required = false) String codice, Model model) {
        if (codice != null && !codice.isBlank()) {
            var trovata = prenotazioneService.perCodice(codice);
            if (trovata.isPresent()) {
                return "redirect:/prenotazione/" + trovata.get().getCodice();
            }
            model.addAttribute("nonTrovata", true);
            model.addAttribute("codice", codice);
        }
        return "public/cerca-prenotazione";
    }

    @GetMapping("/prenotazione/{codice}")
    public String dettaglio(@PathVariable String codice, Model model) {
        var p = prenotazioneService.perCodice(codice);
        if (p.isEmpty()) {
            model.addAttribute("nonTrovata", true);
            model.addAttribute("codice", codice);
            return "public/cerca-prenotazione";
        }
        model.addAttribute("p", p.get());
        return "public/prenotazione";
    }

    @PostMapping("/prenotazione/{codice}/annulla")
    public String annulla(@PathVariable String codice, @RequestParam String telefono, RedirectAttributes redirect) {
        try {
            prenotazioneService.annullaDaCliente(codice, telefono);
            redirect.addFlashAttribute("ok", "La prenotazione è stata annullata. Speriamo di vederti presto.");
        } catch (OperazioneNonValidaException e) {
            redirect.addFlashAttribute("errore", e.getMessage());
        }
        return "redirect:/prenotazione/" + codice;
    }

    /** Evento da aggiungere al calendario del telefono (formato iCalendar). */
    @GetMapping("/prenotazione/{codice}/calendario.ics")
    public ResponseEntity<byte[]> calendario(@PathVariable String codice) {
        Prenotazione p = prenotazioneService.perCodice(codice)
                .orElseThrow(() -> new it.manuel.ristorante.service.NonTrovatoException("Prenotazione non trovata"));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String ics = String.join("\r\n",
                "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Lume Ristorante//IT", "BEGIN:VEVENT",
                "UID:" + p.getCodice() + "@lume-ristorante",
                "DTSTART;TZID=Europe/Rome:" + p.getInizio().format(f),
                "DTEND;TZID=Europe/Rome:" + p.getInizio().plusHours(2).format(f),
                "SUMMARY:Cena da Lume (" + p.getPersone() + " persone)",
                "LOCATION:Lume Ristorante\\, Via Chiaia 148\\, Napoli",
                "DESCRIPTION:Codice prenotazione " + p.getCodice(),
                "END:VEVENT", "END:VCALENDAR", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("lume-" + p.getCodice() + ".ics").build().toString())
                .contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
