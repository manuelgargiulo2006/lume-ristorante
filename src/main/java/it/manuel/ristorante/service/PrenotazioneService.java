package it.manuel.ristorante.service;

import it.manuel.ristorante.config.LumeProperties;
import it.manuel.ristorante.dto.FasciaOraria;
import it.manuel.ristorante.dto.PrenotazioneForm;
import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.PrenotazioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Prenotazioni: disponibilità per fascia oraria, prenotazione online con codice,
 * assegnazione automatica del tavolo e arrivo del cliente.
 */
@Service
public class PrenotazioneService {

    /** Orari prenotabili. Il lunedì il ristorante è chiuso. */
    public static final List<LocalTime> PRANZO = List.of(LocalTime.of(12, 30), LocalTime.of(13, 0),
            LocalTime.of(13, 30), LocalTime.of(14, 0));
    public static final List<LocalTime> CENA = List.of(LocalTime.of(19, 30), LocalTime.of(20, 0),
            LocalTime.of(20, 30), LocalTime.of(21, 0), LocalTime.of(21, 30), LocalTime.of(22, 0));
    public static final DayOfWeek GIORNO_CHIUSURA = DayOfWeek.MONDAY;
    /** Oltre questo numero di persone la prenotazione online va confermata dallo staff. */
    static final int GRUPPO_DA_CONFERMARE = 7;
    static final List<StatoPrenotazione> STATI_ATTIVI = List.of(StatoPrenotazione.RICHIESTA,
            StatoPrenotazione.CONFERMATA, StatoPrenotazione.ARRIVATA);

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PrenotazioneRepository prenotazioneRepository;
    private final SalaService salaService;
    private final OrdineService ordineService;
    private final LumeProperties properties;

    // ------------------------------------------------------------------ lettura

    @Transactional(readOnly = true)
    public Prenotazione prenotazione(Long id) {
        return prenotazioneRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Prenotazione non trovata"));
    }

    @Transactional(readOnly = true)
    public Optional<Prenotazione> perCodice(String codice) {
        if (codice == null || codice.isBlank()) {
            return Optional.empty();
        }
        return prenotazioneRepository.findByCodiceIgnoreCase(codice.trim());
    }

    @Transactional(readOnly = true)
    public List<Prenotazione> delGiorno(LocalDate data) {
        return prenotazioneRepository.findByDataOrderByOraAsc(data);
    }

    @Transactional(readOnly = true)
    public long daConfermare() {
        return prenotazioneRepository.countByStato(StatoPrenotazione.RICHIESTA);
    }

    /** Coperti prenotati giorno per giorno (per la striscia del calendario). */
    @Transactional(readOnly = true)
    public Map<LocalDate, Integer> copertiPerGiorno(LocalDate da, int giorni) {
        Map<LocalDate, Integer> mappa = new LinkedHashMap<>();
        for (int i = 0; i < giorni; i++) {
            mappa.put(da.plusDays(i), 0);
        }
        for (Prenotazione p : prenotazioneRepository.findByDataBetweenOrderByDataAscOraAsc(da, da.plusDays(giorni - 1))) {
            if (p.isAttiva() || p.getStato() == StatoPrenotazione.COMPLETATA) {
                mappa.merge(p.getData(), p.getPersone(), Integer::sum);
            }
        }
        return mappa;
    }

    public boolean isChiuso(LocalDate data) {
        return data.getDayOfWeek() == GIORNO_CHIUSURA;
    }

    /** Orari del giorno con i posti ancora liberi per il numero di persone richiesto. */
    @Transactional(readOnly = true)
    public List<FasciaOraria> fasce(LocalDate data, int persone, Long escludiId) {
        if (data == null || isChiuso(data) || data.isBefore(LocalDate.now())) {
            return List.of();
        }
        List<Prenotazione> attive = prenotazioneRepository.findByDataAndStatoIn(data, STATI_ATTIVI).stream()
                .filter(p -> !p.getId().equals(escludiId))
                .toList();
        LocalDateTime minimo = LocalDateTime.now().plusMinutes(30);
        List<FasciaOraria> fasce = new ArrayList<>();
        for (LocalTime ora : orari()) {
            int occupati = attive.stream()
                    .filter(p -> sovrapposte(p.getOra(), ora))
                    .mapToInt(Prenotazione::getPersone)
                    .sum();
            int liberi = Math.max(0, properties.capienzaPerTurno() - occupati);
            boolean inTempo = LocalDateTime.of(data, ora).isAfter(minimo);
            fasce.add(new FasciaOraria(ora, PRANZO.contains(ora) ? "Pranzo" : "Cena", liberi,
                    inTempo && liberi >= persone));
        }
        return fasce;
    }

