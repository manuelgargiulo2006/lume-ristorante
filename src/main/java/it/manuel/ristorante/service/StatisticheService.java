package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.PiattoVenduto;
import it.manuel.ristorante.dto.PuntoSerie;
import it.manuel.ristorante.dto.Riepilogo;
import it.manuel.ristorante.model.*;
import it.manuel.ristorante.repository.MovimentoMagazzinoRepository;
import it.manuel.ristorante.repository.OrdineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Statistiche di vendita calcolate dai conti chiusi. */
@Service
public class StatisticheService {

    private static final DateTimeFormatter GIORNO = DateTimeFormatter.ofPattern("EEE d", Locale.ITALY);

    private final OrdineRepository ordineRepository;
    private final MovimentoMagazzinoRepository movimentoRepository;

    /** Conti chiusi tra due date (estremi inclusi). */
    @Transactional(readOnly = true)
    public List<Ordine> chiusi(LocalDate da, LocalDate a) {
        return ordineRepository.findByStatoAndChiusoIlBetween(StatoOrdine.CHIUSO,
                da.atStartOfDay(), a.plusDays(1).atStartOfDay().minusNanos(1));
    }

    @Transactional(readOnly = true)
    public Riepilogo riepilogo(LocalDate da, LocalDate a) {
        List<Ordine> ordini = chiusi(da, a);
        long giorni = java.time.temporal.ChronoUnit.DAYS.between(da, a) + 1;
        BigDecimal precedente = incasso(chiusi(da.minusDays(giorni), da.minusDays(1)));
        return riepilogo(ordini, precedente);
    }

    /** Oggi fino a questo momento, confrontato con ieri alla stessa ora. */
    @Transactional(readOnly = true)
    public Riepilogo riepilogoOggi() {
        LocalDate oggi = LocalDate.now();
        java.time.LocalDateTime ieriAdesso = java.time.LocalDateTime.now().minusDays(1);
        List<Ordine> ieri = chiusi(oggi.minusDays(1), oggi.minusDays(1)).stream()
                .filter(o -> !o.getChiusoIl().isAfter(ieriAdesso))
                .toList();
        List<Ordine> diOggi = chiusi(oggi, oggi);
        return riepilogo(diOggi, diOggi.isEmpty() ? null : incasso(ieri));
    }

    private Riepilogo riepilogo(List<Ordine> ordini, BigDecimal precedente) {
        BigDecimal incasso = incasso(ordini);
        int conti = ordini.size();
        int coperti = ordini.stream().mapToInt(Ordine::getCoperti).sum();
        BigDecimal scontrino = conti == 0 ? BigDecimal.ZERO
                : incasso.divide(BigDecimal.valueOf(conti), 2, RoundingMode.HALF_UP);
        BigDecimal perCoperto = coperti == 0 ? BigDecimal.ZERO
                : incasso.divide(BigDecimal.valueOf(coperti), 2, RoundingMode.HALF_UP);
        Integer variazione = precedente == null || precedente.signum() == 0 ? null
                : incasso.subtract(precedente).multiply(BigDecimal.valueOf(100))
                        .divide(precedente, 0, RoundingMode.HALF_UP).intValue();
        return new Riepilogo(incasso, conti, coperti, scontrino, perCoperto, variazione);
    }

    private BigDecimal incasso(List<Ordine> ordini) {
        return ordini.stream().map(Ordine::getTotale).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Incasso giorno per giorno. */
    @Transactional(readOnly = true)
    public List<PuntoSerie> incassoGiornaliero(LocalDate da, LocalDate a) {
        Map<LocalDate, BigDecimal> perGiorno = new LinkedHashMap<>();
        for (LocalDate d = da; !d.isAfter(a); d = d.plusDays(1)) {
            perGiorno.put(d, BigDecimal.ZERO);
        }
        for (Ordine o : chiusi(da, a)) {
            perGiorno.merge(o.getChiusoIl().toLocalDate(), o.getTotale(), BigDecimal::add);
        }
        return perGiorno.entrySet().stream()
                .map(e -> new PuntoSerie(GIORNO.format(e.getKey()), e.getValue()))
                .toList();
    }

    /** Coperti per ora di arrivo: mostra le ore di punta. */
    @Transactional(readOnly = true)
    public List<PuntoSerie> affluenzaPerOra(LocalDate da, LocalDate a) {
        Map<Integer, Integer> perOra = new TreeMap<>();
        for (int h = 12; h <= 23; h++) {
            perOra.put(h, 0);
        }
        for (Ordine o : chiusi(da, a)) {
            perOra.merge(o.getApertoIl().getHour(), o.getCoperti(), Integer::sum);
        }
        return perOra.entrySet().stream()
                .map(e -> new PuntoSerie(e.getKey() + ":00", BigDecimal.valueOf(e.getValue())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PiattoVenduto> piattiPiuVenduti(LocalDate da, LocalDate a, int quanti) {
        Map<Piatto, int[]> porzioni = new HashMap<>();
        Map<Piatto, BigDecimal> incassi = new HashMap<>();
        for (Ordine o : chiusi(da, a)) {
            for (RigaOrdine r : o.getRighe()) {
                porzioni.computeIfAbsent(r.getPiatto(), p -> new int[1])[0] += r.getQuantita();
                incassi.merge(r.getPiatto(), r.getImporto(), BigDecimal::add);
            }
        }
        int massimo = porzioni.values().stream().mapToInt(v -> v[0]).max().orElse(1);
        return porzioni.entrySet().stream()
                .sorted((x, y) -> Integer.compare(y.getValue()[0], x.getValue()[0]))
                .limit(quanti)
                .map(e -> new PiattoVenduto(e.getKey().getNome(), e.getKey().getCategoria().getNome(),
                        e.getValue()[0], incassi.get(e.getKey()), e.getValue()[0] * 100 / massimo))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PuntoSerie> incassoPerCategoria(LocalDate da, LocalDate a) {
        Map<String, BigDecimal> perCategoria = new LinkedHashMap<>();
        for (Ordine o : chiusi(da, a)) {
            for (RigaOrdine r : o.getRighe()) {
                perCategoria.merge(r.getPiatto().getCategoria().getNome(), r.getImporto(), BigDecimal::add);
            }
        }
        return perCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new PuntoSerie(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> metodiPagamento(LocalDate da, LocalDate a) {
        return chiusi(da, a).stream()
                .filter(o -> o.getMetodoPagamento() != null)
                .collect(Collectors.groupingBy(o -> o.getMetodoPagamento().getLabel(), TreeMap::new, Collectors.counting()));
    }

    /** Valore degli sprechi registrati in magazzino nel periodo. */
    @Transactional(readOnly = true)
    public BigDecimal valoreSprechi(LocalDate da, LocalDate a) {
        LocalDateTime inizio = da.atStartOfDay();
        LocalDateTime fine = a.plusDays(1).atStartOfDay();
        return movimentoRepository.findByTipoAndDataBetween(TipoMovimento.SPRECO, inizio, fine).stream()
                .map(m -> m.getQuantita().abs().multiply(m.getIngrediente().getCostoUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public StatisticheService(OrdineRepository ordineRepository, MovimentoMagazzinoRepository movimentoRepository) {
        this.ordineRepository = ordineRepository;
        this.movimentoRepository = movimentoRepository;
    }
}
