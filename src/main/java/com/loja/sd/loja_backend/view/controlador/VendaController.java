package com.loja.sd.loja_backend.view.controlador;

import com.loja.sd.loja_backend.modelos.Encomenda;
import com.loja.sd.loja_backend.modelos.Endereco;
import com.loja.sd.loja_backend.modelos.Inventario;
import com.loja.sd.loja_backend.modelos.Produto;
import com.loja.sd.loja_backend.modelos.QuantidadeEncomendas;
import com.loja.sd.loja_backend.modelos.UtilizadorLocal;
import com.loja.sd.loja_backend.repositorio.EncomendaRepositorio;
import com.loja.sd.loja_backend.repositorio.EnderecoRepositorio;
import com.loja.sd.loja_backend.repositorio.InventarioRepositorio;
import com.loja.sd.loja_backend.repositorio.ProdutoRepositorio;
import com.loja.sd.loja_backend.servico.UtilizadorServico;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class VendaController {

    private final EncomendaRepositorio encomendaRepositorio;
    private final EnderecoRepositorio enderecoRepositorio;
    private final ProdutoRepositorio produtoRepositorio;
    private final InventarioRepositorio inventarioRepositorio;
    private final UtilizadorServico utilizadorServico;

    public VendaController(EncomendaRepositorio encomendaRepositorio, EnderecoRepositorio enderecoRepositorio, ProdutoRepositorio produtoRepositorio, InventarioRepositorio inventarioRepositorio, UtilizadorServico utilizadorServico) {
        this.encomendaRepositorio = encomendaRepositorio;
        this.enderecoRepositorio = enderecoRepositorio;
        this.produtoRepositorio = produtoRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
        this.utilizadorServico = utilizadorServico;
    }

    @GetMapping("/vendas")
    public List<Encomenda> listar(Authentication authentication) {
        UtilizadorLocal utilizador = utilizadorServico.buscarPorUtilizador(authentication.getName());
        return encomendaRepositorio.findByUtilizadorLocalId(utilizador.getId());
    }

    @PostMapping("/vendas")
    @Transactional
    public Fatura comprar(Authentication authentication, @RequestBody VendaPedido pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Venda sem produtos");
        }

        UtilizadorLocal utilizador = utilizadorServico.buscarPorUtilizador(authentication.getName());
        Endereco endereco = buscarEnderecoDoUtilizador(utilizador, pedido.getEnderecoId());

        Encomenda encomenda = new Encomenda();
        encomenda.setUtilizadorLocal(utilizador);
        encomenda.setEndereco(endereco);
        encomenda.setData(LocalDateTime.now());
        encomenda.setValorTotal(0.0);
        encomenda = encomendaRepositorio.save(encomenda);

        Double valorTotal = 0.0;
        for (ItemVendaPedido item : pedido.getItens()) {
            QuantidadeEncomendas quantidade = criarItem(encomenda, item);
            valorTotal = valorTotal + quantidade.getValorTotal();
            encomenda.getQuantidadeEncomendas().add(quantidade);
        }

        encomenda.setValorTotal(valorTotal);
        encomenda = encomendaRepositorio.save(encomenda);

        return criarFatura(encomenda);
    }

    @GetMapping("/vendas/{id}/fatura")
    public Fatura fatura(Authentication authentication, @PathVariable Long id) {
        Encomenda encomenda = encomendaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda nao encontrada"));

        if (!encomenda.getUtilizadorLocal().getUtilizador().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Venda pertence a outro utilizador");
        }

        return criarFatura(encomenda);
    }

    @GetMapping("/estatisticas/produtos/mais-vendidos")
    public List<ProdutoVendido> produtosMaisVendidos(@RequestParam(required = false) Integer limite) {
        return criarProdutosVendidos(produtoRepositorio.produtosMaisVendidos(PageRequest.of(0, valorLimite(limite))));
    }

    @GetMapping("/estatisticas/produtos/menos-vendidos")
    public List<ProdutoVendido> produtosMenosVendidos(@RequestParam(required = false) Integer limite) {
        return criarProdutosVendidos(produtoRepositorio.produtosMenosVendidos(PageRequest.of(0, valorLimite(limite))));
    }

    @GetMapping("/estatisticas/clientes/melhores")
    public List<ClienteFaturado> melhoresClientes(@RequestParam(required = false) Integer limite) {
        List<ClienteFaturado> clientes = new ArrayList<>();

        for (Object[] linha : encomendaRepositorio.melhoresClientes(PageRequest.of(0, valorLimite(limite)))) {
            ClienteFaturado cliente = new ClienteFaturado();
            cliente.setClienteId(((Number) linha[0]).longValue());
            cliente.setUtilizador((String) linha[1]);
            cliente.setNome((String) linha[2]);
            cliente.setSobrenome((String) linha[3]);
            cliente.setValorTotal(((Number) linha[4]).doubleValue());
            clientes.add(cliente);
        }

        return clientes;
    }

    @GetMapping("/estatisticas/faturacao")
    public Faturacao faturacao(@RequestParam(defaultValue = "dia") String periodo) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio;
        LocalDateTime fim;

        if ("dia".equalsIgnoreCase(periodo)) {
            inicio = hoje.atStartOfDay();
            fim = inicio.plusDays(1);
        } else if ("semana".equalsIgnoreCase(periodo)) {
            inicio = hoje.with(DayOfWeek.MONDAY).atStartOfDay();
            fim = inicio.plusWeeks(1);
        } else if ("mes".equalsIgnoreCase(periodo)) {
            inicio = hoje.withDayOfMonth(1).atStartOfDay();
            fim = inicio.plusMonths(1);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periodo invalido");
        }

        Faturacao faturacao = new Faturacao();
        faturacao.setPeriodo(periodo);
        faturacao.setInicio(inicio);
        faturacao.setFim(fim);
        faturacao.setValorTotal(encomendaRepositorio.valorFaturadoEntre(inicio, fim));

        return faturacao;
    }

    private Endereco buscarEnderecoDoUtilizador(UtilizadorLocal utilizador, Long enderecoId) {
        Endereco endereco = enderecoRepositorio.findById(enderecoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereco nao encontrado"));

        if (!endereco.getUtilizadorLocal().getId().equals(utilizador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endereco pertence a outro utilizador");
        }

        return endereco;
    }

    private QuantidadeEncomendas criarItem(Encomenda encomenda, ItemVendaPedido item) {
        if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade invalida");
        }

        Produto produto = produtoRepositorio.findById(item.getProdutoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
        Inventario inventario = inventarioRepositorio.findByProdutoId(produto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto sem inventario"));

        if (inventario.getQuantidade() < item.getQuantidade()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock insuficiente");
        }

        inventario.setQuantidade(inventario.getQuantidade() - item.getQuantidade());
        inventarioRepositorio.save(inventario);

        QuantidadeEncomendas quantidade = new QuantidadeEncomendas();
        quantidade.setEncomenda(encomenda);
        quantidade.setProduto(produto);
        quantidade.setQuantidade(item.getQuantidade());
        quantidade.setPrecoUnitario(produto.getPreco());
        quantidade.setValorTotal(produto.getPreco() * item.getQuantidade());

        return quantidade;
    }

    private Fatura criarFatura(Encomenda encomenda) {
        Fatura fatura = new Fatura();
        fatura.setEncomendaId(encomenda.getId());
        fatura.setCliente(encomenda.getUtilizadorLocal().getNome() + " " + encomenda.getUtilizadorLocal().getSobrenome());
        fatura.setEndereco(encomenda.getEndereco());
        fatura.setData(encomenda.getData());
        fatura.setValorTotal(encomenda.getValorTotal());

        List<ItemFatura> itens = new ArrayList<>();
        for (QuantidadeEncomendas quantidade : encomenda.getQuantidadeEncomendas()) {
            ItemFatura item = new ItemFatura();
            item.setProdutoId(quantidade.getProduto().getId());
            item.setProduto(quantidade.getProduto().getNome());
            item.setQuantidade(quantidade.getQuantidade());
            item.setPrecoUnitario(quantidade.getPrecoUnitario());
            item.setValorTotal(quantidade.getValorTotal());
            itens.add(item);
        }
        fatura.setItens(itens);

        return fatura;
    }

    private List<ProdutoVendido> criarProdutosVendidos(List<Object[]> linhas) {
        List<ProdutoVendido> produtos = new ArrayList<>();

        for (Object[] linha : linhas) {
            ProdutoVendido produto = new ProdutoVendido();
            produto.setProdutoId(((Number) linha[0]).longValue());
            produto.setProduto((String) linha[1]);
            produto.setQuantidade(((Number) linha[2]).longValue());
            produtos.add(produto);
        }

        return produtos;
    }

    private Integer valorLimite(Integer limite) {
        if (limite == null || limite <= 0) {
            return 10;
        }
        return limite;
    }

    public static class VendaPedido {
        private Long enderecoId;
        private List<ItemVendaPedido> itens = new ArrayList<>();

        public Long getEnderecoId() {
            return enderecoId;
        }

        public void setEnderecoId(Long enderecoId) {
            this.enderecoId = enderecoId;
        }

        public List<ItemVendaPedido> getItens() {
            return itens;
        }

        public void setItens(List<ItemVendaPedido> itens) {
            this.itens = itens;
        }
    }

    public static class ItemVendaPedido {
        private Long produtoId;
        private Integer quantidade;

        public Long getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(Long produtoId) {
            this.produtoId = produtoId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }
    }

    public static class Fatura {
        private Long encomendaId;
        private String cliente;
        private Endereco endereco;
        private LocalDateTime data;
        private List<ItemFatura> itens = new ArrayList<>();
        private Double valorTotal;

        public Long getEncomendaId() {
            return encomendaId;
        }

        public void setEncomendaId(Long encomendaId) {
            this.encomendaId = encomendaId;
        }

        public String getCliente() {
            return cliente;
        }

        public void setCliente(String cliente) {
            this.cliente = cliente;
        }

        public Endereco getEndereco() {
            return endereco;
        }

        public void setEndereco(Endereco endereco) {
            this.endereco = endereco;
        }

        public LocalDateTime getData() {
            return data;
        }

        public void setData(LocalDateTime data) {
            this.data = data;
        }

        public List<ItemFatura> getItens() {
            return itens;
        }

        public void setItens(List<ItemFatura> itens) {
            this.itens = itens;
        }

        public Double getValorTotal() {
            return valorTotal;
        }

        public void setValorTotal(Double valorTotal) {
            this.valorTotal = valorTotal;
        }
    }

    public static class ItemFatura {
        private Long produtoId;
        private String produto;
        private Integer quantidade;
        private Double precoUnitario;
        private Double valorTotal;

        public Long getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(Long produtoId) {
            this.produtoId = produtoId;
        }

        public String getProduto() {
            return produto;
        }

        public void setProduto(String produto) {
            this.produto = produto;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public Double getPrecoUnitario() {
            return precoUnitario;
        }

        public void setPrecoUnitario(Double precoUnitario) {
            this.precoUnitario = precoUnitario;
        }

        public Double getValorTotal() {
            return valorTotal;
        }

        public void setValorTotal(Double valorTotal) {
            this.valorTotal = valorTotal;
        }
    }

    public static class ProdutoVendido {
        private Long produtoId;
        private String produto;
        private Long quantidade;

        public Long getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(Long produtoId) {
            this.produtoId = produtoId;
        }

        public String getProduto() {
            return produto;
        }

        public void setProduto(String produto) {
            this.produto = produto;
        }

        public Long getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Long quantidade) {
            this.quantidade = quantidade;
        }
    }

    public static class ClienteFaturado {
        private Long clienteId;
        private String utilizador;
        private String nome;
        private String sobrenome;
        private Double valorTotal;

        public Long getClienteId() {
            return clienteId;
        }

        public void setClienteId(Long clienteId) {
            this.clienteId = clienteId;
        }

        public String getUtilizador() {
            return utilizador;
        }

        public void setUtilizador(String utilizador) {
            this.utilizador = utilizador;
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

        public Double getValorTotal() {
            return valorTotal;
        }

        public void setValorTotal(Double valorTotal) {
            this.valorTotal = valorTotal;
        }
    }

    public static class Faturacao {
        private String periodo;
        private LocalDateTime inicio;
        private LocalDateTime fim;
        private Double valorTotal;

        public String getPeriodo() {
            return periodo;
        }

        public void setPeriodo(String periodo) {
            this.periodo = periodo;
        }

        public LocalDateTime getInicio() {
            return inicio;
        }

        public void setInicio(LocalDateTime inicio) {
            this.inicio = inicio;
        }

        public LocalDateTime getFim() {
            return fim;
        }

        public void setFim(LocalDateTime fim) {
            this.fim = fim;
        }

        public Double getValorTotal() {
            return valorTotal;
        }

        public void setValorTotal(Double valorTotal) {
            this.valorTotal = valorTotal;
        }
    }
}
