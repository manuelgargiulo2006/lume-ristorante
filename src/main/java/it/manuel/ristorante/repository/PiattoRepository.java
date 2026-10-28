package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PiattoRepository extends JpaRepository<Piatto, Long> {

    List<Piatto> findAllByOrderByCategoriaPosizioneAscPosizioneAscNomeAsc();

    List<Piatto> findByInEvidenzaTrueAndDisponibileTrueOrderByCategoriaPosizioneAscPosizioneAsc();

    long countByCategoriaId(Long categoriaId);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    long countByDisponibileFalse();
}
