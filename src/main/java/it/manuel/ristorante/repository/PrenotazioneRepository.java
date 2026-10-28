package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    List<Prenotazione> findByDataOrderByOraAsc(LocalDate data);

    List<Prenotazione> findByDataBetweenOrderByDataAscOraAsc(LocalDate da, LocalDate a);

    Optional<Prenotazione> findByCodiceIgnoreCase(String codice);

    boolean existsByCodice(String codice);

    List<Prenotazione> findByDataAndStatoIn(LocalDate data, Collection<StatoPrenotazione> stati);

    long countByStato(StatoPrenotazione stato);
}
