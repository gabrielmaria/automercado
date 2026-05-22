package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByNome(String nome);
}
