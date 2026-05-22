package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.Produto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepositorio extends JpaRepository<Produto, Long> {
    Optional<Produto> findByNome(String nome);

    @Query("select p.id, p.nome, coalesce(sum(q.quantidade), 0) from Produto p left join p.quantidadeEncomendas q group by p.id, p.nome order by coalesce(sum(q.quantidade), 0) desc")
    List<Object[]> produtosMaisVendidos(Pageable pageable);

    @Query("select p.id, p.nome, coalesce(sum(q.quantidade), 0) from Produto p left join p.quantidadeEncomendas q group by p.id, p.nome order by coalesce(sum(q.quantidade), 0) asc")
    List<Object[]> produtosMenosVendidos(Pageable pageable);
}
