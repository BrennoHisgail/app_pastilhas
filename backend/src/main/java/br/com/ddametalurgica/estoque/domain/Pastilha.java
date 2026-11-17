package br.com.ddametalurgica.estoque.domain;

import br.com.ddametalurgica.estoque.exception.OperacaoNaoPermitidaException;
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

// Pastilha do catalogo.
//
// saldoAtual fica gravado na entidade para nao somar o historico a cada consulta.
// Nao ha setter de saldo: so registrarEntrada/registrarSaida alteram o valor,
// e eles validam as regras (pastilha ativa, quantidade positiva, saldo nunca negativo).
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

    // Prazo de entrega do fornecedor, em dias (t do Ponto de Pedido)
    @Column(name = "lead_time_dias", nullable = false)
    private Integer leadTimeDias = 0;

    // Reserva para cobrir atrasos e picos de consumo (Qs do Ponto de Pedido)
    @Column(name = "estoque_seguranca", nullable = false, precision = 14, scale = 3)
    private BigDecimal estoqueSeguranca = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // Lock otimista: uma alteracao concorrente baseada em dados antigos falha
    // em vez de sobrescrever o saldo
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    // Data de criacao, preenchida na insercao
    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }

    // Data da ultima alteracao, preenchida a cada update
    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Saldo no nivel minimo ou abaixo: precisa de reposicao
    public boolean isAbaixoDoMinimo() {
        return saldoAtual.compareTo(nivelMinimo) <= 0;
    }

    // Compara por valor (compareTo), ignorando a escala do BigDecimal
    public boolean temSaldoPara(BigDecimal quantidade) {
        return saldoAtual.compareTo(quantidade) >= 0;
    }

    public void registrarEntrada(BigDecimal quantidade) {
        exigirQuantidadePositiva(quantidade);
        exigirAtiva();
        this.saldoAtual = this.saldoAtual.add(quantidade);
    }

    // Recusa a saida maior que o saldo: o estoque nunca fica negativo
    public void registrarSaida(BigDecimal quantidade) {
        exigirQuantidadePositiva(quantidade);
        exigirAtiva();
        if (!temSaldoPara(quantidade)) {
            throw new OperacaoNaoPermitidaException(
                    "Saldo insuficiente: a pastilha " + codigo + " tem "
                            + semZerosSobrando(saldoAtual) + " " + unidadeMedida
                            + " em estoque, e a saída pede " + semZerosSobrando(quantidade) + ".");
        }
        this.saldoAtual = this.saldoAtual.subtract(quantidade);
    }

    private void exigirAtiva() {
        if (!ativo) {
            throw new OperacaoNaoPermitidaException(
                    "A pastilha " + codigo + " está inativa e não aceita movimentação.");
        }
    }

    // O DTO ja valida na entrada da API; aqui protege chamadas internas
    private static void exigirQuantidadePositiva(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    // Remove zeros a direita para a mensagem: 4.000 -> 4, 10.500 -> 10.5
    private static String semZerosSobrando(BigDecimal valor) {
        BigDecimal limpo = valor.stripTrailingZeros();
        if (limpo.scale() < 0) {
            limpo = limpo.setScale(0);
        }
        return limpo.toPlainString();
    }

    // Valor imobilizado: saldo x custo unitario
    public BigDecimal getValorEmEstoque() {
        return saldoAtual.multiply(custoUnitario);
    }

    // Exclusao logica: preserva as movimentacoes que referenciam a pastilha
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
