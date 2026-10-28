package it.manuel.ristorante.repository;

import it.manuel.ristorante.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RigaOrdineRepository extends JpaRepository<RigaOrdine, Long> {

    List<RigaOrdine> findByStatoInOrderByInviatoIlAsc(Collection<StatoRiga> stati);

    boolean existsByPiattoId(Long piattoId);
}
