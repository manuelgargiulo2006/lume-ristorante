package it.manuel.ristorante.config;

import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static it.manuel.ristorante.model.Allergene.*;

/**
 * Al primo avvio (database vuoto) crea un ristorante dimostrativo completo:
 * staff, sala, magazzino, menù con ricette, un mese di conti per le statistiche,
 * tavoli occupati in questo momento e prenotazioni dei prossimi giorni.
 *
 * Account: admin / admin1234, sala / sala1234, cucina / cucina1234
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final int GIORNI_STORICO = 28;

    private final StaffRepository staffRepository;
    private final TavoloRepository tavoloRepository;
    private final CategoriaRepository categoriaRepository;
    private final PiattoRepository piattoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final OrdineRepository ordineRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final MovimentoMagazzinoRepository movimentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LumeProperties properties;

    private final Map<String, Ingrediente> ingredienti = new HashMap<>();
    private final Map<String, Piatto> piatti = new LinkedHashMap<>();
    private final Map<Integer, Tavolo> tavoli = new LinkedHashMap<>();
    private final List<Staff> camerieri = new ArrayList<>();
    private final Random random = new Random(2026);
    private final Set<String> codici = new HashSet<>();

    public DataLoader(StaffRepository staffRepository, TavoloRepository tavoloRepository,
                      CategoriaRepository categoriaRepository, PiattoRepository piattoRepository,
                      IngredienteRepository ingredienteRepository, OrdineRepository ordineRepository,
                      PrenotazioneRepository prenotazioneRepository, MovimentoMagazzinoRepository movimentoRepository,
                      PasswordEncoder passwordEncoder, LumeProperties properties) {
        this.staffRepository = staffRepository;
        this.tavoloRepository = tavoloRepository;
        this.categoriaRepository = categoriaRepository;
        this.piattoRepository = piattoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.ordineRepository = ordineRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.movimentoRepository = movimentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }
        log.info("Database vuoto: creo il ristorante dimostrativo...");
        staff();
        sala();
        magazzino();
        menu();
        storico();
        servizioInCorso();
        prenotazioni();
        log.info("Ristorante dimostrativo pronto. Accedi con admin/admin1234, sala/sala1234 o cucina/cucina1234");
    }

    // ------------------------------------------------------------------ staff e sala

    private void staff() {
        staffRepository.save(new Staff("admin", "Giulia Ferrara", passwordEncoder.encode("admin1234"), Ruolo.ADMIN));
        camerieri.add(staffRepository.save(new Staff("sala", "Marco Russo", passwordEncoder.encode("sala1234"), Ruolo.SALA)));
        camerieri.add(staffRepository.save(new Staff("sara", "Sara Esposito", passwordEncoder.encode("sara1234"), Ruolo.SALA)));
        staffRepository.save(new Staff("cucina", "Antonio De Luca", passwordEncoder.encode("cucina1234"), Ruolo.CUCINA));
    }

    private void sala() {
        tavolo(1, 2, ZonaSala.SALA, FormaTavolo.ROTONDO, 10, 20);
        tavolo(2, 2, ZonaSala.SALA, FormaTavolo.ROTONDO, 10, 47);
        tavolo(3, 4, ZonaSala.SALA, FormaTavolo.QUADRATO, 27, 20);
        tavolo(4, 4, ZonaSala.SALA, FormaTavolo.QUADRATO, 27, 47);
        tavolo(5, 4, ZonaSala.SALA, FormaTavolo.QUADRATO, 44, 20);
        tavolo(6, 6, ZonaSala.SALA, FormaTavolo.RETTANGOLARE, 44, 47);
        tavolo(7, 2, ZonaSala.SALA, FormaTavolo.ROTONDO, 10, 76);
        tavolo(8, 4, ZonaSala.SALA, FormaTavolo.QUADRATO, 27, 76);
        tavolo(9, 4, ZonaSala.SALA, FormaTavolo.QUADRATO, 44, 76);
        tavolo(10, 2, ZonaSala.TERRAZZA, FormaTavolo.ROTONDO, 68, 18);
        tavolo(11, 2, ZonaSala.TERRAZZA, FormaTavolo.ROTONDO, 86, 18);
        tavolo(12, 4, ZonaSala.TERRAZZA, FormaTavolo.QUADRATO, 68, 44);
        tavolo(13, 4, ZonaSala.TERRAZZA, FormaTavolo.QUADRATO, 86, 44);
        tavolo(20, 10, ZonaSala.PRIVE, FormaTavolo.RETTANGOLARE, 77, 78);
    }

    private void tavolo(int numero, int posti, ZonaSala zona, FormaTavolo forma, int x, int y) {
        tavoli.put(numero, tavoloRepository.save(new Tavolo(numero, posti, zona, forma, x, y)));
    }

    // ------------------------------------------------------------------ magazzino

    private void magazzino() {
        ing("Ricciola", UnitaMisura.KG, "4.2", "2", "32", "Pescheria Mergellina");
        ing("Manzo marchigiano", UnitaMisura.KG, "3", "2", "38", "Macelleria Iovine");
        ing("Uova", UnitaMisura.PZ, "120", "60", "0.35", "Fattoria Le Starze");
        ing("Nocciole di Giffoni", UnitaMisura.KG, "1.2", "0.5", "24", "Coop. Giffoni");
        ing("Melanzane", UnitaMisura.KG, "6", "3", "2.80", "Orto Vesuviano");
        ing("Mozzarella di bufala", UnitaMisura.KG, "3.5", "2", "14", "Caseificio Barlotti");
        ing("Pomodoro del Piennolo", UnitaMisura.KG, "5", "3", "6.50", "Orto Vesuviano");
        ing("Basilico", UnitaMisura.KG, "0.4", "0.2", "18", "Orto Vesuviano");
        ing("Polpo", UnitaMisura.KG, "1.4", "3", "22", "Pescheria Mergellina");
        ing("Patate viola", UnitaMisura.KG, "4", "2", "3.20", "Orto Vesuviano");
        ing("Friarielli", UnitaMisura.KG, "2", "1.5", "5", "Orto Vesuviano");
        ing("Pasta di Gragnano", UnitaMisura.KG, "12", "5", "6", "Pastificio dei Campi");
        ing("Cipolla ramata di Montoro", UnitaMisura.KG, "9", "4", "2.50", "Orto Vesuviano");
        ing("Colatura di alici di Cetara", UnitaMisura.L, "0.8", "0.5", "60", "Delfino Battista");
        ing("Limoni di Amalfi", UnitaMisura.KG, "3", "2", "4.50", "Costiera Agrumi");
        ing("Riso Carnaroli", UnitaMisura.KG, "1.5", "3", "5.50", "Riseria Acquerello");
        ing("Gamberi rossi", UnitaMisura.KG, "1.2", "1", "58", "Pescheria Mergellina");
        ing("Vongole veraci", UnitaMisura.KG, "2", "2", "16", "Pescheria Mergellina");
        ing("Ricotta di bufala", UnitaMisura.KG, "2.5", "1.5", "12", "Caseificio Barlotti");
        ing("Burro", UnitaMisura.KG, "3", "1.5", "11", "Caseificio Barlotti");
        ing("Provola affumicata", UnitaMisura.KG, "2", "1", "13", "Caseificio Barlotti");
        ing("Agnello", UnitaMisura.KG, "4", "2", "26", "Macelleria Iovine");
        ing("Carciofi", UnitaMisura.PZ, "30", "20", "1.20", "Orto Vesuviano");
        ing("Guancia di vitello", UnitaMisura.KG, "3.5", "2", "19", "Macelleria Iovine");
        ing("Taurasi DOCG", UnitaMisura.L, "3", "2", "18", "Cantina Mastroberardino");
        ing("Scarola", UnitaMisura.KG, "3", "1.5", "3", "Orto Vesuviano");
        ing("Olive di Gaeta", UnitaMisura.KG, "1", "0.5", "15", "Frantoio Gaeta");
        ing("Farina 00", UnitaMisura.KG, "15", "8", "1.10", "Molino Caputo");
        ing("Zucchero", UnitaMisura.KG, "8", "4", "1.30", "Metro");
        ing("Rum agricolo", UnitaMisura.L, "1.2", "0.5", "25", "Metro");
        ing("Panna fresca", UnitaMisura.L, "4", "2", "5.50", "Caseificio Barlotti");
        ing("Amarene", UnitaMisura.KG, "0.3", "0.5", "20", "Fabbri");
        ing("Parmigiano Reggiano", UnitaMisura.KG, "2.5", "1", "24", "Caseificio Barlotti");
        ing("Olio extravergine", UnitaMisura.L, "12", "6", "12", "Frantoio Gaeta");
    }

    private void ing(String nome, UnitaMisura unita, String scorta, String soglia, String costo, String fornitore) {
        Ingrediente i = ingredienteRepository.save(new Ingrediente(nome, unita, scorta, soglia, costo, fornitore));
        ingredienti.put(nome, i);
        MovimentoMagazzino m = new MovimentoMagazzino(i, TipoMovimento.CARICO, i.getScorta(), "Inventario iniziale");
        m.setData(LocalDateTime.now().minusDays(GIORNI_STORICO + 1L));
        movimentoRepository.save(m);
    }

    // ------------------------------------------------------------------ menù

    private void menu() {
        Categoria antipasti = categoriaRepository.save(new Categoria("Per iniziare", "Crudi, mare e orto vesuviano", 1, 1));
        Categoria primi = categoriaRepository.save(new Categoria("Primi", "Pasta di Gragnano e risotti", 2, 2));
        Categoria secondi = categoriaRepository.save(new Categoria("Secondi", "Dal mare e dalla terra", 3, 2));
        Categoria dolci = categoriaRepository.save(new Categoria("Dolci", "La pasticceria napoletana, riletta", 4, 3));
        Categoria percorsi = categoriaRepository.save(new Categoria("Percorsi degustazione",
                "Sei portate scelte dallo chef, per tutto il tavolo", 5, 1));

        piatto(antipasti, "Crudo di ricciola, agrumi di Sorrento e finocchietto", "22",
                "Ricciola del golfo appena tagliata, emulsione di limone di Amalfi e olio al finocchietto selvatico.",
                "Falanghina dei Campi Flegrei DOC: sapida e agrumata come il piatto.",
                true, false, true, 12, Set.of(PESCE),
                r("Ricciola", "0.09"), r("Limoni di Amalfi", "0.03"), r("Olio extravergine", "0.01"));
        piatto(antipasti, "Tartare di manzo, tuorlo marinato e nocciole di Giffoni", "21",
                "Manzo marchigiano battuto al coltello, tuorlo marinato nella soia e nocciole tostate.",
                "Lacryma Christi del Vesuvio rosso: morbido, non copre il crudo.",
                false, false, false, 14, Set.of(UOVA, FRUTTA_A_GUSCIO, SOIA),
                r("Manzo marchigiano", "0.11"), r("Uova", "1"), r("Nocciole di Giffoni", "0.015"));
        piatto(antipasti, "Parmigiana di melanzane in forma nuova", "16",
                "Melanzane fritte e confit, pomodoro del Piennolo, fonduta di provola e basilico croccante.",
                "Piedirosso dei Campi Flegrei: fresco e leggermente speziato.",
                false, true, false, 15, Set.of(LATTE, GLUTINE, UOVA),
                r("Melanzane", "0.2"), r("Pomodoro del Piennolo", "0.08"), r("Provola affumicata", "0.05"),
                r("Basilico", "0.005"), r("Parmigiano Reggiano", "0.02"));
        piatto(antipasti, "Bufala affumicata, Piennolo e basilico", "15",
                "Mozzarella di bufala affumicata al legno di faggio con pomodorini del Piennolo arrostiti.",
                "Greco di Tufo DOCG: minerale, pulisce il palato dalla bufala.",
                false, true, false, 8, Set.of(LATTE),
                r("Mozzarella di bufala", "0.15"), r("Pomodoro del Piennolo", "0.08"), r("Basilico", "0.004"));
        Piatto polpo = piatto(antipasti, "Polpo arrosto, patata viola e friarielli", "20",
                "Polpo cotto a bassa temperatura e passato alla brace, crema di patata viola e salsa di friarielli.",
                "Fiano di Avellino DOCG: struttura e note di nocciola.",
                false, false, false, 16, Set.of(MOLLUSCHI),
                r("Polpo", "0.22"), r("Patate viola", "0.12"), r("Friarielli", "0.05"));
        polpo.setDisponibile(false);

        piatto(primi, "Genovese di Gragnano, cipolla ramata e lacrima di vitello", "19",
                "Ziti spezzati a mano nella genovese cotta otto ore, con fondo bruno e Parmigiano 30 mesi.",
                "Aglianico del Taburno: abbraccia la dolcezza della cipolla.",
                true, false, false, 14, Set.of(GLUTINE, SEDANO, LATTE),
                r("Pasta di Gragnano", "0.1"), r("Cipolla ramata di Montoro", "0.35"),
                r("Guancia di vitello", "0.05"), r("Parmigiano Reggiano", "0.015"));
        piatto(primi, "Linguine, colatura di Cetara e limone", "21",
                "Linguine mantecate con colatura di alici, scorza di limone di Amalfi e pangrattato alle erbe.",
                "Costa d'Amalfi Furore bianco: salino e profumato.",
                false, false, false, 12, Set.of(GLUTINE, PESCE),
                r("Pasta di Gragnano", "0.1"), r("Colatura di alici di Cetara", "0.01"), r("Limoni di Amalfi", "0.02"));
        piatto(primi, "Risotto alla pescatora, gamberi rossi e vongole", "26",
                "Carnaroli tostato, fumetto di crostacei, vongole veraci e gamberi rossi crudi a fine cottura.",
                "Falanghina del Sannio: fresca, esalta il mare.",
                false, false, false, 20, Set.of(CROSTACEI, MOLLUSCHI, PESCE, LATTE),
                r("Riso Carnaroli", "0.09"), r("Gamberi rossi", "0.06"), r("Vongole veraci", "0.12"), r("Burro", "0.015"));
        piatto(primi, "Ravioli di ricotta di bufala, burro nocciola e salvia", "18",
                "Pasta fresca all'uovo tirata a mano, ripieno di ricotta di bufala e limone, burro nocciola.",
                "Greco di Tufo DOCG: accompagna la grassezza del burro.",
                false, true, false, 13, Set.of(GLUTINE, LATTE, UOVA),
                r("Farina 00", "0.08"), r("Uova", "1"), r("Ricotta di bufala", "0.09"), r("Burro", "0.02"));
        piatto(primi, "Mezzi paccheri, Piennolo e provola", "17",
                "Mezzi paccheri con pomodorino del Piennolo appena saltato e provola affumicata filante.",
                "Piedirosso Pompeiano: rosso leggero da servire fresco.",
                false, true, false, 11, Set.of(GLUTINE, LATTE),
                r("Pasta di Gragnano", "0.1"), r("Pomodoro del Piennolo", "0.12"), r("Provola affumicata", "0.05"));

        piatto(secondi, "Ricciola scottata, scarola alla napoletana e olive di Gaeta", "32",
                "Trancio di ricciola scottato sulla pelle, scarola con capperi e olive di Gaeta, salsa al limone.",
                "Fiano di Avellino DOCG: aromatico, regge la scarola.",
                false, false, false, 18, Set.of(PESCE),
                r("Ricciola", "0.18"), r("Scarola", "0.15"), r("Olive di Gaeta", "0.02"));
        piatto(secondi, "Agnello in crosta di erbe, carciofi e mentuccia", "34",
                "Carré di agnello in crosta di erbe e pane, carciofi in due consistenze e jus alla mentuccia.",
                "Aglianico del Vulture: tannico e profondo.",
                false, false, false, 22, Set.of(GLUTINE),
                r("Agnello", "0.24"), r("Carciofi", "2"), r("Farina 00", "0.01"));
        piatto(secondi, "Guancia di vitello al Taurasi, purè affumicato", "30",
                "Guancia brasata dodici ore nel Taurasi, purè di patate affumicato al fieno.",
                "Taurasi DOCG: lo stesso vino della cottura, al calice.",
                true, false, false, 10, Set.of(LATTE, SEDANO, SOLFITI),
                r("Guancia di vitello", "0.22"), r("Taurasi DOCG", "0.08"), r("Patate viola", "0.12"), r("Burro", "0.02"));
        piatto(secondi, "Melanzana in tre consistenze", "24",
                "Melanzana arrostita, in crema e in chips croccanti con pomodoro del Piennolo e basilico.",
                "Lacryma Christi del Vesuvio rosato: fresco e fruttato.",
                false, true, true, 16, Set.of(),
                r("Melanzane", "0.3"), r("Pomodoro del Piennolo", "0.08"), r("Olio extravergine", "0.02"));

        piatto(dolci, "Babà al rum agricolo, chantilly e amarene", "12",
                "Babà lievitato 24 ore, bagna al rum agricolo, crema chantilly e amarene sciroppate.",
                "Passito di Falanghina: dolce ma fresco.",
                true, false, false, 6, Set.of(GLUTINE, UOVA, LATTE),
                r("Farina 00", "0.04"), r("Uova", "1"), r("Rum agricolo", "0.02"), r("Panna fresca", "0.04"),
                r("Amarene", "0.02"));
        piatto(dolci, "Sfogliatella scomposta, ricotta e canditi", "11",
                "Sfoglia croccante alla cannella, crema di ricotta di bufala, arancia candita e semola.",
                "Moscato di Pantelleria: note d'arancia e miele.",
                false, true, false, 7, Set.of(GLUTINE, LATTE, UOVA),
                r("Farina 00", "0.05"), r("Ricotta di bufala", "0.06"), r("Zucchero", "0.02"));
        piatto(dolci, "Delizia al limone di Amalfi", "12",
                "Pan di Spagna, crema al limone di Amalfi e glassa morbida, servita fredda.",
                "Limoncello della costiera, appena uscito dal freezer.",
                false, true, false, 6, Set.of(GLUTINE, UOVA, LATTE, FRUTTA_A_GUSCIO),
                r("Limoni di Amalfi", "0.05"), r("Uova", "1"), r("Panna fresca", "0.05"), r("Zucchero", "0.03"));
        piatto(dolci, "Sorbetto al limone e basilico", "8",
                "Sorbetto mantecato al momento con limoni di Amalfi e basilico fresco.",
                null, false, true, true, 3, Set.of(),
                r("Limoni di Amalfi", "0.06"), r("Zucchero", "0.03"), r("Basilico", "0.002"));

        piatto(percorsi, "Percorso \"Mare di Napoli\"", "95",
                "Crudo di ricciola, linguine alla colatura, risotto alla pescatora, ricciola scottata e due dolci.",
                "Abbinamento vini dedicato: +45 € a persona.",
                true, false, false, 25, Set.of(PESCE, CROSTACEI, MOLLUSCHI, GLUTINE, LATTE, UOVA),
                r("Ricciola", "0.2"), r("Gamberi rossi", "0.04"), r("Pasta di Gragnano", "0.06"));
        piatto(percorsi, "Percorso \"Terra campana\"", "88",
                "Parmigiana in forma nuova, genovese, ravioli di bufala, guancia al Taurasi e babà.",
                "Abbinamento vini dedicato: +40 € a persona.",
                false, false, false, 25, Set.of(GLUTINE, LATTE, UOVA, SEDANO, SOLFITI),
                r("Melanzane", "0.1"), r("Pasta di Gragnano", "0.06"), r("Guancia di vitello", "0.12"));
    }

    private record Dose(String ingrediente, String quantita) {
    }

    private Dose r(String ingrediente, String quantita) {
        return new Dose(ingrediente, quantita);
    }

    private Piatto piatto(Categoria categoria, String nome, String prezzo, String descrizione, String abbinamento,
                          boolean firma, boolean vegetariano, boolean vegano, int minuti, Set<Allergene> allergeni,
                          Dose... ricetta) {
        Piatto p = new Piatto();
        p.setCategoria(categoria);
        p.setNome(nome);
        p.setPrezzo(new BigDecimal(prezzo));
        p.setDescrizione(descrizione);
        p.setAbbinamento(abbinamento);
        p.setInEvidenza(firma);
        p.setVegetariano(vegetariano || vegano);
        p.setVegano(vegano);
        p.setTempoPreparazione(minuti);
        p.getAllergeni().addAll(allergeni);
        p.setPosizione((int) piatti.values().stream().filter(x -> x.getCategoria() == categoria).count() + 1);
        for (Dose d : ricetta) {
            p.aggiungiIngrediente(ingredienti.get(d.ingrediente()), new BigDecimal(d.quantita()));
        }
        piatti.put(nome, piattoRepository.save(p));
        return p;
    }

    // ------------------------------------------------------------------ storico dei conti

    /** Un mese di servizi realistici: più gente nel weekend, cena più piena del pranzo. */
    private void storico() {
        List<Piatto> antipasti = perCategoria("Per iniziare");
        List<Piatto> primi = perCategoria("Primi");
        List<Piatto> secondi = perCategoria("Secondi");
        List<Piatto> dolci = perCategoria("Dolci");
        List<Piatto> percorsi = perCategoria("Percorsi degustazione");
        List<Tavolo> elenco = new ArrayList<>(tavoli.values());
        LocalDate oggi = LocalDate.now();
        int conti = 0;

        for (int g = GIORNI_STORICO; g >= 0; g--) {
            LocalDate giorno = oggi.minusDays(g);
            if (giorno.getDayOfWeek() == DayOfWeek.MONDAY) {
                continue;
            }
            boolean weekend = giorno.getDayOfWeek().getValue() >= 5;
            int pranzo = (weekend ? 7 : 3) + random.nextInt(4);
            int cena = (weekend ? 12 : 8) + random.nextInt(4);
            for (int i = 0; i < pranzo + cena; i++) {
                boolean aPranzo = i < pranzo;
                LocalTime arrivo = aPranzo
                        ? LocalTime.of(12, 30).plusMinutes(random.nextInt(100))
                        : LocalTime.of(19, 30).plusMinutes(random.nextInt(150));
                Tavolo tavolo = elenco.get(random.nextInt(elenco.size()));
                int coperti = Math.max(1, Math.min(tavolo.getPosti(), 2 + random.nextInt(3) - (random.nextInt(4) == 0 ? 1 : 0)));

                Ordine o = new Ordine();
                o.setTavolo(tavolo);
                o.setCoperti(coperti);
                o.setCopertoUnitario(properties.coperto());
                o.setCameriere(camerieri.get(random.nextInt(camerieri.size())));
                o.setApertoIl(LocalDateTime.of(giorno, arrivo));
                LocalDateTime inviato = o.getApertoIl().plusMinutes(8);

                if (!aPranzo && random.nextInt(7) == 0) {
                    riga(o, scegli(percorsi), coperti, inviato);
                } else {
                    for (int c = 0; c < coperti; c++) {
                        if (random.nextInt(100) < 65) riga(o, scegli(antipasti), 1, inviato);
                        if (random.nextInt(100) < 80) riga(o, scegli(primi), 1, inviato.plusMinutes(20));
                        if (random.nextInt(100) < (aPranzo ? 35 : 55)) riga(o, scegli(secondi), 1, inviato.plusMinutes(35));
                        if (random.nextInt(100) < 50) riga(o, scegli(dolci), 1, inviato.plusMinutes(60));
                    }
                }
                if (o.getRighe().isEmpty()) {
                    riga(o, scegli(primi), coperti, inviato);
                }
                o.setScontoPercentuale(random.nextInt(25) == 0 ? 10 : 0);
                MetodoPagamento[] metodi = {MetodoPagamento.CARTA, MetodoPagamento.CARTA, MetodoPagamento.CARTA,
                        MetodoPagamento.CONTANTI, MetodoPagamento.SATISPAY, MetodoPagamento.BUONO};
                o.setMetodoPagamento(metodi[random.nextInt(metodi.length)]);
                o.setStato(StatoOrdine.CHIUSO);
                o.setChiusoIl(o.getApertoIl().plusMinutes(75 + random.nextInt(60)));
                o.setTotale(o.getTotaleCorrente());
                if (o.getChiusoIl().isAfter(LocalDateTime.now())) {
                    continue; // oggi: solo i conti già chiusi a quest'ora
                }
                ordineRepository.save(o);
                conti++;
            }
        }

        // qualche spreco e carico registrato in magazzino
        spreco("Basilico", "0.05", 6, "Appassito");
        spreco("Ricciola", "0.3", 4, "Rifilature non utilizzabili");
        spreco("Panna fresca", "0.5", 2, "Scaduta");
        carico("Ricciola", "3", 3, "Fattura 1184 Pescheria Mergellina");
        carico("Mozzarella di bufala", "2", 1, "Consegna giornaliera");
        log.info("Creati {} conti degli ultimi {} giorni", conti, GIORNI_STORICO);
    }

    private void spreco(String nome, String quantita, int giorniFa, String nota) {
        Ingrediente i = ingredienti.get(nome);
        BigDecimal q = new BigDecimal(quantita);
        i.setScorta(i.getScorta().subtract(q));
        MovimentoMagazzino m = new MovimentoMagazzino(i, TipoMovimento.SPRECO, q.negate(), nota);
        m.setData(LocalDateTime.now().minusDays(giorniFa).withHour(23).withMinute(10));
        movimentoRepository.save(m);
    }

    private void carico(String nome, String quantita, int giorniFa, String nota) {
        Ingrediente i = ingredienti.get(nome);
        BigDecimal q = new BigDecimal(quantita);
        i.setScorta(i.getScorta().add(q));
        MovimentoMagazzino m = new MovimentoMagazzino(i, TipoMovimento.CARICO, q, nota);
        m.setData(LocalDateTime.now().minusDays(giorniFa).withHour(9).withMinute(30));
        movimentoRepository.save(m);
    }

    private List<Piatto> perCategoria(String categoria) {
        return piatti.values().stream().filter(p -> p.getCategoria().getNome().equals(categoria)).toList();
    }

    private Piatto scegli(List<Piatto> lista) {
        // i primi piatti di ogni categoria (i più noti) escono più spesso
        int i = (int) Math.floor(Math.pow(random.nextDouble(), 1.6) * lista.size());
        return lista.get(Math.min(i, lista.size() - 1));
    }

    private RigaOrdine riga(Ordine o, Piatto p, int quantita, LocalDateTime inviato) {
        for (RigaOrdine esistente : o.getRighe()) {
            if (esistente.getPiatto() == p && esistente.getStato() == StatoRiga.SERVITO) {
                esistente.setQuantita(esistente.getQuantita() + quantita);
                return esistente;
            }
        }
        RigaOrdine r = new RigaOrdine();
        r.setPiatto(p);
        r.setQuantita(quantita);
        r.setPrezzoUnitario(p.getPrezzo());
        r.setUscita(p.getCategoria().getUscita());
        r.setStato(StatoRiga.SERVITO);
        r.setInviatoIl(inviato);
        r.setProntoIl(inviato.plusMinutes(p.getTempoPreparazione()));
        o.aggiungiRiga(r);
        return r;
    }

    // ------------------------------------------------------------------ servizio in corso

    /** Alcuni tavoli occupati in questo momento, con piatti in vari stati, per vedere sala e cucina "dal vivo". */
    private void servizioInCorso() {
        LocalDateTime adesso = LocalDateTime.now();

        Ordine t3 = aperto(3, 4, 55, camerieri.get(0));
        live(t3, "Crudo di ricciola, agrumi di Sorrento e finocchietto", 2, StatoRiga.SERVITO, adesso.minusMinutes(45), null);
        live(t3, "Parmigiana di melanzane in forma nuova", 2, StatoRiga.SERVITO, adesso.minusMinutes(45), null);
        live(t3, "Genovese di Gragnano, cipolla ramata e lacrima di vitello", 2, StatoRiga.IN_PREPARAZIONE, adesso.minusMinutes(14), null);
        live(t3, "Risotto alla pescatora, gamberi rossi e vongole", 2, StatoRiga.INVIATO, adesso.minusMinutes(14), "Uno senza vongole");
        ordineRepository.save(t3);

        Ordine t6 = aperto(6, 6, 30, camerieri.get(1));
        live(t6, "Tartare di manzo, tuorlo marinato e nocciole di Giffoni", 3, StatoRiga.PRONTO, adesso.minusMinutes(22), null);
        live(t6, "Bufala affumicata, Piennolo e basilico", 3, StatoRiga.IN_PREPARAZIONE, adesso.minusMinutes(22), null);
        live(t6, "Linguine, colatura di Cetara e limone", 4, StatoRiga.INVIATO, adesso.minusMinutes(22), null);
        live(t6, "Ravioli di ricotta di bufala, burro nocciola e salvia", 2, StatoRiga.INVIATO, adesso.minusMinutes(22), "Senza salvia");
        ordineRepository.save(t6);

        Ordine t10 = aperto(10, 2, 95, camerieri.get(0));
        live(t10, "Percorso \"Mare di Napoli\"", 2, StatoRiga.SERVITO, adesso.minusMinutes(85), null);
        live(t10, "Babà al rum agricolo, chantilly e amarene", 1, StatoRiga.SERVITO, adesso.minusMinutes(20), null);
        t10.setStato(StatoOrdine.CONTO);
        ordineRepository.save(t10);

        Ordine t12 = aperto(12, 3, 12, camerieri.get(1));
        live(t12, "Crudo di ricciola, agrumi di Sorrento e finocchietto", 1, StatoRiga.INVIATO, adesso.minusMinutes(4), null);
        live(t12, "Mezzi paccheri, Piennolo e provola", 2, StatoRiga.IN_ATTESA, null, null);
        live(t12, "Guancia di vitello al Taurasi, purè affumicato", 1, StatoRiga.IN_ATTESA, null, "Cottura media");
        ordineRepository.save(t12);

        Ordine t1 = aperto(1, 2, 40, camerieri.get(0));
        live(t1, "Agnello in crosta di erbe, carciofi e mentuccia", 1, StatoRiga.IN_PREPARAZIONE, adesso.minusMinutes(9), null);
        live(t1, "Ricciola scottata, scarola alla napoletana e olive di Gaeta", 1, StatoRiga.IN_PREPARAZIONE, adesso.minusMinutes(9), null);
        live(t1, "Linguine, colatura di Cetara e limone", 2, StatoRiga.SERVITO, adesso.minusMinutes(30), null);
        ordineRepository.save(t1);
    }

    private Ordine aperto(int tavolo, int coperti, int minutiFa, Staff cameriere) {
        Ordine o = new Ordine();
        o.setTavolo(tavoli.get(tavolo));
        o.setCoperti(coperti);
        o.setCopertoUnitario(properties.coperto());
        o.setCameriere(cameriere);
        o.setApertoIl(LocalDateTime.now().minusMinutes(minutiFa));
        return o;
    }

    private void live(Ordine o, String piatto, int quantita, StatoRiga stato, LocalDateTime inviato, String nota) {
        Piatto p = piatti.get(piatto);
        RigaOrdine r = new RigaOrdine();
        r.setPiatto(p);
        r.setQuantita(quantita);
        r.setPrezzoUnitario(p.getPrezzo());
        r.setUscita(p.getCategoria().getUscita());
        r.setStato(stato);
        r.setInviatoIl(inviato);
        r.setNote(nota);
        if (stato == StatoRiga.PRONTO || stato == StatoRiga.SERVITO) {
            r.setProntoIl(LocalDateTime.now().minusMinutes(3));
        }
        o.aggiungiRiga(r);
    }

    // ------------------------------------------------------------------ prenotazioni

    private void prenotazioni() {
        LocalDate oggi = LocalDate.now();
        prenota(oggi, "13:00", 2, "Famiglia Conte", "333 481 2290", StatoPrenotazione.COMPLETATA, null, null);
        prenota(oggi, "20:00", 2, "Elena Marino", "347 220 1188", StatoPrenotazione.CONFERMATA, 2, "Anniversario: una candela sul dolce");
        prenota(oggi, "20:30", 4, "Luca Ferraro", "320 556 7741", StatoPrenotazione.CONFERMATA, 4, null);
        prenota(oggi, "20:30", 2, "Anna Romano", "339 118 4420", StatoPrenotazione.CONFERMATA, 11, "Intolleranza al lattosio");
        prenota(oggi, "21:00", 8, "Studio Notarile Greco", "081 552 3344", StatoPrenotazione.RICHIESTA, null, "Cena aziendale, vorrebbero il privé");
        prenota(oggi, "21:30", 4, "Paolo Bianchi", "348 902 1133", StatoPrenotazione.CONFERMATA, 13, null);
        prenota(oggi, "13:30", 3, "Giorgio Galli", "331 774 0921", StatoPrenotazione.NO_SHOW, null, null);

        String[] nomi = {"Martina Ricci", "Davide Colombo", "Federica Bruno", "Alessandro Gallo", "Chiara Fontana",
                "Simone Caruso", "Valentina Moretti", "Roberto Leone", "Ilaria De Rosa", "Francesco Longo",
                "Laura Santoro", "Matteo Marini", "Giorgia Rinaldi", "Stefano Villa", "Sofia Mancini"};
        List<LocalTime> orari = List.of(LocalTime.of(13, 0), LocalTime.of(20, 0), LocalTime.of(20, 30),
                LocalTime.of(21, 0), LocalTime.of(21, 30), LocalTime.of(13, 30));
        for (int g = 1; g <= 14; g++) {
            LocalDate giorno = oggi.plusDays(g);
            if (giorno.getDayOfWeek() == DayOfWeek.MONDAY) {
                continue;
            }
            int quante = (giorno.getDayOfWeek().getValue() >= 5 ? 6 : 2) + random.nextInt(4);
            for (int i = 0; i < quante; i++) {
                LocalTime ora = orari.get(random.nextInt(orari.size()));
                int persone = 2 + random.nextInt(4);
                StatoPrenotazione stato = random.nextInt(8) == 0 ? StatoPrenotazione.RICHIESTA : StatoPrenotazione.CONFERMATA;
                prenota(giorno, ora.toString(), persone, nomi[random.nextInt(nomi.length)],
                        "3" + (30 + random.nextInt(60)) + " " + (100 + random.nextInt(900)) + " " + (1000 + random.nextInt(9000)),
                        stato, null, null);
            }
        }
    }

    private String codice() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String codice;
        do {
            StringBuilder sb = new StringBuilder("LM-");
            for (int i = 0; i < 5; i++) {
                sb.append(alfabeto.charAt(random.nextInt(alfabeto.length())));
            }
            codice = sb.toString();
        } while (!codici.add(codice));
        return codice;
    }

    private void prenota(LocalDate data, String ora, int persone, String nome, String telefono,
                         StatoPrenotazione stato, Integer tavolo, String note) {
        Prenotazione p = new Prenotazione();
        p.setCodice(codice());
        p.setNome(nome);
        p.setTelefono(telefono);
        p.setData(data);
        p.setOra(LocalTime.parse(ora));
        p.setPersone(persone);
        p.setStato(stato);
        p.setOrigine(random.nextBoolean() ? Origine.ONLINE : Origine.TELEFONO);
        p.setNote(note);
        p.setTavolo(tavolo == null ? null : tavoli.get(tavolo));
        p.setCreataIl(LocalDateTime.now().minusDays(random.nextInt(10) + 1L));
        prenotazioneRepository.save(p);
    }
}
