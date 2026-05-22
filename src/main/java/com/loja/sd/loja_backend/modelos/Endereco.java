package com.loja.sd.loja_backend.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "endereco")
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "endereco_1", nullable = false)
    private String endereco1;

    @Column(name = "endereco_2")
    private String endereco2;

    @Column(name = "cidade", nullable = false)
    private String cidade;

    @Column(name = "pais", nullable = false)
    private String pais;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "utilizador_local_id", nullable = false)
    private UtilizadorLocal utilizadorLocal;

    public UtilizadorLocal getUtilizadorLocal() {
        return utilizadorLocal;
    }

    public void setUtilizadorLocal(UtilizadorLocal utilizadorLocal) {
        this.utilizadorLocal = utilizadorLocal;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEndereco2() {
        return endereco2;
    }

    public void setEndereco2(String endereco2) {
        this.endereco2 = endereco2;
    }

    public String getEndereco1() {
        return endereco1;
    }

    public void setEndereco1(String endereco1) {
        this.endereco1 = endereco1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
