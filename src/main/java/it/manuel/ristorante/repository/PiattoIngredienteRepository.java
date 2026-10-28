package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.PiattoIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PiattoIngredienteRepository extends JpaRepository<PiattoIngrediente, Long> {

    boolean existsByIngredienteId(Long ingredienteId);
}
