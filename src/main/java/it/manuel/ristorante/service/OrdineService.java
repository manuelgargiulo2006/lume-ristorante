package it.manuel.ristorante.service;

import it.manuel.ristorante.config.LumeProperties;
import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.OrdineRepository;
import it.manuel.ristorante.repository.PiattoRepository;
import it.manuel.ristorante.repository.PrenotazioneRepository;
import it.manuel.ristorante.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Comande di sala: apertura del tavolo, piatti, invio in cucina, conto e pagamento.
 * Alla chiusura del conto gli ingredienti vengono scalati dal magazzino.
 */
@Service
public class OrdineService {

    private final OrdineRepository ordineRepository;
    private final PiattoRepository piattoRepository;
    private final StaffRepository staffRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final SalaService salaService;
    private final MagazzinoService magazzinoService;
    private final LumeProperties properties;

    @Transactional(readOnly = true)
    public Ordine ordine(Long id) {
        return ordineRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Comanda non trovata"));
    }

    @Transactional(readOnly = true)
    public List<Ordine> attivi() {
        return ordineRepository.findByStatoInOrderByApertoIlAsc(SalaService.STATI_ATTIVI);
    }

    @Transactional(readOnly = true)
    public List<Ordine> ultimiChiusi() {
        return ordineRepository.findTop8ByStatoOrderByChiusoIlDesc(StatoOrdine.CHIUSO);
    }

    /** Apre un tavolo. Se il tavolo ha già una comanda aperta restituisce quella. */
    @Transactional
    public Ordine apri(Long tavoloId, int coperti, Long cameriereId, Long prenotazioneId) {
        Tavolo tavolo = salaService.tavolo(tavoloId);
        if (!tavolo.isAttivo()) {
            throw new OperazioneNonValidaException("Il tavolo " + tavolo.getNumero() + " non è in servizio");
        }
        var esistente = ordineRepository.findFirstByTavoloIdAndStatoIn(tavoloId, SalaService.STATI_ATTIVI);
        if (esistente.isPresent()) {
            return esistente.get();
        }
        if (coperti < 1 || coperti > 30) {
            throw new OperazioneNonValidaException("Indica un numero di coperti tra 1 e 30");
        }
        Ordine ordine = new Ordine();
        ordine.setTavolo(tavolo);
        ordine.setCoperti(coperti);
        ordine.setCopertoUnitario(properties.coperto());
        ordine.setApertoIl(LocalDateTime.now());
        if (cameriereId != null) {
            staffRepository.findById(cameriereId).ifPresent(ordine::setCameriere);
        }
        if (prenotazioneId != null) {
            prenotazioneRepository.findById(prenotazioneId).ifPresent(p -> {
                p.setStato(StatoPrenotazione.ARRIVATA);
                p.setTavolo(tavolo);
                ordine.setPrenotazione(p);
                if (p.getNote() != null) {
                    ordine.setNote(p.getNote());
                }
            });
        }
        return ordineRepository.save(ordine);
    }

    // ------------------------------------------------------------------ piatti

    @Transactional
    public RigaOrdine aggiungi(Long ordineId, Long piattoId, int quantita, String note) {
        Ordine ordine = modificabile(ordineId);
        Piatto piatto = piattoRepository.findById(piattoId)
                .orElseThrow(() -> new NonTrovatoException("Piatto non trovato"));
        if (!piatto.isDisponibile()) {
            throw new OperazioneNonValidaException("\"" + piatto.getNome() + "\" è esaurito per oggi");
        }
        if (quantita < 1 || quantita > 50) {
            throw new OperazioneNonValidaException("Quantità non valida");
        }
        String nota = Testi.facoltativo(note);
        // se lo stesso piatto non ancora inviato è già in comanda con la stessa nota, aumento la quantità
        for (RigaOrdine r : ordine.getRighe()) {
            if (r.getStato() == StatoRiga.IN_ATTESA && r.getPiatto().getId().equals(piattoId)
                    && Objects.equals(r.getNote(), nota)) {
                r.setQuantita(r.getQuantita() + quantita);
                return r;
            }
        }
        RigaOrdine riga = new RigaOrdine();
        riga.setPiatto(piatto);
        riga.setQuantita(quantita);
        riga.setPrezzoUnitario(piatto.getPrezzo());
        riga.setNote(nota);
        riga.setUscita(piatto.getCategoria().getUscita());
        ordine.aggiungiRiga(riga);
        ordineRepository.save(ordine);
        return riga;
    }

