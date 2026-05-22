package com.loja.sd.loja_backend.servico;

import com.loja.sd.loja_backend.modelos.Papel;
import com.loja.sd.loja_backend.modelos.UtilizadorLocal;
import com.loja.sd.loja_backend.repositorio.UtilizadorLocalRepositorio;
import com.loja.sd.loja_backend.view.modelos.Registro;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UtilizadorServico implements UserDetailsService {

    private final UtilizadorLocalRepositorio utilizadorRepositorio;
    private final PasswordEncoder passwordEncoder;

    public UtilizadorServico(UtilizadorLocalRepositorio utilizadorRepositorio, PasswordEncoder passwordEncoder) {
        this.utilizadorRepositorio = utilizadorRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    public UtilizadorLocal registrar(Registro registro) {
        if (utilizadorRepositorio.existsByUtilizador(registro.getUtilizador())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador ja existe");
        }
        if (utilizadorRepositorio.existsByEmail(registro.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ja existe");
        }

        UtilizadorLocal utilizador = new UtilizadorLocal();
        utilizador.setNome(registro.getNome());
        utilizador.setEmail(registro.getEmail());
        utilizador.setSobrenome(registro.getSobrenome());
        utilizador.setPassword(passwordEncoder.encode(registro.getPassword()));
        utilizador.setUtilizador(registro.getUtilizador());
        utilizador.setPapel(Papel.CLIENTE);

        return utilizadorRepositorio.save(utilizador);
    }

    public UtilizadorLocal buscarPorUtilizador(String utilizador) {
        return utilizadorRepositorio.findByUtilizador(utilizador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador nao encontrado"));
    }

    public UtilizadorLocal atualizarPerfil(String utilizadorAtual, String email, String nome, String sobrenome) {
        UtilizadorLocal utilizador = buscarPorUtilizador(utilizadorAtual);
        utilizador.setNome(nome);
        utilizador.setSobrenome(sobrenome);
        utilizador.setEmail(email);

        return utilizadorRepositorio.save(utilizador);
    }

    @Override
    public UserDetails loadUserByUsername(String utilizador) throws UsernameNotFoundException {
        UtilizadorLocal utilizadorLocal = utilizadorRepositorio.findByUtilizador(utilizador)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador nao encontrado"));

        Papel papel = utilizadorLocal.getPapel() == null ? Papel.CLIENTE : utilizadorLocal.getPapel();

        return User.withUsername(utilizadorLocal.getUtilizador())
                .password(utilizadorLocal.getPassword())
                .roles(papel.name())
                .build();
    }
}
