package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.Encomenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EncomendaRepositorio extends JpaRepository<Encomenda, Long> {
    List<Encomenda> findByUtilizadorLocalId(Long utilizadorLocalId);

    @Query("select u.id, u.utilizador, u.nome, u.sobrenome, coalesce(sum(e.valorTotal), 0) from Encomenda e join e.utilizadorLocal u group by u.id, u.utilizador, u.nome, u.sobrenome order by coalesce(sum(e.valorTotal), 0) desc")
    List<Object[]> melhoresClientes(Pageable pageable);

    @Query("select coalesce(sum(e.valorTotal), 0) from Encomenda e where e.data >= :inicio and e.data < :fim")
    Double valorFaturadoEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
