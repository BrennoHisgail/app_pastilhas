package br.com.ddametalurgica.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

// A sessao de login fica no banco (Spring Session JDBC), nao na memoria
// da instancia. E isso que permite ter duas instancias atras do NGINX:
// se uma cair, a outra le a sessao do banco e o usuario continua logado.
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sessão de login gravada no banco")
class SessaoNoBancoTest {

    private static final String EMAIL = "sessao@dda.com.br";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarUsuario() {
        if (usuarioRepository.findByEmailIgnoreCase(EMAIL).isEmpty()) {
            Usuario usuario = Fabrica.usuario(EMAIL, PerfilUsuario.OPERADOR);
            usuario.definirSenha(passwordEncoder.encode(SENHA));
            usuarioRepository.save(usuario);
        }
    }

    @Test
    @DisplayName("o login grava a sessão no banco e o cookie basta para continuar logado")
    void loginFicaNoBanco() throws Exception {
        Cookie cookie = mvc.perform(formLogin("/login").user(EMAIL).password(SENHA))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getCookie("JSESSIONID");

        assertThat(cookie).as("cookie de sessão devolvido no login").isNotNull();

        // a sessao do usuario esta gravada no banco
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM spring_session WHERE principal_name = ?", Integer.class, EMAIL))
                .isPositive();

        // a proxima requisicao leva so o cookie: o login e recuperado do banco,
        // como aconteceria se ela caisse na outra instancia
        mvc.perform(get("/api/pastilhas").cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem o cookie de sessão a API continua respondendo 401")
    void semCookieNaoEntra() throws Exception {
        mvc.perform(get("/api/pastilhas"))
                .andExpect(status().isUnauthorized());
    }
}
