package br.com.ddametalurgica.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.dto.MovimentacaoRequest;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.security.UsuarioDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovimentacaoService - entradas, saidas e historico")
class MovimentacaoServiceTest {

    @Mock
    private MovimentacaoRepository repository;

    @Mock
    private PastilhaRepository pastilhaRepository;

    @InjectMocks
    private MovimentacaoService service;

    @Captor
    private ArgumentCaptor<Movimentacao> movimentacaoSalva;

    private MovimentacaoRequest pedido(BigDecimal quantidade) {
        return new MovimentacaoRequest(1L, quantidade, "NF 123", "Fornecedor Sandvik", "teste");
    }

    private void devolvePastilha(Pastilha pastilha) {
        when(pastilhaRepository.buscarParaMovimentar(1L)).thenReturn(Optional.of(pastilha));
        when(repository.save(any(Movimentacao.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Nested
    @DisplayName("entrada")
    class Entrada {

        @Test
        @DisplayName("soma no saldo e guarda o saldo resultante na movimentacao")
        void somaNoSaldo() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("100"), new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            MovimentacaoResponse resposta = service.registrarEntrada(pedido(new BigDecimal("30")));

            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("130");
            assertThat(resposta.saldoApos()).isEqualByComparingTo("130");
            assertThat(resposta.tipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        }

        @Test
        @DisplayName("entrada e aceita mesmo com a pastilha zerada")
        void aceitaComSaldoZero() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", BigDecimal.ZERO, new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            assertThat(service.registrarEntrada(pedido(new BigDecimal("10"))).saldoApos())
                    .isEqualByComparingTo("10");
        }
    }

    @Nested
    @DisplayName("quem lancou")
    class QuemLancou {

        @AfterEach
        void sair() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("grava o usuario logado na movimentacao")
        void gravaUsuarioLogado() {
            Usuario usuario = Fabrica.comId(Fabrica.usuario("operador@dda.com.br"), 5L);
            UsuarioDetails logado = new UsuarioDetails(usuario);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(logado, null, logado.getAuthorities()));
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("100"), new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            MovimentacaoResponse resposta = service.registrarEntrada(pedido(new BigDecimal("10")));

            verify(repository).save(movimentacaoSalva.capture());
            assertThat(movimentacaoSalva.getValue().getUsuario()).isSameAs(usuario);
            assertThat(resposta.usuarioNome()).isEqualTo("Usuario de teste");
        }

        @Test
        @DisplayName("sem ninguem logado a movimentacao fica sem usuario")
        void semUsuario() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("100"), new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            assertThat(service.registrarEntrada(pedido(new BigDecimal("10"))).usuarioNome()).isNull();
        }
    }

    @Nested
    @DisplayName("saida")
    class Saida {

        @Test
        @DisplayName("subtrai do saldo quando tem estoque")
        void subtraiDoSaldo() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("100"), new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            MovimentacaoResponse resposta = service.registrarSaida(pedido(new BigDecimal("40")));

            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("60");
            assertThat(resposta.saldoApos()).isEqualByComparingTo("60");
            assertThat(resposta.tipo()).isEqualTo(TipoMovimentacao.SAIDA);
        }

        @Test
        @DisplayName("aceita a saida que zera exatamente o estoque")
        void aceitaZerarOEstoque() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("40"), new BigDecimal("20")), 1L);
            devolvePastilha(pastilha);

            assertThat(service.registrarSaida(pedido(new BigDecimal("40"))).saldoApos())
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("recusa a saida maior que o saldo e nao mexe no estoque")
        void recusaSaldoInsuficiente() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("10"), new BigDecimal("20")), 1L);
            when(pastilhaRepository.buscarParaMovimentar(1L)).thenReturn(Optional.of(pastilha));

            assertThatThrownBy(() -> service.registrarSaida(pedido(new BigDecimal("11"))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Saldo insuficiente");

            assertThat(pastilha.getSaldoAtual()).isEqualByComparingTo("10");
            verify(repository, never()).save(any());
            verify(pastilhaRepository, never()).save(any());
        }

        @Test
        @DisplayName("a mensagem de saldo insuficiente mostra os numeros sem zeros sobrando")
        void mensagemSemZerosSobrando() {
            Pastilha pastilha = Fabrica.comId(
                    Fabrica.pastilha("A1", new BigDecimal("4.000"), new BigDecimal("20")), 1L);
            when(pastilhaRepository.buscarParaMovimentar(1L)).thenReturn(Optional.of(pastilha));

            assertThatThrownBy(() -> service.registrarSaida(pedido(new BigDecimal("10.500"))))
                    .hasMessageContaining("tem 4 UN")
                    .hasMessageContaining("pede 10.5")
                    .hasMessageNotContaining("4.000");
        }
    }

    @Nested
    @DisplayName("regras comuns as duas")
    class RegrasComuns {

        @Test
        @DisplayName("devolve 404 quando a pastilha nao existe")
        void pastilhaInexistente() {
            when(pastilhaRepository.buscarParaMovimentar(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarEntrada(pedido(BigDecimal.TEN)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("nao encontrada");
        }

        @Test
        @DisplayName("pastilha inativa nao aceita entrada nem saida")
        void pastilhaInativa() {
            Pastilha pastilha = Fabrica.comId(Fabrica.pastilha("A1"), 1L);
            pastilha.inativar();
            when(pastilhaRepository.buscarParaMovimentar(1L)).thenReturn(Optional.of(pastilha));

            assertThatThrownBy(() -> service.registrarEntrada(pedido(BigDecimal.TEN)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativa");

            assertThatThrownBy(() -> service.registrarSaida(pedido(BigDecimal.ONE)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativa");
        }

        @Test
        @DisplayName("le a pastilha com trava, para duas saidas ao mesmo tempo nao zerarem o estoque")
        void usaLeituraComTrava() {
            Pastilha pastilha = Fabrica.comId(Fabrica.pastilha("A1"), 1L);
            devolvePastilha(pastilha);

            service.registrarSaida(pedido(BigDecimal.ONE));

            verify(pastilhaRepository).buscarParaMovimentar(1L);
            verify(pastilhaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("campo opcional vazio ou so com espacos e gravado como nulo")
        void limpaCamposOpcionais() {
            Pastilha pastilha = Fabrica.comId(Fabrica.pastilha("A1"), 1L);
            devolvePastilha(pastilha);

            service.registrarEntrada(new MovimentacaoRequest(1L, BigDecimal.TEN, "   ", "", null));

            verify(repository).save(movimentacaoSalva.capture());
            Movimentacao gravada = movimentacaoSalva.getValue();
            assertThat(gravada.getDocumento()).isNull();
            assertThat(gravada.getOrigemDestino()).isNull();
            assertThat(gravada.getObservacao()).isNull();
        }

        @Test
        @DisplayName("campo opcional preenchido perde so os espacos das pontas")
        void mantemCamposPreenchidos() {
            Pastilha pastilha = Fabrica.comId(Fabrica.pastilha("A1"), 1L);
            devolvePastilha(pastilha);

            service.registrarEntrada(new MovimentacaoRequest(
                    1L, BigDecimal.TEN, "  NF 999  ", "  Sandvik  ", "  chegou hoje  "));

            verify(repository).save(movimentacaoSalva.capture());
            Movimentacao gravada = movimentacaoSalva.getValue();
            assertThat(gravada.getDocumento()).isEqualTo("NF 999");
            assertThat(gravada.getOrigemDestino()).isEqualTo("Sandvik");
            assertThat(gravada.getObservacao()).isEqualTo("chegou hoje");
        }
    }

    @Nested
    @DisplayName("historico")
    class Historico {

        private final Pageable pagina = PageRequest.of(0, 20);

        @Test
        @DisplayName("sem filtro usa id zero e um periodo bem largo")
        void semFiltro() {
            when(repository.buscar(eq(0L), any(), any(), eq(pagina))).thenReturn(Page.empty(pagina));

            assertThat(service.historico(null, null, null, pagina)).isEmpty();

            ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(repository).buscar(eq(0L), inicio.capture(), fim.capture(), eq(pagina));
            assertThat(inicio.getValue()).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
            assertThat(fim.getValue()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("com datas usa o dia inteiro: comeca 00:00 e termina 23:59")
        void periodoCobreODiaInteiro() {
            when(repository.buscar(eq(7L), any(), any(), eq(pagina))).thenReturn(Page.empty(pagina));

            service.historico(7L, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), pagina);

            ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(repository).buscar(eq(7L), inicio.capture(), fim.capture(), eq(pagina));
            assertThat(inicio.getValue()).isEqualTo(LocalDateTime.of(2026, 1, 10, 0, 0));
            assertThat(fim.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 12));
            assertThat(fim.getValue().getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("recusa periodo invertido, com a data inicial depois da final")
        void periodoInvertido() {
            assertThatThrownBy(() -> service.historico(
                    null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), pagina))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("data inicial");

            verify(repository, never()).buscar(any(), any(), any(), any());
        }

        @Test
        @DisplayName("aceita o mesmo dia como inicio e fim")
        void mesmoDia() {
            when(repository.buscar(eq(0L), any(), any(), eq(pagina))).thenReturn(Page.empty(pagina));

            LocalDate hoje = LocalDate.of(2026, 2, 1);
            assertThat(service.historico(null, hoje, hoje, pagina)).isEmpty();
        }
    }
}