    public static List<LocalTime> orari() {
        List<LocalTime> tutti = new ArrayList<>(PRANZO);
        tutti.addAll(CENA);
        return tutti;
    }

    private boolean sovrapposte(LocalTime a, LocalTime b) {
        long distanza = Math.abs(java.time.Duration.between(a, b).toMinutes());
        return distanza < properties.durataTavoloMinuti();
    }

    // ------------------------------------------------------------------ creazione

    /** Prenotazione fatta dal cliente sul sito. */
    @Transactional
    public Prenotazione prenotaOnline(PrenotazioneForm form) {
        if (!orari().contains(form.getOra())) {
            throw new OperazioneNonValidaException("Scegli uno degli orari proposti");
        }
        Prenotazione p = nuova(form, Origine.ONLINE, null);
        p.setStato(form.getPersone() >= GRUPPO_DA_CONFERMARE ? StatoPrenotazione.RICHIESTA : StatoPrenotazione.CONFERMATA);
        if (p.getStato() == StatoPrenotazione.CONFERMATA) {
            p.setTavolo(suggerisciTavolo(p.getData(), p.getOra(), p.getPersone(), null).orElse(null));
        }
        return prenotazioneRepository.save(p);
    }

    /** Prenotazione inserita dallo staff (telefono o di persona). */
    @Transactional
    public Prenotazione salvaDaStaff(Long id, PrenotazioneForm form, Origine origine) {
        Prenotazione p;
        if (id == null) {
            p = nuova(form, origine, null);
            p.setStato(StatoPrenotazione.CONFERMATA);
        } else {
            p = prenotazione(id);
            if (!p.isAttiva()) {
                throw new OperazioneNonValidaException("Questa prenotazione è già chiusa");
            }
            verifica(form, id);
            compila(p, form);
        }
        if (form.getTavoloId() != null) {
            p.setTavolo(salaService.tavolo(form.getTavoloId()));
        } else if (p.getTavolo() == null) {
            p.setTavolo(suggerisciTavolo(p.getData(), p.getOra(), p.getPersone(), p.getId()).orElse(null));
        }
        return prenotazioneRepository.save(p);
    }

    private Prenotazione nuova(PrenotazioneForm form, Origine origine, Long id) {
        verifica(form, id);
        Prenotazione p = new Prenotazione();
        compila(p, form);
        p.setOrigine(origine);
        p.setCodice(nuovoCodice());
        p.setCreataIl(LocalDateTime.now());
        return p;
    }

    private void verifica(PrenotazioneForm form, Long escludiId) {
        if (form.getData().isBefore(LocalDate.now())) {
            throw new OperazioneNonValidaException("La data è già passata");
        }
        if (form.getData().isAfter(LocalDate.now().plusMonths(3))) {
            throw new OperazioneNonValidaException("Accettiamo prenotazioni fino a tre mesi in anticipo");
        }
        if (isChiuso(form.getData())) {
            throw new OperazioneNonValidaException("Il lunedì il ristorante è chiuso");
        }
        FasciaOraria fascia = fasce(form.getData(), form.getPersone(), escludiId).stream()
                .filter(f -> f.ora().equals(form.getOra()))
                .findFirst()
                .orElse(null);
        if (fascia != null && !fascia.disponibile()) {
            throw new OperazioneNonValidaException(fascia.postiLiberi() == 0
                    ? "Alle " + fascia.getEtichetta() + " siamo al completo: scegli un altro orario"
                    : "Alle " + fascia.getEtichetta() + " restano solo " + fascia.postiLiberi() + " posti");
        }
    }

