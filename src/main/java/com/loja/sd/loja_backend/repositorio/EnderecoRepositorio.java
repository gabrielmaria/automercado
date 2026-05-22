package com.loja.sd.loja_backend.repositorio;

import com.loja.sd.loja_backend.modelos.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnderecoRepositorio extends JpaRepository<Endereco, Long> {
    List<Endereco> findByUtilizadorLocalId(Long utilizadorLocalId);
}
