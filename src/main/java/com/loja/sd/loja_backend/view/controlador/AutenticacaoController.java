package com.loja.sd.loja_backend.view.controlador;

import com.loja.sd.loja_backend.modelos.UtilizadorLocal;
import com.loja.sd.loja_backend.servico.UtilizadorServico;
import com.loja.sd.loja_backend.view.modelos.Registo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/autenticacao")
public class AutenticacaoController {

    private final UtilizadorServico utilizadorServico;
    private final AuthenticationManager authenticationManager;

    public AutenticacaoController(UtilizadorServico utilizadorServico, AuthenticationManager authenticationManager) {
        this.utilizadorServico = utilizadorServico;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/registar")
    public UtilizadorLocal registar(@RequestBody Registo registo) {
        return utilizadorServico.registrar(registo);
    }

    @PostMapping("/login")
    public UtilizadorLocal login(@RequestBody Login login, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getUtilizador(), login.getPassword())
        );

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(authentication);
        SecurityContextHolder.setContext(contexto);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);

        return utilizadorServico.buscarPorUtilizador(login.getUtilizador());
    }

    public static class Login {
        private String utilizador;
        private String password;

        public String getUtilizador() {
            return utilizador;
        }

        public void setUtilizador(String utilizador) {
            this.utilizador = utilizador;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
