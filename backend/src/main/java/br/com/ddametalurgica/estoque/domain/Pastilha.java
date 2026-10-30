package br.com.ddametalurgica.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Entidade da pastilha. Cada objeto desta classe vira uma linha
// na tabela "pastilha" do banco.
//
// O saldo atual fica guardado aqui na propria pastilha para o
// sistema nao ter que somar todas as movimentacoes toda vez que
// alguem abre a tela.
//
// Repare que saldoAtual nao tem set: quem muda o saldo sao os
// metodos registrarEntrada e registrarSaida la embaixo.
@Entity
@Table(name = "pastilha")
public class Pastilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 10)
    private UnidadeMedida unidadeMedida;

    @Column(name = "nivel_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal nivelMinimo = BigDecimal.ZERO;

    @Column(name = "saldo_atual", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(name = "custo_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    // Quantos dias o fornecedor demora para entregar
    @Column(name = "lead_time_dias", nullable = false)
    private Integer leadTimeDias = 0;

    // Quantidade de reserva, para nao zerar o estoque
    @Column(name = "estoque_seguranca", nullable = false, precision = 14, scale = 3)
    private BigDecimal estoqueSeguranca = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // Se duas pessoas alterarem a mesma pastilha ao mesmo tempo, a segunda
    // recebe erro em vez de sobrescrever o saldo com um valor antigo
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    // O JPA chama este metodo sozinho antes de salvar pela primeira vez
    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }

    // E este aqui antes de cada alteracao
    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Avisa se a pastilha precisa ser reposta
    public boolean isAbaixoDoMinimo() {
        return saldoAtual.compareTo(nivelMinimo) <= 0;
    }

    // Confere se da para tirar essa quantidade do estoque
    public boolean temSaldoPara(BigDecimal quantidade) {
        return saldoAtual.compareTo(quantidade) >= 0;
    }

    // Soma no saldo quando entra material
    public void registrarEntrada(BigDecimal quantidade) {
        this.saldoAtual = this.saldoAtual.add(quantidade);
    }

    // Subtrai do saldo quando sai material.
    // Antes de chamar tem que usar o temSaldoPara.
    public void registrarSaida(BigDecimal quantidade) {
        this.saldoAtual = this.saldoAtual.subtract(quantidade);
    }

    // Quanto de dinheiro esta parado nesta pastilha
    public BigDecimal getValorEmEstoque() {
        return saldoAtual.multiply(custoUnitario);
    }

    // Nao apagamos a pastilha do banco, so marcamos como inativa,
    // senao perderiamos as movimentacoes antigas dela
    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public BigDecimal getNivelMinimo() {
        return nivelMinimo;
    }

    public void setNivelMinimo(BigDecimal nivelMinimo) {
        this.nivelMinimo = nivelMinimo;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    public Integer getLeadTimeDias() {
        return leadTimeDias;
    }

    public void setLeadTimeDias(Integer leadTimeDias) {
        this.leadTimeDias = leadTimeDias;
    }

    public BigDecimal getEstoqueSeguranca() {
        return estoqueSeguranca;
    }

    public void setEstoqueSeguranca(BigDecimal estoqueSeguranca) {
        this.estoqueSeguranca = estoqueSeguranca;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
