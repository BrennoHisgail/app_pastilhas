package br.com.ddametalurgica.estoque.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Testa as consultas escritas na mao do UsuarioRepository contra um
 * banco de verdade (H2 em memoria).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UsuarioRepository - consultas da gestao de usuarios")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TestEntityManager em;

    private final Pageable primeiraPagina = PageRequest.of(0, 20, Sort.by("nome"));

    @BeforeEach
    void carregarMassa() {
        salvar("Administradora Demo", "admin@dda.com.br", PerfilUsuario.ADMIN, true);
        salvar("Operador Demo", "operador@dda.com.br", PerfilUsuario.OPERADOR, true);
        salvar("Operador Antigo", "antigo@dda.com.br", PerfilUsuario.OPERADOR, false);
        em.flush();
        em.clear();
    }

    private void salvar(String nome, String email, PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = Fabrica.usuario(email, perfil);
        usuario.setNome(nome);
        if (!ativo) {
            usuario.inativar();
        }
        em.persist(usuario);
    }

    @Test
    @DisplayName("findByEmailIgnoreCase acha o usuario sem diferenciar maiuscula de minuscula")
    void achaPeloEmail() {
        assertThat(repository.findByEmailIgnoreCase("ADMIN@dda.com.br"))
                .get()
                .extracting(Usuario::getNome)
                .isEqualTo("Administradora Demo");
    }

    @Test
    @DisplayName("email duplicado e detectado ignorando a caixa das letras")
    void detectaEmailDuplicado() {
        assertThat(repository.existsByEmailIgnoreCase("admin@dda.com.br")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("nao-existe@dda.com.br")).isFalse();
    }

    @Test
    @DisplayName("busca vazia traz todos os ativos")
    void buscaVaziaTrazTodosOsAtivos() {
        Page<Usuario> pagina = repository.buscar("", true, primeiraPagina);

        assertThat(pagina.getContent())
                .extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("admin@dda.com.br", "operador@dda.com.br");
    }

    @Test
    @DisplayName("ativo=false lista so os inativados")
    void listaInativos() {
        Page<Usuario> pagina = repository.buscar("", false, primeiraPagina);

        assertThat(pagina.getContent()).extracting(Usuario::getEmail).containsExactly("antigo@dda.com.br");
    }

    @Test
    @DisplayName("busca acha por trecho do nome ou do email")
    void buscaPorNomeOuEmail() {
        assertThat(repository.buscar("administradora", true, primeiraPagina).getContent())
                .extracting(Usuario::getEmail).containsExactly("admin@dda.com.br");
        assertThat(repository.buscar("operador@dda", true, primeiraPagina).getContent())
                .extracting(Usuario::getEmail).containsExactly("operador@dda.com.br");
    }

    @Test
    @DisplayName("conta so os administradores ativos")
    void contaAdministradoresAtivos() {
        assertThat(repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).isEqualTo(1);
        assertThat(repository.countByPerfilAndAtivoTrue(PerfilUsuario.OPERADOR)).isEqualTo(1);
    }
}
