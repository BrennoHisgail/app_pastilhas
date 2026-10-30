package br.com.ddametalurgica.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.dto.IndicadorPastilhaResponse;
import br.com.ddametalurgica.estoque.dto.IndicadoresEstoqueResponse;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.repository.TotalMovimentado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndicadorService - PP, Giro, Cobertura e Curva ABC")
class IndicadorServiceTest {

    @Mock
    private PastilhaRepository pastilhaRepository;

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @InjectMocks
    private IndicadorService service;

    private IndicadoresEstoqueResponse resultado;

    // Todas com custo 10, lead time 7 e estoque de seguranca 5 (ver Fabrica).
    // Periodo de 30 dias:
    //   A1 saldo 100, entrou 30, saiu 60 -> valor consumido 600
    //   B2 saldo 10,  saiu 30            -> valor consumido 300
    //   D4 saldo 40,  saiu 10            -> valor consumido 100
    //   C3 saldo 50,  sem movimento      -> valor consumido 0
    @BeforeEach
    void calcular() {
        Pastilha a1 = Fabrica.comId(Fabrica.pastilha("A1", new BigDecimal("100"), new BigDecimal("20")), 1L);
        Pastilha b2 = Fabrica.comId(Fabrica.pastilha("B2", new BigDecimal("10"), new BigDecimal("5")), 2L);
        Pastilha c3 = Fabrica.comId(Fabrica.pastilha("C3", new BigDecimal("50"), new BigDecimal("5")), 3L);
        Pastilha d4 = Fabrica.comId(Fabrica.pastilha("D4", new BigDecimal("40"), new BigDecimal("5")), 4L);
        when(pastilhaRepository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(List.of(a1, b2, c3, d4));

        when(movimentacaoRepository.somarPorPastilhaDesde(any())).thenReturn(List.of(
                new TotalMovimentado(1L, TipoMovimentacao.ENTRADA, new BigDecimal("30")),
                new TotalMovimentado(1L, TipoMovimentacao.SAIDA, new BigDecimal("60")),
                new TotalMovimentado(2L, TipoMovimentacao.SAIDA, new BigDecimal("30")),
                new TotalMovimentado(4L, TipoMovimentacao.SAIDA, new BigDecimal("10"))));

        resultado = service.calcular(30);
    }

    private IndicadorPastilhaResponse item(String codigo) {
        return resultado.itens().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("consumo medio diario e Ponto de Pedido seguem PP = (d x t) + Qs")
    void pontoDePedido() {
        // d = 60 / 30 = 2; PP = 2 x 7 + 5 = 19
        assertThat(item("A1").consumoMedioDiario()).isEqualByComparingTo("2");
        assertThat(item("A1").pontoDePedido()).isEqualByComparingTo("19");
        assertThat(item("A1").abaixoDoPontoDePedido()).isFalse();

        // d = 1; PP = 1 x 7 + 5 = 12, e o saldo 10 ja esta abaixo
        assertThat(item("B2").pontoDePedido()).isEqualByComparingTo("12");
        assertThat(item("B2").abaixoDoPontoDePedido()).isTrue();
        assertThat(resultado.abaixoDoPontoDePedido()).isEqualTo(1);
    }

    @Test
    @DisplayName("cobertura e saldo / consumo diario, e fica vazia sem consumo")
    void cobertura() {
        assertThat(item("A1").coberturaDias()).isEqualByComparingTo("50");
        assertThat(item("C3").coberturaDias()).isNull();
    }

    @Test
    @DisplayName("giro e o consumo dividido pelo estoque medio do periodo")
    void giro() {
        // saldo inicial = 100 - 30 + 60 = 130; estoque medio = (130 + 100) / 2 = 115
        // giro = 60 / 115 = 0,52
        assertThat(item("A1").giro()).isEqualByComparingTo("0.52");
        assertThat(item("C3").giro()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Curva ABC: ordena pelo valor consumido e separa em 80% / 95%")
    void curvaAbc() {
        assertThat(resultado.itens()).extracting(IndicadorPastilhaResponse::codigo)
                .containsExactly("A1", "B2", "D4", "C3");
        assertThat(resultado.itens()).extracting(IndicadorPastilhaResponse::classeAbc)
                .containsExactly("A", "A", "B", "C");
        assertThat(item("B2").percentualAcumulado()).isEqualByComparingTo("90");
        assertThat(resultado.classeA()).isEqualTo(2);
        assertThat(resultado.classeB()).isEqualTo(1);
        assertThat(resultado.classeC()).isEqualTo(1);
    }
}
