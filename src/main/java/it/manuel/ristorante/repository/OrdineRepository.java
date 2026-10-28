package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdineRepository extends JpaRepository<Ordine, Long> {

    List<Ordine> findByStatoInOrderByApertoIlAsc(Collection<StatoOrdine> stati);

    Optional<Ordine> findFirstByTavoloIdAndStatoIn(Long tavoloId, Collection<StatoOrdine> stati);

    /** Carica anche i piatti di ogni conto in un'unica query (servono per le statistiche). */
    @EntityGraph(attributePaths = "righe")
    List<Ordine> findByStatoAndChiusoIlBetween(StatoOrdine stato, LocalDateTime da, LocalDateTime a);

    List<Ordine> findTop8ByStatoOrderByChiusoIlDesc(StatoOrdine stato);

    boolean existsByTavoloId(Long tavoloId);
}
