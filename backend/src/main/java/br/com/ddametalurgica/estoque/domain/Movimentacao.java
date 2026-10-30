package br.com.ddametalurgica.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Entidade que guarda cada entrada ou saida do estoque.
//
// O campo saldoApos grava como ficou o saldo depois do lancamento,
// assim da para conferir o historico depois.
//
// A movimentacao nao tem metodo de alterar de proposito: se o
// usuario errar, ele lanca uma nova movimentacao para corrigir.
@Entity
@Table(name = "movimentacao")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pastilha_id", nullable = false)
    private Pastilha pastilha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "saldo_apos", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoApos;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Quem fez o lancamento. Ainda nao e preenchido.
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "origem_destino", length = 160)
    private String origemDestino;

    @Column(length = 60)
    private String documento;

    @Column(length = 400)
    private String observacao;

    protected Movimentacao() {
        // exigido pelo JPA
    }

    public Movimentacao(Pastilha pastilha,
                        TipoMovimentacao tipo,
                        BigDecimal quantidade,
                        String documento,
                        String origemDestino,
                        String observacao) {
        this.pastilha = pastilha;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = pastilha.getSaldoAtual();
        this.documento = documento;
        this.origemDestino = origemDestino;
        this.observacao = observacao;
    }

    @PrePersist
    void aoCriar() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Pastilha getPastilha() {
        return pastilha;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getSaldoApos() {
        return saldoApos;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getOrigemDestino() {
        return origemDestino;
    }

    public String getDocumento() {
        return documento;
    }

    public String getObservacao() {
        return observacao;
    }
}
