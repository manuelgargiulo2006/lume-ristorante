package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.PiattoForm;
import it.manuel.ristorante.model.Categoria;
import it.manuel.ristorante.model.Ingrediente;
import it.manuel.ristorante.model.Piatto;
import it.manuel.ristorante.repository.CategoriaRepository;
import it.manuel.ristorante.repository.IngredienteRepository;
import it.manuel.ristorante.repository.PiattoRepository;
import it.manuel.ristorante.repository.RigaOrdineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gestione del menù: categorie, piatti, ricette e disponibilità. */
@Service
public class MenuService {

    private final CategoriaRepository categoriaRepository;
    private final PiattoRepository piattoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final RigaOrdineRepository rigaOrdineRepository;

    // ------------------------------------------------------------------ lettura

    @Transactional(readOnly = true)
    public List<Categoria> categorie() {
        return categoriaRepository.findAllByOrderByPosizioneAsc();
    }

    @Transactional(readOnly = true)
    public List<Piatto> piatti() {
        return piattoRepository.findAllByOrderByCategoriaPosizioneAscPosizioneAscNomeAsc();
    }

    /** Menù raggruppato per categoria, nell'ordine stabilito. */
    @Transactional(readOnly = true)
    public Map<Categoria, List<Piatto>> menuPerCategoria(boolean soloDisponibili) {
        Map<Categoria, List<Piatto>> menu = new LinkedHashMap<>();
        categorie().forEach(c -> menu.put(c, new java.util.ArrayList<>()));
        for (Piatto p : piatti()) {
            if (!soloDisponibili || p.isDisponibile()) {
                menu.computeIfAbsent(p.getCategoria(), c -> new java.util.ArrayList<>()).add(p);
            }
        }
        menu.values().removeIf(List::isEmpty);
        return menu;
    }

    @Transactional(readOnly = true)
    public List<Piatto> piattiFirma() {
        return piattoRepository.findByInEvidenzaTrueAndDisponibileTrueOrderByCategoriaPosizioneAscPosizioneAsc();
    }

    @Transactional(readOnly = true)
    public Piatto piatto(Long id) {
        return piattoRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Piatto non trovato"));
    }

    @Transactional(readOnly = true)
    public Categoria categoria(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Categoria non trovata"));
    }

    @Transactional(readOnly = true)
    public long contaPiatti(Long categoriaId) {
        return piattoRepository.countByCategoriaId(categoriaId);
    }

    @Transactional(readOnly = true)
    public long contaEsauriti() {
        return piattoRepository.countByDisponibileFalse();
    }

    // ------------------------------------------------------------------ piatti

    @Transactional
    public Piatto salvaPiatto(Long id, PiattoForm form) {
        String nome = Testi.nome(form.getNome());
        boolean duplicato = id == null
                ? piattoRepository.existsByNomeIgnoreCase(nome)
                : piattoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (duplicato) {
            throw new OperazioneNonValidaException("Nel menù c'è già un piatto chiamato \"" + nome + "\"");
        }
        if (form.isVegano() && !form.isVegetariano()) {
            form.setVegetariano(true);
        }
        Piatto piatto = id == null ? new Piatto() : piatto(id);
        piatto.setNome(nome);
        piatto.setDescrizione(Testi.facoltativo(form.getDescrizione()));
        piatto.setAbbinamento(Testi.facoltativo(form.getAbbinamento()));
        piatto.setPrezzo(form.getPrezzo());
        piatto.setCategoria(categoria(form.getCategoriaId()));
        piatto.getAllergeni().clear();
        if (form.getAllergeni() != null) {
            piatto.getAllergeni().addAll(form.getAllergeni());
        }
        piatto.setVegetariano(form.isVegetariano());
        piatto.setVegano(form.isVegano());
        piatto.setDisponibile(form.isDisponibile());
        piatto.setInEvidenza(form.isInEvidenza());
        piatto.setTempoPreparazione(form.getTempoPreparazione());
        if (id == null) {
            piatto.setPosizione((int) piattoRepository.countByCategoriaId(form.getCategoriaId()) + 1);
        }

        // ricetta: ogni ingrediente una volta sola, con quantità positiva
        piatto.getRicetta().clear();
        java.util.Set<Long> visti = new java.util.HashSet<>();
        for (PiattoForm.RigaRicetta riga : form.getRicetta()) {
            if (riga.getIngredienteId() == null || riga.getQuantita() == null || riga.getQuantita().signum() <= 0) {
                continue;
            }
            if (!visti.add(riga.getIngredienteId())) {
                continue;
            }
            Ingrediente ingrediente = ingredienteRepository.findById(riga.getIngredienteId())
                    .orElseThrow(() -> new NonTrovatoException("Ingrediente non trovato"));
            piatto.aggiungiIngrediente(ingrediente, riga.getQuantita());
        }
        return piattoRepository.save(piatto);
    }

