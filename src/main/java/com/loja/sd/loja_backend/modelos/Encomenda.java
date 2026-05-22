package com.loja.sd.loja_backend.modelos;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "encomenda")
public class Encomenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilizador_local_id", nullable = false)
    private UtilizadorLocal utilizadorLocal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal = 0.0;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuantidadeEncomendas> quantidadeEncomendas = new ArrayList<>();

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public List<QuantidadeEncomendas> getQuantidadeEncomendas() {
        return quantidadeEncomendas;
    }

    public void setQuantidadeEncomendas(List<QuantidadeEncomendas> quantidadeEncomendas) {
        this.quantidadeEncomendas = quantidadeEncomendas;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public UtilizadorLocal getUtilizadorLocal() {
        return utilizadorLocal;
    }

    public void setUtilizadorLocal(UtilizadorLocal utilizadorLocal) {
        this.utilizadorLocal = utilizadorLocal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
