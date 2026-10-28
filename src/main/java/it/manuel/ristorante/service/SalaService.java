package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.TavoloForm;
import it.manuel.ristorante.dto.TavoloVista;
import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.OrdineRepository;
import it.manuel.ristorante.repository.PrenotazioneRepository;
import it.manuel.ristorante.repository.TavoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Tavoli della sala e loro stato in tempo reale. */
@Service
public class SalaService {

    static final List<StatoOrdine> STATI_ATTIVI = List.of(StatoOrdine.APERTO, StatoOrdine.CONTO);

    private final TavoloRepository tavoloRepository;
    private final OrdineRepository ordineRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    @Transactional(readOnly = true)
    public List<Tavolo> tavoli() {
        return tavoloRepository.findAllByOrderByNumeroAsc();
    }

    @Transactional(readOnly = true)
    public List<Tavolo> tavoliAttivi() {
        return tavoloRepository.findByAttivoTrueOrderByNumeroAsc();
    }

    @Transactional(readOnly = true)
    public Tavolo tavolo(Long id) {
        return tavoloRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Tavolo non trovato"));
    }

    /** Fotografia della sala: per ogni tavolo la comanda aperta e la prossima prenotazione. */
    @Transactional(readOnly = true)
    public List<TavoloVista> vista() {
        Map<Long, Ordine> ordini = new HashMap<>();
        for (Ordine o : ordineRepository.findByStatoInOrderByApertoIlAsc(STATI_ATTIVI)) {
            ordini.put(o.getTavolo().getId(), o);
        }
        LocalTime limite = LocalTime.now().minusMinutes(30);
        Map<Long, Prenotazione> prossime = new HashMap<>();
        for (Prenotazione p : prenotazioneRepository.findByDataAndStatoIn(LocalDate.now(),
                List.of(StatoPrenotazione.RICHIESTA, StatoPrenotazione.CONFERMATA))) {
            if (p.getTavolo() != null && !p.getOra().isBefore(limite)) {
                prossime.merge(p.getTavolo().getId(), p, (a, b) -> a.getOra().isBefore(b.getOra()) ? a : b);
            }
        }
        return tavoliAttivi().stream()
                .map(t -> new TavoloVista(t, ordini.get(t.getId()), prossime.get(t.getId())))
                .toList();
    }

    /** Conteggi per la legenda della mappa: libero, occupato, conto... */
    public Map<String, Long> riepilogo(List<TavoloVista> vista) {
        Map<String, Long> conteggi = new LinkedHashMap<>();
        for (String s : List.of("libero", "prenotato", "occupato", "pronto", "conto")) {
            conteggi.put(s, vista.stream().filter(v -> v.stato().equals(s)).count());
        }
        return conteggi;
    }

    // ------------------------------------------------------------------ gestione tavoli

    @Transactional
    public Tavolo salva(Long id, TavoloForm form) {
        boolean duplicato = id == null
                ? tavoloRepository.existsByNumero(form.getNumero())
                : tavoloRepository.existsByNumeroAndIdNot(form.getNumero(), id);
        if (duplicato) {
            throw new OperazioneNonValidaException("Esiste già il tavolo " + form.getNumero());
        }
        Tavolo tavolo = id == null ? new Tavolo() : tavolo(id);
        if (!form.isAttivo() && id != null
                && ordineRepository.findFirstByTavoloIdAndStatoIn(id, STATI_ATTIVI).isPresent()) {
            throw new OperazioneNonValidaException("Il tavolo " + tavolo.getNumero()
                    + " ha una comanda aperta: chiudila prima di disattivarlo.");
        }
        tavolo.setNumero(form.getNumero());
        tavolo.setPosti(form.getPosti());
        tavolo.setZona(form.getZona());
        tavolo.setForma(form.getForma());
        tavolo.setPosX(form.getPosX());
        tavolo.setPosY(form.getPosY());
        tavolo.setAttivo(form.isAttivo());
        return tavoloRepository.save(tavolo);
    }

    /** Sposta un tavolo sulla mappa (trascinamento dal gestionale). */
    @Transactional
    public void sposta(Long id, int x, int y) {
        Tavolo tavolo = tavolo(id);
        tavolo.setPosX(Math.max(3, Math.min(97, x)));
        tavolo.setPosY(Math.max(4, Math.min(96, y)));
    }

    @Transactional
    public void elimina(Long id) {
        Tavolo tavolo = tavolo(id);
        if (ordineRepository.existsByTavoloId(id)) {
            throw new OperazioneNonValidaException("Il tavolo " + tavolo.getNumero()
                    + " ha uno storico di conti: puoi disattivarlo invece di eliminarlo.");
        }
        tavoloRepository.delete(tavolo);
    }

    public SalaService(TavoloRepository tavoloRepository, OrdineRepository ordineRepository, PrenotazioneRepository prenotazioneRepository) {
        this.tavoloRepository = tavoloRepository;
        this.ordineRepository = ordineRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }
}