    /** +1 / -1 su un piatto non ancora inviato in cucina. A zero il piatto viene tolto. */
    @Transactional
    public void cambiaQuantita(Long ordineId, Long rigaId, int delta) {
        Ordine ordine = modificabile(ordineId);
        RigaOrdine riga = riga(ordine, rigaId);
        if (riga.getStato() != StatoRiga.IN_ATTESA) {
            throw new OperazioneNonValidaException("\"" + riga.getPiatto().getNome()
                    + "\" è già stato inviato in cucina e non si può modificare");
        }
        int nuova = riga.getQuantita() + delta;
        if (nuova <= 0) {
            ordine.getRighe().remove(riga);
        } else {
            riga.setQuantita(Math.min(nuova, 50));
        }
    }

    @Transactional
    public void rimuovi(Long ordineId, Long rigaId) {
        Ordine ordine = modificabile(ordineId);
        RigaOrdine riga = riga(ordine, rigaId);
        if (riga.getStato() != StatoRiga.IN_ATTESA) {
            throw new OperazioneNonValidaException("Un piatto già inviato in cucina non si può togliere");
        }
        ordine.getRighe().remove(riga);
    }

    @Transactional
    public void nota(Long ordineId, Long rigaId, String nota) {
        Ordine ordine = modificabile(ordineId);
        RigaOrdine riga = riga(ordine, rigaId);
        if (riga.getStato() != StatoRiga.IN_ATTESA) {
            throw new OperazioneNonValidaException("La cucina ha già ricevuto questo piatto");
        }
        String pulita = Testi.facoltativo(nota);
        if (pulita != null && pulita.length() > 200) {
            throw new OperazioneNonValidaException("La nota può avere al massimo 200 caratteri");
        }
        riga.setNote(pulita);
    }

    @Transactional
    public void cambiaUscita(Long ordineId, Long rigaId, int uscita) {
        Ordine ordine = modificabile(ordineId);
        RigaOrdine riga = riga(ordine, rigaId);
        if (riga.getStato() != StatoRiga.IN_ATTESA) {
            throw new OperazioneNonValidaException("Il piatto è già in cucina");
        }
        riga.setUscita(Math.max(1, Math.min(3, uscita)));
    }

    @Transactional
    public void aggiornaCoperti(Long ordineId, int coperti) {
        if (coperti < 1 || coperti > 30) {
            throw new OperazioneNonValidaException("Indica un numero di coperti tra 1 e 30");
        }
        modificabile(ordineId).setCoperti(coperti);
    }

    // ------------------------------------------------------------------ cucina e servizio

    /** Manda in cucina tutti i piatti ancora in bozza. Restituisce quanti piatti sono partiti. */
    @Transactional
    public int inviaInCucina(Long ordineId) {
        Ordine ordine = modificabile(ordineId);
        LocalDateTime adesso = LocalDateTime.now();
        int inviati = 0;
        for (RigaOrdine r : ordine.getRighe()) {
            if (r.getStato() == StatoRiga.IN_ATTESA) {
                r.setStato(StatoRiga.INVIATO);
                r.setInviatoIl(adesso);
                inviati += r.getQuantita();
            }
        }
        if (inviati == 0) {
            throw new OperazioneNonValidaException("Non ci sono piatti nuovi da inviare in cucina");
        }
        return inviati;
    }

    @Transactional
    public int serviPronti(Long ordineId) {
        Ordine ordine = ordine(ordineId);
        int serviti = 0;
        for (RigaOrdine r : ordine.getRighe()) {
            if (r.getStato() == StatoRiga.PRONTO) {
                r.setStato(StatoRiga.SERVITO);
                serviti += r.getQuantita();
            }
        }
        return serviti;
    }

    // ------------------------------------------------------------------ conto

