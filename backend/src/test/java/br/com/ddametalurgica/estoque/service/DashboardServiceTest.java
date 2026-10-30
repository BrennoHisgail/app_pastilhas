package br.com.ddametalurgica.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.dto.IndicadoresResponse;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.repository.ResumoPastilhas;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - numeros do painel")
class DashboardServiceTest {

    @Mock
    private PastilhaRepository pastilhaRepository;

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    @DisplayName("junta os contadores, os alertas e as ultimas movimentacoes numa resposta so")
    void montaIndicadores() {
        Pastilha emFalta = Fabrica.comId(
                Fabrica.pastilha("A1", new BigDecimal("5"), new BigDecimal("20")), 1L);
        Movimentacao recente = new Movimentacao(
                emFalta, TipoMovimentacao.SAIDA, new BigDecimal("3"), "OP 10", "Usinagem", null);

        when(pastilhaRepository.buscarResumo())
                .thenReturn(new ResumoPastilhas(12L, 5L, 4L, new BigDecimal("1234.56")));
        when(movimentacaoRepository.contarDesde(any())).thenReturn(87L);
        when(pastilhaRepository.listarAbaixoDoMinimo(any(Pageable.class))).thenReturn(List.of(emFalta));
        when(movimentacaoRepository.findTop8ByOrderByDataHoraDesc()).thenReturn(List.of(recente));

        IndicadoresResponse indicadores = service.indicadores();

        assertThat(indicadores.itensCadastrados()).isEqualTo(12L);
        assertThat(indicadores.categorias()).isEqualTo(5L);
        assertThat(indicadores.itensAbaixoDoMinimo()).isEqualTo(4L);
        assertThat(indicadores.valorEmEstoque()).isEqualByComparingTo("1234.56");
        assertThat(indicadores.movimentacoesUltimos30Dias()).isEqualTo(87L);
        assertThat(indicadores.alertasReposicao()).hasSize(1);
        assertThat(indicadores.alertasReposicao().get(0).abaixoDoMinimo()).isTrue();
        assertThat(indicadores.movimentacoesRecentes()).hasSize(1);
        assertThat(indicadores.movimentacoesRecentes().get(0).pastilhaCodigo()).isEqualTo("A1");
    }

    @Test
    @DisplayName("o contador de movimentacoes olha os ultimos 30 dias")
    void contaUltimos30Dias() {
        when(pastilhaRepository.listarAbaixoDoMinimo(any(Pageable.class))).thenReturn(List.of());
        when(movimentacaoRepository.findTop8ByOrderByDataHoraDesc()).thenReturn(List.of());
        when(pastilhaRepository.buscarResumo()).thenReturn(new ResumoPastilhas(0L, 0L, 0L, BigDecimal.ZERO));

        service.indicadores();

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(movimentacaoRepository).contarDesde(desde.capture());
        assertThat(desde.getValue().toLocalDate())
                .isEqualTo(LocalDateTime.now().minusDays(30).toLocalDate());
    }

    @Test
    @DisplayName("o painel mostra no maximo 10 alertas")
    void limitaAlertasDoPainel() {
        when(pastilhaRepository.listarAbaixoDoMinimo(any(Pageable.class))).thenReturn(List.of());
        when(movimentacaoRepository.findTop8ByOrderByDataHoraDesc()).thenReturn(List.of());
        when(pastilhaRepository.buscarResumo()).thenReturn(new ResumoPastilhas(0L, 0L, 0L, BigDecimal.ZERO));

        service.indicadores();

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(pastilhaRepository).listarAbaixoDoMinimo(pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("a tela de reposicao escolhe quantos itens quer ver")
    void alertasComLimiteProprio() {
        when(pastilhaRepository.listarAbaixoDoMinimo(any(Pageable.class))).thenReturn(List.of());

        service.alertasReposicao(50);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(pastilhaRepository).listarAbaixoDoMinimo(pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(50);
    }
}