    private void compila(Prenotazione p, PrenotazioneForm form) {
        p.setNome(Testi.nome(form.getNome()));
        p.setTelefono(form.getTelefono().trim());
        p.setEmail(Testi.facoltativo(form.getEmail()));
        p.setData(form.getData());
        p.setOra(form.getOra());
        p.setPersone(form.getPersone());
        p.setNote(Testi.facoltativo(form.getNote()));
    }

    private String nuovoCodice() {
        String codice;
        do {
            StringBuilder sb = new StringBuilder("LM-");
            for (int i = 0; i < 5; i++) {
                sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            }
            codice = sb.toString();
        } while (prenotazioneRepository.existsByCodice(codice));
        return codice;
    }

    /** Il tavolo più piccolo adatto e libero in quella fascia oraria. */
    @Transactional(readOnly = true)
    public Optional<Tavolo> suggerisciTavolo(LocalDate data, LocalTime ora, int persone, Long escludiId) {
        Set<Long> occupati = new HashSet<>();
        for (Prenotazione p : prenotazioneRepository.findByDataAndStatoIn(data, STATI_ATTIVI)) {
            if (p.getTavolo() != null && !p.getId().equals(escludiId) && sovrapposte(p.getOra(), ora)) {
                occupati.add(p.getTavolo().getId());
            }
        }
        return salaService.tavoliAttivi().stream()
                .filter(t -> t.getPosti() >= persone && !occupati.contains(t.getId()))
                .min(Comparator.comparingInt(Tavolo::getPosti).thenComparingInt(Tavolo::getNumero));
    }

    // ------------------------------------------------------------------ stato

    @Transactional
    public Prenotazione cambiaStato(Long id, StatoPrenotazione stato) {
        Prenotazione p = prenotazione(id);
        if (stato == StatoPrenotazione.ARRIVATA) {
            throw new OperazioneNonValidaException("Usa \"Fai accomodare\" per aprire il tavolo");
        }
        if (stato == StatoPrenotazione.CONFERMATA && p.getTavolo() == null) {
            p.setTavolo(suggerisciTavolo(p.getData(), p.getOra(), p.getPersone(), p.getId()).orElse(null));
        }
        p.setStato(stato);
        return p;
    }

    /** Il cliente è arrivato: si apre la comanda sul tavolo assegnato. */
    @Transactional
    public Ordine faiAccomodare(Long id, Long cameriereId) {
        Prenotazione p = prenotazione(id);
        if (!p.isAttiva() || p.getStato() == StatoPrenotazione.ARRIVATA) {
            throw new OperazioneNonValidaException("La prenotazione non è in attesa di arrivo");
        }
        Tavolo tavolo = p.getTavolo();
        if (tavolo == null) {
            tavolo = suggerisciTavolo(p.getData(), p.getOra(), p.getPersone(), p.getId())
                    .orElseThrow(() -> new OperazioneNonValidaException("Nessun tavolo libero adatto: assegnane uno a mano"));
        }
        return ordineService.apri(tavolo.getId(), p.getPersone(), cameriereId, p.getId());
    }

    /** Il cliente annulla dal sito: per sicurezza deve indicare anche il telefono usato. */
    @Transactional
    public Prenotazione annullaDaCliente(String codice, String telefono) {
        Prenotazione p = perCodice(codice)
                .orElseThrow(() -> new OperazioneNonValidaException("Nessuna prenotazione con questo codice"));
        String cifre = telefono == null ? "" : telefono.replaceAll("\\D", "");
        String attese = p.getTelefono().replaceAll("\\D", "");
        if (cifre.length() < 6 || !attese.endsWith(cifre.substring(Math.max(0, cifre.length() - 6)))) {
            throw new OperazioneNonValidaException("Il numero di telefono non corrisponde a quello della prenotazione");
        }
        if (p.getStato() != StatoPrenotazione.RICHIESTA && p.getStato() != StatoPrenotazione.CONFERMATA) {
            throw new OperazioneNonValidaException("Questa prenotazione non può più essere annullata online");
        }
        p.setStato(StatoPrenotazione.CANCELLATA);
        return p;
    }

    public PrenotazioneService(PrenotazioneRepository prenotazioneRepository, SalaService salaService, OrdineService ordineService, LumeProperties properties) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.salaService = salaService;
        this.ordineService = ordineService;
        this.properties = properties;
    }
}
