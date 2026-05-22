package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventarioRepositorio extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByProdutoId(Long produtoId);
}
