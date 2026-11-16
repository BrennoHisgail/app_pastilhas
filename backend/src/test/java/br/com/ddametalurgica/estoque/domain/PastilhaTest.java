package br.com.ddametalurgica.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.exception.OperacaoNaoPermitidaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Pastilha - regras que ficam na propria entidade")
class PastilhaTest {

    @Nested
    @DisplayName("saldo")
    class Saldo {

        @Test
        @DisplayName("entrada soma no saldo atual")
        void entradaSoma() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            pastilha.registrarEntrada(new BigDecimal("5.5"));

            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("15.5");
        }

        @Test
        @DisplayName("saida subtrai do saldo atual")
        void saidaSubtrai() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            pastilha.registrarSaida(new BigDecimal("4"));

            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("6");
        }

        @Test
        @DisplayName("pastilha nova comeca com saldo zero")
        void nasceZerada() {
            assertThat(new Pastilha().getSaldoAtual()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("temSaldoPara aceita quantidade igual ao saldo")
        void temSaldoParaOExato() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            assertThat(pastilha.temSaldoPara(new BigDecimal("10"))).isTrue();
            assertThat(pastilha.temSaldoPara(new BigDecimal("10.001"))).isFalse();
        }

        @Test
        @DisplayName("temSaldoPara compara o valor, nao a quantidade de casas decimais")
        void temSaldoParaComparaValorNaoEscala() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            assertThat(pastilha.temSaldoPara(new BigDecimal("10.000"))).isTrue();
        }

        @Test
        @DisplayName("saida maior que o saldo e recusada pela propria pastilha e o saldo nao muda")
        void saidaSemSaldoERecusada() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            assertThatThrownBy(() -> pastilha.registrarSaida(new BigDecimal("11")))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessageContaining("Saldo insuficiente");
            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("pastilha inativa recusa entrada e saida")
        void inativaNaoMovimenta() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);
            pastilha.inativar();

            assertThatThrownBy(() -> pastilha.registrarEntrada(BigDecimal.ONE))
                    .isInstanceOf(OperacaoNaoPermitidaException.class);
            assertThatThrownBy(() -> pastilha.registrarSaida(BigDecimal.ONE))
                    .isInstanceOf(OperacaoNaoPermitidaException.class);
            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("quantidade zero ou negativa e recusada")
        void quantidadeInvalida() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("10"), BigDecimal.ZERO);

            assertThatThrownBy(() -> pastilha.registrarEntrada(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> pastilha.registrarSaida(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("nivel minimo")
    class NivelMinimo {

        @Test
        @DisplayName("entra na lista de reposicao quando o saldo chega no nivel minimo")
        void abaixoQuandoIgual() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("20"), new BigDecimal("20"));

            assertThat(pastilha.isAbaixoDoMinimo()).isTrue();
        }

        @Test
        @DisplayName("fica fora da lista quando o saldo e maior que o nivel minimo")
        void acimaDoMinimo() {
            Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("21"), new BigDecimal("20"));

            assertThat(pastilha.isAbaixoDoMinimo()).isFalse();
        }
    }

    @Test
    @DisplayName("valor em estoque e o saldo vezes o custo unitario")
    void valorEmEstoque() {
        Pastilha pastilha = Fabrica.pastilha("A1", new BigDecimal("3"), BigDecimal.ZERO);
        pastilha.setCustoUnitario(new BigDecimal("12.50"));

        assertThat(pastilha.getValorEmEstoque()).isEqualByComparingTo("37.50");
    }

    @Test
    @DisplayName("inativar e reativar mudam so a flag, o saldo continua igual")
    void inativarEReativar() {
        Pastilha pastilha = Fabrica.pastilha("A1");
        assertThat(pastilha.isAtivo()).isTrue();

        pastilha.inativar();
        assertThat(pastilha.isAtivo()).isFalse();
        assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("100");

        pastilha.reativar();
        assertThat(pastilha.isAtivo()).isTrue();
    }
}