    @Transactional
    public void richiediConto(Long ordineId) {
        Ordine ordine = modificabile(ordineId);
        if (ordine.getRighe().isEmpty()) {
            throw new OperazioneNonValidaException("La comanda è vuota");
        }
        ordine.setStato(StatoOrdine.CONTO);
    }

    @Transactional
    public void riapri(Long ordineId) {
        Ordine ordine = ordine(ordineId);
        if (ordine.getStato() != StatoOrdine.CONTO) {
            throw new OperazioneNonValidaException("La comanda non è in attesa del conto");
        }
        ordine.setStato(StatoOrdine.APERTO);
    }

    @Transactional
    public Ordine chiudi(Long ordineId, MetodoPagamento metodo, int sconto) {
        Ordine ordine = ordine(ordineId);
        if (!ordine.isAttivo()) {
            throw new OperazioneNonValidaException("Questo conto è già stato chiuso");
        }
        if (ordine.getRighe().isEmpty()) {
            throw new OperazioneNonValidaException("Non si può chiudere un conto vuoto: annulla la comanda");
        }
        if (ordine.haRigheDaInviare()) {
            throw new OperazioneNonValidaException("Ci sono piatti non inviati in cucina: inviali o toglili prima del conto");
        }
        if (metodo == null) {
            throw new OperazioneNonValidaException("Scegli il metodo di pagamento");
        }
        if (sconto < 0 || sconto > 100) {
            throw new OperazioneNonValidaException("Lo sconto deve essere tra 0 e 100%");
        }
        ordine.getRighe().forEach(r -> {
            if (r.getStato() != StatoRiga.SERVITO) {
                r.setStato(StatoRiga.SERVITO);
            }
        });
        ordine.setScontoPercentuale(sconto);
        ordine.setMetodoPagamento(metodo);
        ordine.setTotale(ordine.getTotaleCorrente());
        ordine.setChiusoIl(LocalDateTime.now());
        ordine.setStato(StatoOrdine.CHIUSO);
        if (ordine.getPrenotazione() != null) {
            ordine.getPrenotazione().setStato(StatoPrenotazione.COMPLETATA);
        }
        magazzinoService.scaricaOrdine(ordine);
        return ordine;
    }

    /** Annulla una comanda aperta per errore (solo se la cucina non ha ricevuto nulla). */
    @Transactional
    public void annulla(Long ordineId) {
        Ordine ordine = modificabile(ordineId);
        boolean inCucina = ordine.getRighe().stream().anyMatch(r -> r.getStato() != StatoRiga.IN_ATTESA);
        if (inCucina) {
            throw new OperazioneNonValidaException("La cucina ha già ricevuto dei piatti: chiudi il conto invece di annullarlo");
        }
        ordine.getRighe().clear();
        ordine.setStato(StatoOrdine.ANNULLATO);
        ordine.setChiusoIl(LocalDateTime.now());
        if (ordine.getPrenotazione() != null) {
            ordine.getPrenotazione().setStato(StatoPrenotazione.CONFERMATA);
        }
    }

    private Ordine modificabile(Long ordineId) {
        Ordine ordine = ordine(ordineId);
        if (ordine.getStato() == StatoOrdine.CONTO) {
            throw new OperazioneNonValidaException("Il conto è stato richiesto: riapri la comanda per modificarla");
        }
        if (!ordine.isAttivo()) {
            throw new OperazioneNonValidaException("Questa comanda è chiusa");
        }
        return ordine;
    }

    private RigaOrdine riga(Ordine ordine, Long rigaId) {
        return ordine.getRighe().stream()
                .filter(r -> r.getId() != null && r.getId().equals(rigaId))
                .findFirst()
                .orElseThrow(() -> new NonTrovatoException("Piatto non trovato nella comanda"));
    }

    public OrdineService(OrdineRepository ordineRepository, PiattoRepository piattoRepository, StaffRepository staffRepository, PrenotazioneRepository prenotazioneRepository, SalaService salaService, MagazzinoService magazzinoService, LumeProperties properties) {
        this.ordineRepository = ordineRepository;
        this.piattoRepository = piattoRepository;
        this.staffRepository = staffRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.salaService = salaService;
        this.magazzinoService = magazzinoService;
        this.properties = properties;
    }
}
