package br.com.ddametalurgica.estoque.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.config.SecurityConfig;
import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.dto.AlterarSenhaRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioCreateRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioResponse;
import br.com.ddametalurgica.estoque.dto.UsuarioUpdateRequest;
import br.com.ddametalurgica.estoque.exception.ApiExceptionHandler;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
@DisplayName("UsuarioController - contrato HTTP da gestao de usuarios")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private UsuarioService service;

    private UsuarioResponse resposta(Long id, String email) {
        return new UsuarioResponse(id, "Fulana", email, PerfilUsuario.OPERADOR, true, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("GET /api/usuarios devolve a pagina com os filtros repassados")
    void lista() throws Exception {
        Page<UsuarioResponse> pagina = new PageImpl<>(
                List.of(resposta(1L, "fulana@dda.com.br")), PageRequest.of(0, 20), 1);
        when(service.listar(anyString(), anyBoolean(), any())).thenReturn(pagina);

        mvc.perform(get("/api/usuarios").param("busca", "fulana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("fulana@dda.com.br"));

        verify(service).listar(eq("fulana"), eq(true), any());
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} inexistente devolve 404")
    void buscarInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Usuario 99 nao encontrado."));

        mvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuario 99 nao encontrado."));
    }

    @Test
    @DisplayName("POST /api/usuarios devolve 201 com o cabecalho Location")
    void criaComLocation() throws Exception {
        when(service.criar(any())).thenReturn(resposta(7L, "nova@dda.com.br"));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new UsuarioCreateRequest("Nova", "nova@dda.com.br", "senha12345", PerfilUsuario.OPERADOR))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/usuarios/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("POST com senha curta devolve 400")
    void recusaSenhaCurta() throws Exception {
        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new UsuarioCreateRequest("Nova", "nova@dda.com.br", "123", PerfilUsuario.OPERADOR))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());

        verify(service, never()).criar(any());
    }

    @Test
    @DisplayName("POST com email repetido devolve 409")
    void recusaEmailDuplicado() throws Exception {
        when(service.criar(any())).thenThrow(new RegraNegocioException("Ja existe um usuario com o email x@dda.com.br."));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new UsuarioCreateRequest("X", "x@dda.com.br", "senha12345", PerfilUsuario.OPERADOR))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negocio violada"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id} devolve 200 com o usuario alterado")
    void atualiza() throws Exception {
        when(service.atualizar(eq(1L), any())).thenReturn(resposta(1L, "fulana@dda.com.br"));

        mvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UsuarioUpdateRequest("Fulana", PerfilUsuario.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("fulana@dda.com.br"));
    }

    @Test
    @DisplayName("PATCH senha devolve 204")
    void alteraSenha() throws Exception {
        mvc.perform(patch("/api/usuarios/1/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AlterarSenhaRequest("novaSenha123"))))
                .andExpect(status().isNoContent());

        verify(service).alterarSenha(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} devolve 204")
    void inativa() throws Exception {
        mvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(service).inativar(1L);
    }

    @Test
    @DisplayName("DELETE que viola regra de negocio devolve 409")
    void inativacaoRecusada() throws Exception {
        org.mockito.Mockito.doThrow(new RegraNegocioException("Voce nao pode inativar o proprio usuario."))
                .when(service).inativar(1L);

        mvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Voce nao pode inativar o proprio usuario."));
    }

    @Test
    @DisplayName("PATCH reativar devolve 200")
    void reativa() throws Exception {
        when(service.reativar(1L)).thenReturn(resposta(1L, "fulana@dda.com.br"));

        mvc.perform(patch("/api/usuarios/1/reativar"))
                .andExpect(status().isOk());
    }
}
