package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TavoloRepository extends JpaRepository<Tavolo, Long> {

    List<Tavolo> findAllByOrderByNumeroAsc();

    List<Tavolo> findByAttivoTrueOrderByNumeroAsc();

    boolean existsByNumero(int numero);

    boolean existsByNumeroAndIdNot(int numero, Long id);
}
