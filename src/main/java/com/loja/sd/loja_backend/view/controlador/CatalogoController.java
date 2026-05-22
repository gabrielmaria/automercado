package com.loja.sd.loja_backend.view.controlador;

import com.loja.sd.loja_backend.modelos.Categoria;
import com.loja.sd.loja_backend.modelos.Inventario;
import com.loja.sd.loja_backend.modelos.Produto;
import com.loja.sd.loja_backend.repositorio.CategoriaRepositorio;
import com.loja.sd.loja_backend.repositorio.InventarioRepositorio;
import com.loja.sd.loja_backend.repositorio.ProdutoRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
public class CatalogoController {

    private final CategoriaRepositorio categoriaRepositorio;
    private final ProdutoRepositorio produtoRepositorio;
    private final InventarioRepositorio inventarioRepositorio;

    public CatalogoController(CategoriaRepositorio categoriaRepositorio, ProdutoRepositorio produtoRepositorio, InventarioRepositorio inventarioRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.produtoRepositorio = produtoRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
    }

    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return categoriaRepositorio.findAll();
    }

    @GetMapping("/categorias/{id}")
    public Categoria buscarCategoria(@PathVariable Long id) {
        return categoriaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
    }

    @PostMapping("/categorias")
    public Categoria criarCategoria(@RequestBody Categoria categoria) {
        if (categoriaRepositorio.findByNome(categoria.getNome()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria ja existe");
        }

        return categoriaRepositorio.save(categoria);
    }

    @PutMapping("/categorias/{id}")
    public Categoria atualizarCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        Categoria categoriaAtual = buscarCategoria(id);
        categoriaRepositorio.findByNome(categoria.getNome()).ifPresent(categoriaExistente -> {
            if (!categoriaExistente.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria ja existe");
            }
        });

        categoriaAtual.setNome(categoria.getNome());
        categoriaAtual.setDescricao(categoria.getDescricao());
        return categoriaRepositorio.save(categoriaAtual);
    }

    @DeleteMapping("/categorias/{id}")
    public void apagarCategoria(@PathVariable Long id) {
        categoriaRepositorio.delete(buscarCategoria(id));
    }

    @GetMapping("/produtos")
    public List<Produto> listarProdutos() {
        return produtoRepositorio.findAll();
    }

    @GetMapping("/produtos/{id}")
    public Produto buscarProduto(@PathVariable Long id) {
        return produtoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
    }

    @PostMapping("/produtos")
    public Produto criarProduto(@RequestBody ProdutoPedido pedido) {
        if (produtoRepositorio.findByNome(pedido.getNome()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto ja existe");
        }

        Produto produto = new Produto();
        preencherProduto(produto, pedido);

        Produto produtoGuardado = produtoRepositorio.save(produto);
        Inventario inventario = new Inventario();
        inventario.setProduto(produtoGuardado);
        inventario.setQuantidade(valorStock(pedido.getStock()));
        inventarioRepositorio.save(inventario);
        produtoGuardado.setInventario(inventario);

        return produtoGuardado;
    }

    @PutMapping("/produtos/{id}")
    public Produto atualizarProduto(@PathVariable Long id, @RequestBody ProdutoPedido pedido) {
        Produto produto = buscarProduto(id);
        produtoRepositorio.findByNome(pedido.getNome()).ifPresent(produtoExistente -> {
            if (!produtoExistente.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto ja existe");
            }
        });

        preencherProduto(produto, pedido);
        Produto produtoGuardado = produtoRepositorio.save(produto);

        Inventario inventario = inventarioRepositorio.findByProdutoId(produtoGuardado.getId())
                .orElseGet(() -> {
                    Inventario novoInventario = new Inventario();
                    novoInventario.setProduto(produtoGuardado);
                    return novoInventario;
                });
        inventario.setQuantidade(valorStock(pedido.getStock()));
        inventarioRepositorio.save(inventario);
        produtoGuardado.setInventario(inventario);

        return produtoGuardado;
    }

    @DeleteMapping("/produtos/{id}")
    public void apagarProduto(@PathVariable Long id) {
        Produto produto = buscarProduto(id);
        try {
            produtoRepositorio.delete(produto);
            produtoRepositorio.flush();
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto com historico de vendas nao pode ser apagado");
        }
    }

    private void preencherProduto(Produto produto, ProdutoPedido pedido) {
        Categoria categoria = categoriaRepositorio.findById(pedido.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));

        produto.setNome(pedido.getNome());
        produto.setResumodescricao(pedido.getResumodescricao());
        produto.setDescricao(pedido.getDescricao());
        produto.setPreco(pedido.getPreco());
        produto.setMarca(pedido.getMarca());
        produto.setModelo(pedido.getModelo());
        produto.setCilindradaPotencia(pedido.getCilindradaPotencia());
        produto.setAno(pedido.getAno());
        produto.setQuilometragem(pedido.getQuilometragem());
        produto.setCategoria(categoria);
    }

    private Integer valorStock(Integer stock) {
        if (stock == null) {
            return 0;
        }
        return stock;
    }

    public static class ProdutoPedido {
        private String nome;
        private String resumodescricao;
        private String descricao;
        private Double preco;
        private String marca;
        private String modelo;
        private String cilindradaPotencia;
        private Integer ano;
        private Integer quilometragem;
        private Integer stock;
        private Long categoriaId;

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getResumodescricao() {
            return resumodescricao;
        }

        public void setResumodescricao(String resumodescricao) {
            this.resumodescricao = resumodescricao;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public Double getPreco() {
            return preco;
        }

        public void setPreco(Double preco) {
            this.preco = preco;
        }

        public String getMarca() {
            return marca;
        }

        public void setMarca(String marca) {
            this.marca = marca;
        }

        public String getModelo() {
            return modelo;
        }

        public void setModelo(String modelo) {
            this.modelo = modelo;
        }

        public String getCilindradaPotencia() {
            return cilindradaPotencia;
        }

        public void setCilindradaPotencia(String cilindradaPotencia) {
            this.cilindradaPotencia = cilindradaPotencia;
        }

        public Integer getAno() {
            return ano;
        }

        public void setAno(Integer ano) {
            this.ano = ano;
        }

        public Integer getQuilometragem() {
            return quilometragem;
        }

        public void setQuilometragem(Integer quilometragem) {
            this.quilometragem = quilometragem;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public Long getCategoriaId() {
            return categoriaId;
        }

        public void setCategoriaId(Long categoriaId) {
            this.categoriaId = categoriaId;
        }
    }
}
