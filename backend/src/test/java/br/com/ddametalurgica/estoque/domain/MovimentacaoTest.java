package br.com.ddametalurgica.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Movimentacao - fotografia do saldo no momento do lancamento")
class MovimentacaoTest {

    @Test
    @DisplayName("saldoApos guarda o saldo que a pastilha tem na hora da criacao")
    void guardaSaldoApos() {
        Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("100"), BigDecimal.ZERO);
        pastilha.registrarEntrada(new BigDecimal("30"));

        Movimentacao movimentacao = new Movimentacao(
                pastilha, TipoMovimentacao.ENTRADA, new BigDecimal("30"), "NF 1", "Fornecedor", null);

        assertThat(movimentacao.getSaldoApos()).isEqualByComparingTo("130");
        assertThat(movimentacao.getQuantidade()).isEqualByComparingTo("30");
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(movimentacao.getPastilha()).isSameAs(pastilha);
        assertThat(movimentacao.getDocumento()).isEqualTo("NF 1");
        assertThat(movimentacao.getOrigemDestino()).isEqualTo("Fornecedor");
    }

    @Test
    @DisplayName("data e hora sao preenchidas na gravacao quando vem vazias")
    void preencheDataHoraNaGravacao() {
        Movimentacao movimentacao = new Movimentacao(
                Fabrica.pastilha("A1"), TipoMovimentacao.SAIDA, BigDecimal.ONE, null, null, null);
        assertThat(movimentacao.getDataHora()).isNull();

        ReflectionTestUtils.invokeMethod(movimentacao, "aoCriar");

        assertThat(movimentacao.getDataHora()).isNotNull()
                .isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("data e hora ja informada nao e sobrescrita na gravacao")
    void naoSobrescreveDataHora() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        Movimentacao movimentacao = new Movimentacao(
                Fabrica.pastilha("A1"), TipoMovimentacao.SAIDA, BigDecimal.ONE, null, null, null);
        ReflectionTestUtils.setField(movimentacao, "dataHora", ontem);

        ReflectionTestUtils.invokeMethod(movimentacao, "aoCriar");

        assertThat(movimentacao.getDataHora()).isEqualTo(ontem);
    }
}
