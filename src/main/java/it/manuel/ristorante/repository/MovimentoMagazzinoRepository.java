package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentoMagazzinoRepository extends JpaRepository<MovimentoMagazzino, Long> {

    List<MovimentoMagazzino> findTop40ByOrderByDataDesc();

    List<MovimentoMagazzino> findByTipoAndDataBetween(TipoMovimento tipo, LocalDateTime da, LocalDateTime a);

    boolean existsByIngredienteId(Long ingredienteId);
}
