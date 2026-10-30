package br.com.ddametalurgica.estoque.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.ddametalurgica.estoque.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confere que cada rota das telas aponta para o HTML certo. Como as
 * paginas buscam os dados pela API, nao ha nada mais para testar aqui.
 *
 * Roda como ADMIN porque cadastro e edicao de pastilha sao so desse perfil.
 */
@WebMvcTest(PaginaController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@DisplayName("PaginaController - rotas das telas")
class PaginaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("a raiz abre o painel")
    void painel() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("painel"));
    }

    @Test
    @DisplayName("/pastilhas abre a lista")
    void listaDePastilhas() throws Exception {
        mvc.perform(get("/pastilhas"))
                .andExpect(status().isOk())
                .andExpect(view().name("pastilhas/lista"));
    }

    @Test
    @DisplayName("/pastilhas/nova abre o formulario sem id")
    void formularioDeCadastro() throws Exception {
        mvc.perform(get("/pastilhas/nova"))
                .andExpect(status().isOk())
                .andExpect(view().name("pastilhas/form"))
                .andExpect(model().attribute("pastilhaId", ""));
    }

    @Test
    @DisplayName("/pastilhas/{id}/editar abre o formulario com o id da pastilha")
    void formularioDeEdicao() throws Exception {
        mvc.perform(get("/pastilhas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("pastilhas/form"))
                .andExpect(model().attribute("pastilhaId", 7L));
    }

    @Test
    @DisplayName("/movimentacoes abre a tela de entradas e saidas")
    void telaDeMovimentacoes() throws Exception {
        mvc.perform(get("/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("movimentacoes"));
    }

    @Test
    @DisplayName("/login abre a tela de login sem precisar estar logado")
    @WithAnonymousUser
    void telaDeLogin() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("sem login as telas mandam para o /login")
    @WithAnonymousUser
    void semLoginRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/pastilhas").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("/indicadores abre a tela de indicadores")
    @WithMockUser(roles = "OPERADOR")
    void telaDeIndicadores() throws Exception {
        mvc.perform(get("/indicadores"))
                .andExpect(status().isOk())
                .andExpect(view().name("indicadores"));
    }

    @Test
    @DisplayName("/usuarios abre a lista de usuarios")
    void listaDeUsuarios() throws Exception {
        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/lista"));
    }

    @Test
    @DisplayName("/usuarios/{id}/editar abre o formulario com o id do usuario")
    void formularioDeUsuario() throws Exception {
        mvc.perform(get("/usuarios/3/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/form"))
                .andExpect(model().attribute("usuarioId", 3L));
    }

    @Test
    @DisplayName("OPERADOR nao abre as telas de usuario nem o cadastro de pastilha")
    @WithMockUser(roles = "OPERADOR")
    void operadorNaoAbreTelasDeAdmin() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/pastilhas/nova")).andExpect(status().isForbidden());
    }
}
