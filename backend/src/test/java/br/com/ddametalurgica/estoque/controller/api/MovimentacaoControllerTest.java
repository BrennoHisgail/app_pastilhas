package br.com.ddametalurgica.estoque.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.config.SecurityConfig;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.exception.ApiExceptionHandler;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.service.MovimentacaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MovimentacaoController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@DisplayName("MovimentacaoController - contrato HTTP das entradas e saidas")
class MovimentacaoControllerTest {

    private static final String CORPO_VALIDO = """
            {"pastilhaId":1,"quantidade":25,"documento":"NF 12345",
             "origemDestino":"Fornecedor Sandvik","observacao":"recebido"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MovimentacaoService service;

    private MovimentacaoResponse resposta(TipoMovimentacao tipo, String saldoApos) {
        return new MovimentacaoResponse(
                10L, 1L, "CNMG120408", "Pastilha de torneamento", tipo,
                new BigDecimal("25"), new BigDecimal(saldoApos), LocalDateTime.now(),
                "NF 12345", "Fornecedor Sandvik", "recebido");
    }

    @Test
    @DisplayName("POST /entrada devolve 201 com o saldo resultante e o Location")
    void registraEntrada() throws Exception {
        when(service.registrarEntrada(any())).thenReturn(resposta(TipoMovimentacao.ENTRADA, "125"));

        mvc.perform(post("/api/movimentacoes/entrada")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/movimentacoes/10"))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.saldoApos").value(125));
    }

    @Test
    @DisplayName("POST /saida devolve 201 com o saldo resultante")
    void registraSaida() throws Exception {
        when(service.registrarSaida(any())).thenReturn(resposta(TipoMovimentacao.SAIDA, "75"));

        mvc.perform(post("/api/movimentacoes/saida")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.saldoApos").value(75));
    }

    @Test
    @DisplayName("saida sem saldo devolve 409 com a mensagem para o usuario")
    void saldoInsuficiente() throws Exception {
        when(service.registrarSaida(any())).thenThrow(new RegraNegocioException(
                "Saldo insuficiente: a pastilha CNMG120408 tem 10 UN em estoque, e a saida pede 25."));

        mvc.perform(post("/api/movimentacoes/saida")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negocio violada"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Saldo insuficiente")));
    }

    @Test
    @DisplayName("movimentacao em pastilha inexistente devolve 404")
    void pastilhaInexistente() throws Exception {
        when(service.registrarEntrada(any()))
                .thenThrow(new RecursoNaoEncontradoException("Pastilha 1 nao encontrada."));

        mvc.perform(post("/api/movimentacoes/entrada")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("quantidade zero ou negativa devolve 400 sem chamar o service")
    void recusaQuantidadeInvalida() throws Exception {
        for (String quantidade : List.of("0", "-5")) {
            mvc.perform(post("/api/movimentacoes/entrada")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pastilhaId\":1,\"quantidade\":" + quantidade + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros.quantidade").exists());
        }

        verify(service, never()).registrarEntrada(any());
    }

    @Test
    @DisplayName("requisicao sem pastilha e sem quantidade devolve 400 apontando os dois campos")
    void recusaCamposObrigatorios() throws Exception {
        mvc.perform(post("/api/movimentacoes/saida")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.pastilhaId").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists());
    }

    @Test
    @DisplayName("documento acima do tamanho permitido devolve 400")
    void recusaDocumentoLongoDemais() throws Exception {
        String documento = "X".repeat(61);

        mvc.perform(post("/api/movimentacoes/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pastilhaId\":1,\"quantidade\":1,\"documento\":\"" + documento + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.documento").exists());
    }

    @Test
    @DisplayName("GET /api/movimentacoes devolve o historico paginado")
    void historico() throws Exception {
        when(service.historico(any(), any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(resposta(TipoMovimentacao.ENTRADA, "125")), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].pastilhaCodigo").value("CNMG120408"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/movimentacoes repassa os filtros de pastilha e periodo")
    void historicoComFiltros() throws Exception {
        when(service.historico(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/movimentacoes")
                        .param("pastilhaId", "3").param("de", "2026-01-01").param("ate", "2026-01-31"))
                .andExpect(status().isOk());

        verify(service).historico(
                eq(3L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), any());
    }

    @Test
    @DisplayName("data em formato errado devolve 400, nao 500")
    void recusaDataInvalida() throws Exception {
        mvc.perform(get("/api/movimentacoes").param("de", "31/01/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"));
    }

    @Test
    @DisplayName("periodo invertido devolve 409 com a mensagem do service")
    void periodoInvertido() throws Exception {
        when(service.historico(any(), any(), any(), any()))
                .thenThrow(new RegraNegocioException("A data inicial nao pode ser posterior a data final."));

        mvc.perform(get("/api/movimentacoes").param("de", "2026-03-01").param("ate", "2026-02-01"))
                .andExpect(status().isConflict());
    }
}
