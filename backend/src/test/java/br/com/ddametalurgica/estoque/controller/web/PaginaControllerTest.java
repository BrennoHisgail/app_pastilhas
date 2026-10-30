package br.com.ddametalurgica.estoque.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.ddametalurgica.estoque.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
}
