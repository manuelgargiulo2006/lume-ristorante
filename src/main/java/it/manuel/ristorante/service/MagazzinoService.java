package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.IngredienteForm;
import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.IngredienteRepository;
import it.manuel.ristorante.repository.MovimentoMagazzinoRepository;
import it.manuel.ristorante.repository.PiattoIngredienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Magazzino: scorte, carichi, sprechi e scarico automatico a ogni conto chiuso. */
@Service
public class MagazzinoService {

    private final IngredienteRepository ingredienteRepository;
    private final MovimentoMagazzinoRepository movimentoRepository;
    private final PiattoIngredienteRepository piattoIngredienteRepository;

    @Transactional(readOnly = true)
    public List<Ingrediente> ingredienti() {
        return ingredienteRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Ingrediente> sottoScorta() {
        return ingredienti().stream().filter(Ingrediente::isSottoScorta).toList();
    }

    @Transactional(readOnly = true)
    public Ingrediente ingrediente(Long id) {
        return ingredienteRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Ingrediente non trovato"));
    }

    @Transactional(readOnly = true)
    public List<MovimentoMagazzino> ultimiMovimenti() {
        return movimentoRepository.findTop40ByOrderByDataDesc();
    }

    @Transactional(readOnly = true)
    public BigDecimal valoreMagazzino() {
        return ingredienti().stream().map(Ingrediente::getValore).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Ingrediente salva(Long id, IngredienteForm form) {
        String nome = Testi.nome(form.getNome());
        boolean duplicato = id == null
                ? ingredienteRepository.existsByNomeIgnoreCase(nome)
                : ingredienteRepository.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (duplicato) {
            throw new OperazioneNonValidaException("L'ingrediente \"" + nome + "\" è già in magazzino");
        }
        Ingrediente ingrediente = id == null ? new Ingrediente() : ingrediente(id);
        BigDecimal scortaPrecedente = ingrediente.getScorta();
        ingrediente.setNome(nome);
        ingrediente.setUnita(form.getUnita());
        ingrediente.setSogliaMinima(form.getSogliaMinima());
        ingrediente.setCostoUnitario(form.getCostoUnitario());
        ingrediente.setFornitore(Testi.facoltativo(form.getFornitore()));
        ingrediente.setScorta(form.getScorta());
        ingredienteRepository.save(ingrediente);

        BigDecimal differenza = form.getScorta().subtract(scortaPrecedente == null ? BigDecimal.ZERO : scortaPrecedente);
        if (differenza.signum() != 0) {
            movimentoRepository.save(new MovimentoMagazzino(ingrediente,
                    id == null ? TipoMovimento.CARICO : TipoMovimento.RETTIFICA, differenza,
                    id == null ? "Scorta iniziale" : "Correzione dalla scheda ingrediente"));
        }
        return ingrediente;
    }

    /**
     * Registra un movimento manuale.
     *
     * @param quantita sempre positiva: il segno lo decide il tipo (carico +, spreco -)
     */
    @Transactional
    public MovimentoMagazzino registra(Long ingredienteId, TipoMovimento tipo, BigDecimal quantita, String nota) {
        if (quantita == null || quantita.signum() <= 0) {
            throw new OperazioneNonValidaException("Indica una quantità maggiore di zero");
        }
        if (tipo == TipoMovimento.SCARICO_VENDITA) {
            throw new OperazioneNonValidaException("Lo scarico per vendita è automatico alla chiusura del conto");
        }
        Ingrediente ingrediente = ingrediente(ingredienteId);
        BigDecimal delta;
        if (tipo == TipoMovimento.CARICO) {
            delta = quantita;
        } else if (tipo == TipoMovimento.SPRECO) {
            if (quantita.compareTo(ingrediente.getScorta()) > 0) {
                throw new OperazioneNonValidaException("In magazzino ci sono solo "
                        + ingrediente.getScorta().stripTrailingZeros().toPlainString() + " "
                        + ingrediente.getUnita().getLabel() + " di " + ingrediente.getNome());
            }
            delta = quantita.negate();
        } else {
            // rettifica: la quantità indicata è la nuova giacenza contata a mano
            delta = quantita.subtract(ingrediente.getScorta());
        }
        ingrediente.setScorta(ingrediente.getScorta().add(delta));
        return movimentoRepository.save(new MovimentoMagazzino(ingrediente, tipo, delta, Testi.facoltativo(nota)));
    }

    /** Scala dal magazzino gli ingredienti di tutti i piatti di un conto chiuso. */
    @Transactional
    public void scaricaOrdine(Ordine ordine) {
        Map<Ingrediente, BigDecimal> consumi = new LinkedHashMap<>();
        for (RigaOrdine riga : ordine.getRighe()) {
            for (PiattoIngrediente pi : riga.getPiatto().getRicetta()) {
                BigDecimal q = pi.getQuantita().multiply(BigDecimal.valueOf(riga.getQuantita()));
                consumi.merge(pi.getIngrediente(), q, BigDecimal::add);
            }
        }
        String nota = "Tavolo " + ordine.getTavolo().getNumero() + " - conto n. " + ordine.getId();
        consumi.forEach((ingrediente, q) -> {
            ingrediente.setScorta(ingrediente.getScorta().subtract(q).setScale(3, RoundingMode.HALF_UP));
            movimentoRepository.save(new MovimentoMagazzino(ingrediente, TipoMovimento.SCARICO_VENDITA, q.negate(), nota));
        });
    }

    @Transactional
    public void elimina(Long id) {
        Ingrediente ingrediente = ingrediente(id);
        if (piattoIngredienteRepository.existsByIngredienteId(id)) {
            throw new OperazioneNonValidaException("\"" + ingrediente.getNome()
                    + "\" è usato nelle ricette di alcuni piatti: toglilo prima dalle ricette.");
        }
        if (movimentoRepository.existsByIngredienteId(id)) {
            throw new OperazioneNonValidaException("\"" + ingrediente.getNome()
                    + "\" ha uno storico di movimenti e non può essere eliminato.");
        }
        ingredienteRepository.delete(ingrediente);
    }

    public MagazzinoService(IngredienteRepository ingredienteRepository, MovimentoMagazzinoRepository movimentoRepository, PiattoIngredienteRepository piattoIngredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.movimentoRepository = movimentoRepository;
        this.piattoIngredienteRepository = piattoIngredienteRepository;
    }
}
