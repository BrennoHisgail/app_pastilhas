package br.com.ddametalurgica.estoque.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.config.SecurityConfig;
import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import br.com.ddametalurgica.estoque.dto.PastilhaRequest;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.exception.ApiExceptionHandler;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.service.PastilhaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PastilhaController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@DisplayName("PastilhaController - contrato HTTP do cadastro")
@WithMockUser(roles = "ADMIN")
class PastilhaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private PastilhaService service;

    private PastilhaResponse resposta(Long id, String codigo) {
        return new PastilhaResponse(
                id, codigo, "Pastilha de teste", "Torneamento", UnidadeMedida.UN,
                new BigDecimal("20"), new BigDecimal("100"), new BigDecimal("10.00"),
                new BigDecimal("1000.00"), 7, new BigDecimal("5"),
                false, true, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("GET /api/pastilhas devolve a pagina com os filtros repassados")
    void lista() throws Exception {
        Page<PastilhaResponse> pagina = new PageImpl<>(
                List.of(resposta(1L, "CNMG120408")), PageRequest.of(0, 20), 1);
        when(service.listar(anyString(), anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(pagina);

        mvc.perform(get("/api/pastilhas").param("busca", "cnmg").param("categoria", "Torneamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("CNMG120408"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).listar(eq("cnmg"), eq("Torneamento"), eq(true), eq(false), any());
    }

    @Test
    @DisplayName("GET /api/pastilhas/categorias devolve a lista de categorias")
    void categorias() throws Exception {
        when(service.listarCategorias()).thenReturn(List.of("Fresamento", "Torneamento"));

        mvc.perform(get("/api/pastilhas/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Fresamento"));
    }

    @Test
    @DisplayName("GET /api/pastilhas/{id} inexistente devolve 404 com a mensagem do service")
    void buscarInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Pastilha 99 nao encontrada."));

        mvc.perform(get("/api/pastilhas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
                .andExpect(jsonPath("$.detail").value("Pastilha 99 nao encontrada."));
    }

    @Test
    @DisplayName("GET /api/pastilhas/{id} com id que nao e numero devolve 400")
    void idNaoNumerico() throws Exception {
        mvc.perform(get("/api/pastilhas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"));
    }

    @Test
    @DisplayName("POST /api/pastilhas devolve 201 com o cabecalho Location")
    void criaComLocation() throws Exception {
        when(service.criar(any())).thenReturn(resposta(7L, "CNMG120408"));

        mvc.perform(post("/api/pastilhas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao("CNMG120408"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/pastilhas/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("POST com campos faltando devolve 400 listando cada campo")
    void recusaCamposFaltando() throws Exception {
        PastilhaRequest vazia = new PastilhaRequest(" ", "", null, null, null, null, null, null);

        mvc.perform(post("/api/pastilhas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(vazia)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros.codigo").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.categoria").exists())
                .andExpect(jsonPath("$.erros.unidadeMedida").exists());

        verify(service, never()).criar(any());
    }

    @Test
    @DisplayName("POST com valor negativo devolve 400")
    void recusaValorNegativo() throws Exception {
        PastilhaRequest negativa = new PastilhaRequest(
                "X1", "Teste", "Torneamento", UnidadeMedida.UN,
                new BigDecimal("-1"), new BigDecimal("-2"), -3, new BigDecimal("-4"));

        mvc.perform(post("/api/pastilhas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(negativa)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nivelMinimo").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists())
                .andExpect(jsonPath("$.erros.leadTimeDias").exists())
                .andExpect(jsonPath("$.erros.estoqueSeguranca").exists());
    }

    @Test
    @DisplayName("POST com codigo repetido devolve 409")
    void recusaCodigoDuplicado() throws Exception {
        when(service.criar(any())).thenThrow(new RegraNegocioException("Ja existe uma pastilha com o codigo X1."));

        mvc.perform(post("/api/pastilhas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao("X1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negocio violada"))
                .andExpect(jsonPath("$.detail").value("Ja existe uma pastilha com o codigo X1."));
    }

    @Test
    @DisplayName("POST com unidade de medida inexistente devolve 400, nao 500")
    void recusaUnidadeInvalida() throws Exception {
        String corpo = """
                {"codigo":"X1","descricao":"Teste","categoria":"Torneamento",
                 "unidadeMedida":"TONELADA","nivelMinimo":1,"custoUnitario":1,
                 "leadTimeDias":1,"estoqueSeguranca":1}
                """;

        mvc.perform(post("/api/pastilhas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"));
    }

    @Test
    @DisplayName("POST com JSON quebrado devolve 400, nao 500")
    void recusaJsonQuebrado() throws Exception {
        mvc.perform(post("/api/pastilhas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/pastilhas/{id} devolve 200 com a pastilha alterada")
    void atualiza() throws Exception {
        when(service.atualizar(eq(1L), any())).thenReturn(resposta(1L, "CNMG120408"));

        mvc.perform(put("/api/pastilhas/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao("CNMG120408"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CNMG120408"));
    }

    @Test
    @DisplayName("PATCH nivel-minimo altera so o nivel minimo")
    void alteraNivelMinimo() throws Exception {
        when(service.alterarNivelMinimo(eq(1L), any(BigDecimal.class))).thenReturn(resposta(1L, "CNMG120408"));

        mvc.perform(patch("/api/pastilhas/1/nivel-minimo").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nivelMinimo\": 150}"))
                .andExpect(status().isOk());

        verify(service).alterarNivelMinimo(1L, new BigDecimal("150"));
    }

    @Test
    @DisplayName("PATCH nivel-minimo negativo devolve 400")
    void recusaNivelMinimoNegativo() throws Exception {
        mvc.perform(patch("/api/pastilhas/1/nivel-minimo").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nivelMinimo\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nivelMinimo").exists());

        verify(service, never()).alterarNivelMinimo(any(), any());
    }

    @Test
    @DisplayName("DELETE inativa e devolve 204 sem corpo")
    void inativa() throws Exception {
        mvc.perform(delete("/api/pastilhas/1").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).inativar(1L);
    }

    @Test
    @DisplayName("DELETE em pastilha ja inativa devolve 409")
    void recusaInativarDuasVezes() throws Exception {
        Mockito.doThrow(new RegraNegocioException("A pastilha X1 ja esta inativa."))
                .when(service).inativar(1L);

        mvc.perform(delete("/api/pastilhas/1").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH reativar devolve a pastilha ativa")
    void reativa() throws Exception {
        when(service.reativar(1L)).thenReturn(resposta(1L, "CNMG120408"));

        mvc.perform(patch("/api/pastilhas/1/reativar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("OPERADOR consulta as pastilhas normalmente")
    void operadorConsulta() throws Exception {
        when(service.listarCategorias()).thenReturn(List.of("Torneamento"));

        mvc.perform(get("/api/pastilhas/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("OPERADOR nao pode cadastrar pastilha: 403")
    void operadorNaoCadastra() throws Exception {
        mvc.perform(post("/api/pastilhas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao("CNMG120408"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));

        verify(service, never()).criar(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("sem login a API devolve 401 em vez de mandar para a tela de login")
    void semLoginDevolve401() throws Exception {
        mvc.perform(get("/api/pastilhas"))
                .andExpect(status().isUnauthorized());
    }
}
