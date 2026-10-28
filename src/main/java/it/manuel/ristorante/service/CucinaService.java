package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.TicketCucina;
import it.manuel.ristorante.model.Ordine;
import it.manuel.ristorante.model.RigaOrdine;
import it.manuel.ristorante.model.StatoRiga;
import it.manuel.ristorante.repository.OrdineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Monitor della cucina: comande ricevute e avanzamento dei piatti. */
@Service
public class CucinaService {

    /** Oltre questi minuti dall'invio il ticket diventa arancione, poi rosso. */
    static final int MINUTI_ATTENZIONE = 12;
    static final int MINUTI_RITARDO = 20;

    private final OrdineRepository ordineRepository;

    @Transactional(readOnly = true)
    public List<TicketCucina> tickets() {
        LocalDateTime adesso = LocalDateTime.now();
        return ordineRepository.findByStatoInOrderByApertoIlAsc(SalaService.STATI_ATTIVI).stream()
                .map(o -> ticket(o, adesso))
                .filter(t -> !t.righe().isEmpty())
                .sorted(Comparator.comparing((TicketCucina t) -> t.urgenza().equals("pronto"))
                        .thenComparing(TicketCucina::minutiAttesa, Comparator.reverseOrder()))
                .toList();
    }

    private TicketCucina ticket(Ordine ordine, LocalDateTime adesso) {
        List<TicketCucina.RigaCucina> righe = ordine.getRighe().stream()
                .filter(r -> r.getStato() == StatoRiga.INVIATO || r.getStato() == StatoRiga.IN_PREPARAZIONE
                        || r.getStato() == StatoRiga.PRONTO)
                .sorted(Comparator.comparingInt(RigaOrdine::getUscita).thenComparing(RigaOrdine::getId))
                .map(r -> new TicketCucina.RigaCucina(r.getId(), r.getPiatto().getNome(), r.getQuantita(),
                        r.getNote(), r.getStato().name(), r.getStato().getLabel(), r.getUscita(),
                        r.getInviatoIl() == null ? 0 : Duration.between(r.getInviatoIl(), adesso).toMinutes()))
                .toList();

        // il tempo di attesa è quello del piatto più vecchio non ancora pronto
        long attesa = righe.stream()
                .filter(r -> !r.stato().equals("PRONTO"))
                .mapToLong(TicketCucina.RigaCucina::minuti)
                .max().orElse(0);
        boolean tuttoPronto = !righe.isEmpty() && righe.stream().allMatch(r -> r.stato().equals("PRONTO"));
        String urgenza = tuttoPronto ? "pronto"
                : attesa >= MINUTI_RITARDO ? "ritardo"
                : attesa >= MINUTI_ATTENZIONE ? "attenzione" : "regolare";
        String cameriere = ordine.getCameriere() == null ? "" : ordine.getCameriere().getNomeCompleto();
        return new TicketCucina(ordine.getId(), ordine.getTavolo().getNumero(), ordine.getCoperti(), cameriere,
                attesa, urgenza, righe);
    }

    /** Porta un piatto allo stato successivo: in coda, in preparazione, pronto. */
    @Transactional
    public void avanza(Long ordineId, Long rigaId) {
        RigaOrdine riga = riga(ordineId, rigaId);
        switch (riga.getStato()) {
            case INVIATO -> riga.setStato(StatoRiga.IN_PREPARAZIONE);
            case IN_PREPARAZIONE -> {
                riga.setStato(StatoRiga.PRONTO);
                riga.setProntoIl(LocalDateTime.now());
            }
            default -> throw new OperazioneNonValidaException("Il piatto è già pronto");
        }
    }

    /** Annulla l'ultimo avanzamento (tocco sbagliato). */
    @Transactional
    public void indietro(Long ordineId, Long rigaId) {
        RigaOrdine riga = riga(ordineId, rigaId);
        switch (riga.getStato()) {
            case PRONTO -> {
                riga.setStato(StatoRiga.IN_PREPARAZIONE);
                riga.setProntoIl(null);
            }
            case IN_PREPARAZIONE -> riga.setStato(StatoRiga.INVIATO);
            default -> throw new OperazioneNonValidaException("Il piatto è appena arrivato in cucina");
        }
    }

    /** Segna come pronti tutti i piatti di una comanda. */
    @Transactional
    public void tuttoPronto(Long ordineId) {
        Ordine ordine = ordineRepository.findById(ordineId)
                .orElseThrow(() -> new NonTrovatoException("Comanda non trovata"));
        LocalDateTime adesso = LocalDateTime.now();
        ordine.getRighe().stream()
                .filter(r -> r.getStato() == StatoRiga.INVIATO || r.getStato() == StatoRiga.IN_PREPARAZIONE)
                .forEach(r -> {
                    r.setStato(StatoRiga.PRONTO);
                    r.setProntoIl(adesso);
                });
    }

    private RigaOrdine riga(Long ordineId, Long rigaId) {
        Ordine ordine = ordineRepository.findById(ordineId)
                .orElseThrow(() -> new NonTrovatoException("Comanda non trovata"));
        return ordine.getRighe().stream()
                .filter(r -> r.getId().equals(rigaId))
                .findFirst()
                .orElseThrow(() -> new NonTrovatoException("Piatto non trovato"));
    }

    public CucinaService(OrdineRepository ordineRepository) {
        this.ordineRepository = ordineRepository;
    }
}
