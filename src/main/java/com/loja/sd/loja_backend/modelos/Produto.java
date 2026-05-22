package com.loja.sd.loja_backend.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Column(name = "resumodescricao", nullable = false)
    private String resumodescricao;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "preco", nullable = false)
    private Double preco;

    @Column(name = "marca", nullable = false)
    private String marca;

    @Column(name = "modelo", nullable = false)
    private String modelo;

    @Column(name = "cilindrada_potencia")
    private String cilindradaPotencia;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "quilometragem")
    private Integer quilometragem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @OneToOne(mappedBy = "produto", cascade = CascadeType.REMOVE, optional = false, orphanRemoval = true)
    private Inventario inventario;

    @JsonIgnore
    @OneToMany(mappedBy = "produto")
    private List<QuantidadeEncomendas> quantidadeEncomendas = new ArrayList<>();

    public List<QuantidadeEncomendas> getQuantidadeEncomendas() {
        return quantidadeEncomendas;
    }

    public void setQuantidadeEncomendas(List<QuantidadeEncomendas> quantidadeEncomendas) {
        this.quantidadeEncomendas = quantidadeEncomendas;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Integer quilometragem) {
        this.quilometragem = quilometragem;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getCilindradaPotencia() {
        return cilindradaPotencia;
    }

    public void setCilindradaPotencia(String cilindradaPotencia) {
        this.cilindradaPotencia = cilindradaPotencia;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public void setInventario(Inventario inventario) {
        this.inventario = inventario;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getResumodescricao() {
        return resumodescricao;
    }

    public void setResumodescricao(String resumodescricao) {
        this.resumodescricao = resumodescricao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
