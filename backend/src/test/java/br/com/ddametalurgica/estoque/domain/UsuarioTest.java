package br.com.ddametalurgica.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Usuario - regras que ficam na propria entidade")
class UsuarioTest {

    @Test
    @DisplayName("usuario novo nasce ativo")
    void nasceAtivo() {
        assertThat(new Usuario().isAtivo()).isTrue();
    }

    @Test
    @DisplayName("inativar e reativar mudam so a flag")
    void inativarEReativar() {
        Usuario usuario = Fabrica.usuario("teste@dda.com.br");
        assertThat(usuario.isAtivo()).isTrue();

        usuario.inativar();
        assertThat(usuario.isAtivo()).isFalse();

        usuario.reativar();
        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("definirSenha grava o hash recebido, sem alterar")
    void definirSenha() {
        Usuario usuario = Fabrica.usuario("teste@dda.com.br");

        usuario.definirSenha("$2a$10$hashQualquer");

        assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$hashQualquer");
    }

    @Test
    @DisplayName("o perfil admin e distinto do operador")
    void perfis() {
        Usuario admin = Fabrica.usuario("admin@dda.com.br", PerfilUsuario.ADMIN);
        Usuario operador = Fabrica.usuario("operador@dda.com.br", PerfilUsuario.OPERADOR);

        assertThat(admin.getPerfil()).isEqualTo(PerfilUsuario.ADMIN);
        assertThat(operador.getPerfil()).isEqualTo(PerfilUsuario.OPERADOR);
    }
}
