package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.UtilizadorLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilizadorLocalRepositorio extends JpaRepository<UtilizadorLocal, Long> {
    Optional<UtilizadorLocal> findByUtilizador(String utilizador);

    boolean existsByUtilizador(String utilizador);

    boolean existsByEmail(String email);
}