    /** Segna un piatto come esaurito (o di nuovo disponibile) con un clic. */
    @Transactional
    public Piatto cambiaDisponibilita(Long id) {
        Piatto piatto = piatto(id);
        piatto.setDisponibile(!piatto.isDisponibile());
        return piatto;
    }

    @Transactional
    public void eliminaPiatto(Long id) {
        Piatto piatto = piatto(id);
        if (rigaOrdineRepository.existsByPiattoId(id)) {
            throw new OperazioneNonValidaException("\"" + piatto.getNome()
                    + "\" compare in ordini già registrati: invece di eliminarlo segnalo come esaurito.");
        }
        piattoRepository.delete(piatto);
    }

    @Transactional
    public void salvaTestiAi(Long id, String descrizione, String abbinamento) {
        Piatto piatto = piatto(id);
        piatto.setDescrizione(Testi.facoltativo(descrizione));
        piatto.setAbbinamento(Testi.facoltativo(abbinamento));
    }

    // ------------------------------------------------------------------ categorie

    @Transactional
    public Categoria salvaCategoria(Long id, String nomeGrezzo, String sottotitolo, int uscita) {
        String nome = Testi.nome(nomeGrezzo);
        if (nome.length() > 60) {
            throw new OperazioneNonValidaException("Il nome della categoria può avere al massimo 60 caratteri");
        }
        boolean duplicato = id == null
                ? categoriaRepository.existsByNomeIgnoreCase(nome)
                : categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (duplicato) {
            throw new OperazioneNonValidaException("La categoria \"" + nome + "\" esiste già");
        }
        Categoria categoria = id == null ? new Categoria() : categoria(id);
        categoria.setNome(nome);
        categoria.setSottotitolo(Testi.facoltativo(sottotitolo));
        categoria.setUscita(Math.max(1, Math.min(3, uscita)));
        if (id == null) {
            categoria.setPosizione(categorie().size() + 1);
        }
        return categoriaRepository.save(categoria);
    }

    /** Sposta una categoria su o giù nell'ordine del menù. */
    @Transactional
    public void spostaCategoria(Long id, int direzione) {
        List<Categoria> lista = new java.util.ArrayList<>(categorie());
        int i = lista.indexOf(categoria(id));
        int j = i + (direzione < 0 ? -1 : 1);
        if (i < 0 || j < 0 || j >= lista.size()) {
            return;
        }
        java.util.Collections.swap(lista, i, j);
        for (int k = 0; k < lista.size(); k++) {
            lista.get(k).setPosizione(k + 1);
        }
        categoriaRepository.saveAll(lista);
    }

    @Transactional
    public void eliminaCategoria(Long id) {
        Categoria categoria = categoria(id);
        long piatti = piattoRepository.countByCategoriaId(id);
        if (piatti > 0) {
            throw new OperazioneNonValidaException("\"" + categoria.getNome() + "\" contiene " + piatti
                    + (piatti == 1 ? " piatto" : " piatti") + ": spostali o eliminali prima.");
        }
        categoriaRepository.delete(categoria);
    }

    public MenuService(CategoriaRepository categoriaRepository, PiattoRepository piattoRepository, IngredienteRepository ingredienteRepository, RigaOrdineRepository rigaOrdineRepository) {
        this.categoriaRepository = categoriaRepository;
        this.piattoRepository = piattoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.rigaOrdineRepository = rigaOrdineRepository;
    }
}
