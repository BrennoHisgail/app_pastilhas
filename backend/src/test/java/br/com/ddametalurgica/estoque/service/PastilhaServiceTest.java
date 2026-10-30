package br.com.ddametalurgica.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.dto.PastilhaRequest;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("PastilhaService - regras do cadastro")
class PastilhaServiceTest {

    @Mock
    private PastilhaRepository repository;

    @InjectMocks
    private PastilhaService service;

    @Captor
    private ArgumentCaptor<Pastilha> pastilhaSalva;

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("grava a pastilha quando o codigo ainda nao existe")
        void criaComCodigoNovo() {
            when(repository.existsByCodigoIgnoreCase("CNMG120408")).thenReturn(false);
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            PastilhaResponse resposta = service.criar(Fabrica.requisicao("CNMG120408"));

            assertThat(resposta.codigo()).isEqualTo("CNMG120408");
            assertThat(resposta.saldoAtual()).isEqualByComparingTo("0");
            assertThat(resposta.ativo()).isTrue();
        }

        @Test
        @DisplayName("recusa codigo repetido, mesmo escrito em outra caixa")
        void recusaCodigoDuplicado() {
            when(repository.existsByCodigoIgnoreCase("cnmg120408")).thenReturn(true);

            assertThatThrownBy(() -> service.criar(Fabrica.requisicao("cnmg120408")))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Ja existe uma pastilha");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("tira os espacos das pontas dos campos de texto")
        void limpaEspacos() {
            when(repository.existsByCodigoIgnoreCase("CNMG120408")).thenReturn(false);
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            PastilhaRequest comEspacos = new PastilhaRequest(
                    "  CNMG120408  ", "  Pastilha de torneamento  ", "  Torneamento  ",
                    UnidadeMedida.UN, new BigDecimal("20"), new BigDecimal("10.00"), 7, new BigDecimal("5"));

            PastilhaResponse resposta = service.criar(comEspacos);

            assertThat(resposta.codigo()).isEqualTo("CNMG120408");
            assertThat(resposta.descricao()).isEqualTo("Pastilha de torneamento");
            assertThat(resposta.categoria()).isEqualTo("Torneamento");
        }

        @Test
        @DisplayName("a pastilha nova nasce sem saldo: saldo so entra por movimentacao")
        void nasceSemSaldo() {
            when(repository.existsByCodigoIgnoreCase(anyString())).thenReturn(false);
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            service.criar(Fabrica.requisicao("NOVA1"));

            verify(repository).save(pastilhaSalva.capture());
            assertThat(pastilhaSalva.getValue().getSaldoAtual()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("deixa manter o proprio codigo sem reclamar de duplicidade")
        void mantemOProprioCodigo() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            PastilhaResponse resposta = service.atualizar(1L, Fabrica.requisicao("CNMG120408"));

            assertThat(resposta.codigo()).isEqualTo("CNMG120408");
            verify(repository, never()).existsByCodigoIgnoreCase(anyString());
        }

        @Test
        @DisplayName("recusa trocar para um codigo que ja e de outra pastilha")
        void recusaCodigoDeOutra() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.existsByCodigoIgnoreCase("WNMG080408")).thenReturn(true);

            assertThatThrownBy(() -> service.atualizar(1L, Fabrica.requisicao("WNMG080408")))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("WNMG080408");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("editar o cadastro nao mexe no saldo")
        void naoMexeNoSaldo() {
            Pastilha existente = Fabrica.comId(
                    Fabrica.pastilha("CNMG120408", new BigDecimal("77"), new BigDecimal("20")), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            PastilhaResponse resposta = service.atualizar(1L, Fabrica.requisicao("CNMG120408"));

            assertThat(resposta.saldoAtual()).isEqualByComparingTo("77");
        }

        @Test
        @DisplayName("devolve 404 quando o id nao existe")
        void idInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(99L, Fabrica.requisicao("X1")))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("inativar e reativar")
    class Ciclo {

        @Test
        @DisplayName("inativar marca a pastilha como inativa em vez de apagar")
        void inativaSemApagar() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.inativar(1L);

            verify(repository).save(pastilhaSalva.capture());
            assertThat(pastilhaSalva.getValue().isAtivo()).isFalse();
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("recusa inativar uma pastilha que ja esta inativa")
        void recusaInativarDuasVezes() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            existente.inativar();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ja esta inativa");
        }

        @Test
        @DisplayName("reativar volta a pastilha para ativa")
        void reativa() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            existente.inativar();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            assertThat(service.reativar(1L).ativo()).isTrue();
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("filtro nulo vira string vazia, que a consulta entende como sem filtro")
        void normalizaFiltrosNulos() {
            Pageable pagina = PageRequest.of(0, 20);
            when(repository.buscar(eq(""), eq(""), anyBoolean(), anyBoolean(), eq(pagina)))
                    .thenReturn(Page.empty(pagina));

            Page<PastilhaResponse> resultado = service.listar(null, null, true, false, pagina);

            assertThat(resultado).isEmpty();
            verify(repository).buscar("", "", true, false, pagina);
        }

        @Test
        @DisplayName("filtro com espacos e limpo antes de ir para a consulta")
        void normalizaFiltrosComEspacos() {
            Pageable pagina = PageRequest.of(0, 20);
            Pastilha encontrada = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            when(repository.buscar("cnmg", "Torneamento", true, false, pagina))
                    .thenReturn(new PageImpl<>(List.of(encontrada), pagina, 1));

            Page<PastilhaResponse> resultado = service.listar("  cnmg  ", " Torneamento ", true, false, pagina);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getContent().get(0).codigo()).isEqualTo("CNMG120408");
        }

        @Test
        @DisplayName("buscarPorId devolve 404 quando nao acha")
        void buscarPorIdInexistente() {
            when(repository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(42L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("alterarNivelMinimo grava o novo valor")
        void alteraNivelMinimo() {
            Pastilha existente = Fabrica.comId(Fabrica.pastilha("CNMG120408"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Pastilha.class))).thenAnswer(chamada -> chamada.getArgument(0));

            PastilhaResponse resposta = service.alterarNivelMinimo(1L, new BigDecimal("150"));

            assertThat(resposta.nivelMinimo()).isEqualByComparingTo("150");
            // saldo 100 contra nivel 150: agora entra na lista de reposicao
            assertThat(resposta.abaixoDoMinimo()).isTrue();
        }

        @Test
        @DisplayName("listarCategorias repassa o que o banco devolveu")
        void listaCategorias() {
            when(repository.listarCategorias()).thenReturn(List.of("Fresamento", "Torneamento"));

            assertThat(service.listarCategorias()).containsExactly("Fresamento", "Torneamento");
        }
    }
}
