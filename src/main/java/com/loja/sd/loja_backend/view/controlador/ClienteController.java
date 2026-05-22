package com.loja.sd.loja_backend.view.controlador;

import com.loja.sd.loja_backend.modelos.Endereco;
import com.loja.sd.loja_backend.modelos.UtilizadorLocal;
import com.loja.sd.loja_backend.repositorio.EnderecoRepositorio;
import com.loja.sd.loja_backend.servico.UtilizadorServico;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ClienteController {

    private final UtilizadorServico utilizadorServico;
    private final EnderecoRepositorio enderecoRepositorio;

    public ClienteController(UtilizadorServico utilizadorServico, EnderecoRepositorio enderecoRepositorio) {
        this.utilizadorServico = utilizadorServico;
        this.enderecoRepositorio = enderecoRepositorio;
    }

    @GetMapping("/clientes/perfil")
    public UtilizadorLocal perfil(Authentication authentication) {
        return utilizadorServico.buscarPorUtilizador(authentication.getName());
    }

    @PutMapping("/clientes/perfil")
    public UtilizadorLocal atualizarPerfil(Authentication authentication, @RequestBody PerfilPedido perfil) {
        return utilizadorServico.atualizarPerfil(authentication.getName(), perfil.getEmail(), perfil.getNome(), perfil.getSobrenome());
    }

    @GetMapping("/clientes/enderecos")
    public List<Endereco> listarEnderecos(Authentication authentication) {
        UtilizadorLocal utilizador = utilizadorServico.buscarPorUtilizador(authentication.getName());
        return enderecoRepositorio.findByUtilizadorLocalId(utilizador.getId());
    }

    @PostMapping("/clientes/enderecos")
    public Endereco criarEndereco(Authentication authentication, @RequestBody EnderecoPedido pedido) {
        UtilizadorLocal utilizador = utilizadorServico.buscarPorUtilizador(authentication.getName());
        Endereco endereco = new Endereco();
        endereco.setUtilizadorLocal(utilizador);
        preencherEndereco(endereco, pedido);
        return enderecoRepositorio.save(endereco);
    }

    @PutMapping("/clientes/enderecos/{id}")
    public Endereco atualizarEndereco(Authentication authentication, @PathVariable Long id, @RequestBody EnderecoPedido pedido) {
        Endereco endereco = buscarEnderecoDoUtilizador(authentication.getName(), id);
        preencherEndereco(endereco, pedido);
        return enderecoRepositorio.save(endereco);
    }

    @DeleteMapping("/clientes/enderecos/{id}")
    public void apagarEndereco(Authentication authentication, @PathVariable Long id) {
        enderecoRepositorio.delete(buscarEnderecoDoUtilizador(authentication.getName(), id));
    }

    private Endereco buscarEnderecoDoUtilizador(String utilizador, Long id) {
        UtilizadorLocal utilizadorLocal = utilizadorServico.buscarPorUtilizador(utilizador);
        Endereco endereco = enderecoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereco nao encontrado"));

        if (!endereco.getUtilizadorLocal().getId().equals(utilizadorLocal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endereco pertence a outro utilizador");
        }

        return endereco;
    }

    private void preencherEndereco(Endereco endereco, EnderecoPedido pedido) {
        endereco.setEndereco1(pedido.getEndereco1());
        endereco.setEndereco2(pedido.getEndereco2());
        endereco.setCidade(pedido.getCidade());
        endereco.setPais(pedido.getPais());
    }

    public static class EnderecoPedido {
        private String endereco1;
        private String endereco2;
        private String cidade;
        private String pais;

        public String getEndereco1() {
            return endereco1;
        }

        public void setEndereco1(String endereco1) {
            this.endereco1 = endereco1;
        }

        public String getEndereco2() {
            return endereco2;
        }

        public void setEndereco2(String endereco2) {
            this.endereco2 = endereco2;
        }

        public String getCidade() {
            return cidade;
        }

        public void setCidade(String cidade) {
            this.cidade = cidade;
        }

        public String getPais() {
            return pais;
        }

        public void setPais(String pais) {
            this.pais = pais;
        }
    }

    public static class PerfilPedido {
        private String email;
        private String nome;
        private String sobrenome;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getSobrenome() {
            return sobrenome;
        }

        public void setSobrenome(String sobrenome) {
            this.sobrenome = sobrenome;
        }
    }
}
