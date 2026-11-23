package br.com.ddametalurgica.estoque.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UsuarioDetails")
class UsuarioDetailsTest {

    // A sessao de login e gravada no banco convertida em bytes. Se o
    // UsuarioDetails (ou o Usuario dentro dele) deixar de ser Serializable,
    // o login quebra; este teste avisa antes.
    @Test
    @DisplayName("vira bytes e volta igual, para poder ser gravado na sessão do banco")
    void serializavel() throws Exception {
        UsuarioDetails original = new UsuarioDetails(
                Fabrica.comId(Fabrica.usuario("admin@dda.com.br", PerfilUsuario.ADMIN), 7L));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream saida = new ObjectOutputStream(bytes)) {
            saida.writeObject(original);
        }

        UsuarioDetails lido;
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            lido = (UsuarioDetails) entrada.readObject();
        }

        assertThat(lido.getUsuario().getId()).isEqualTo(7L);
        assertThat(lido.getUsername()).isEqualTo("admin@dda.com.br");
        assertThat(lido.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }
}
