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
import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.dto.AlterarSenhaRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioCreateRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioResponse;
import br.com.ddametalurgica.estoque.dto.UsuarioUpdateRequest;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.repository.UsuarioRepository;
import br.com.ddametalurgica.estoque.security.UsuarioDetails;
import java.util.List;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - regras da gestao de usuarios")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UsuarioService service;

    @Captor
    private ArgumentCaptor<Usuario> usuarioSalvo;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UsuarioDetails(usuario), null, List.of()));
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("grava o usuario quando o email ainda nao existe")
        void criaComEmailNovo() {
            when(repository.existsByEmailIgnoreCase("nova@dda.com.br")).thenReturn(false);
            when(encoder.encode("senha12345")).thenReturn("hash-criptografado");
            when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            UsuarioResponse resposta = service.criar(
                    new UsuarioCreateRequest("Nova", "nova@dda.com.br", "senha12345", PerfilUsuario.OPERADOR));

            assertThat(resposta.email()).isEqualTo("nova@dda.com.br");
            assertThat(resposta.perfil()).isEqualTo(PerfilUsuario.OPERADOR);
        }

        @Test
        @DisplayName("a senha e criptografada antes de gravar, nunca em texto puro")
        void criptografaASenha() {
            when(repository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(encoder.encode("senha12345")).thenReturn("hash-criptografado");
            when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            service.criar(new UsuarioCreateRequest("Nova", "nova@dda.com.br", "senha12345", PerfilUsuario.OPERADOR));

            verify(repository).save(usuarioSalvo.capture());
            assertThat(usuarioSalvo.getValue().getSenhaHash()).isEqualTo("hash-criptografado");
        }

        @Test
        @DisplayName("recusa email repetido, mesmo escrito em outra caixa")
        void recusaEmailDuplicado() {
            when(repository.existsByEmailIgnoreCase("ADMIN@dda.com.br")).thenReturn(true);

            assertThatThrownBy(() -> service.criar(
                    new UsuarioCreateRequest("Outra", "ADMIN@dda.com.br", "senha12345", PerfilUsuario.ADMIN)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Ja existe um usuario");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("atualizar e alterar senha")
    class Atualizar {

        @Test
        @DisplayName("atualizar troca nome e perfil, sem mexer no email")
        void atualizaNomeEPerfil() {
            Usuario existente = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br", PerfilUsuario.OPERADOR), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            UsuarioResponse resposta = service.atualizar(1L, new UsuarioUpdateRequest("Fulana Nova", PerfilUsuario.ADMIN));

            assertThat(resposta.nome()).isEqualTo("Fulana Nova");
            assertThat(resposta.perfil()).isEqualTo(PerfilUsuario.ADMIN);
            assertThat(resposta.email()).isEqualTo("fulana@dda.com.br");
        }

        @Test
        @DisplayName("devolve 404 quando o id nao existe")
        void idInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(99L, new UsuarioUpdateRequest("X", PerfilUsuario.OPERADOR)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("alterarSenha grava o novo hash")
        void alteraSenha() {
            Usuario existente = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(encoder.encode("novaSenha123")).thenReturn("novo-hash");
            when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            service.alterarSenha(1L, new AlterarSenhaRequest("novaSenha123"));

            verify(repository).save(usuarioSalvo.capture());
            assertThat(usuarioSalvo.getValue().getSenhaHash()).isEqualTo("novo-hash");
        }
    }

    @Nested
    @DisplayName("inativar e reativar")
    class Ciclo {

        @Test
        @DisplayName("inativar marca o usuario como inativo em vez de apagar")
        void inativaSemApagar() {
            Usuario existente = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br"), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.inativar(1L);

            verify(repository).save(usuarioSalvo.capture());
            assertThat(usuarioSalvo.getValue().isAtivo()).isFalse();
        }

        @Test
        @DisplayName("recusa inativar um usuario que ja esta inativo")
        void recusaInativarDuasVezes() {
            Usuario existente = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br"), 1L);
            existente.inativar();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ja esta inativo");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("recusa inativar o proprio usuario logado")
        void recusaInativarAsiMesmo() {
            Usuario euMesmo = Fabrica.comId(Fabrica.usuario("eu@dda.com.br", PerfilUsuario.ADMIN), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(euMesmo));
            autenticarComo(euMesmo);

            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("proprio usuario");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("recusa inativar o ultimo administrador ativo")
        void recusaInativarUltimoAdmin() {
            Usuario ultimoAdmin = Fabrica.comId(Fabrica.usuario("admin@dda.com.br", PerfilUsuario.ADMIN), 1L);
            when(repository.findById(1L)).thenReturn(Optional.of(ultimoAdmin));
            when(repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ultimo administrador");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deixa inativar um admin quando existe outro admin ativo")
        void permiteInativarAdminComOutroDisponivel() {
            Usuario admin = Fabrica.comId(Fabrica.usuario("admin2@dda.com.br", PerfilUsuario.ADMIN), 2L);
            when(repository.findById(2L)).thenReturn(Optional.of(admin));
            when(repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).thenReturn(2L);

            service.inativar(2L);

            verify(repository).save(usuarioSalvo.capture());
            assertThat(usuarioSalvo.getValue().isAtivo()).isFalse();
        }

        @Test
        @DisplayName("reativar volta o usuario para ativo")
        void reativa() {
            Usuario existente = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br"), 1L);
            existente.inativar();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

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
            when(repository.buscar(eq(""), anyBoolean(), eq(pagina))).thenReturn(Page.empty(pagina));

            Page<UsuarioResponse> resultado = service.listar(null, true, pagina);

            assertThat(resultado).isEmpty();
            verify(repository).buscar("", true, pagina);
        }

        @Test
        @DisplayName("filtro com espacos e limpo antes de ir para a consulta")
        void normalizaFiltrosComEspacos() {
            Pageable pagina = PageRequest.of(0, 20);
            Usuario encontrado = Fabrica.comId(Fabrica.usuario("fulana@dda.com.br"), 1L);
            when(repository.buscar("fulana", true, pagina)).thenReturn(new PageImpl<>(List.of(encontrado), pagina, 1));

            Page<UsuarioResponse> resultado = service.listar("  fulana  ", true, pagina);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getContent().get(0).email()).isEqualTo("fulana@dda.com.br");
        }

        @Test
        @DisplayName("buscarPorId devolve 404 quando nao acha")
        void buscarPorIdInexistente() {
            when(repository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(42L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }
}
