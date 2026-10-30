package br.com.ddametalurgica.estoque.controller.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.config.SecurityConfig;
import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import br.com.ddametalurgica.estoque.dto.IndicadoresResponse;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.exception.ApiExceptionHandler;
import br.com.ddametalurgica.estoque.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(roles = "OPERADOR")
@DisplayName("DashboardController - contrato HTTP do painel")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    private PastilhaResponse emFalta() {
        return new PastilhaResponse(
                1L, "WNMG080408", "Pastilha revestida", "Torneamento", UnidadeMedida.UN,
                new BigDecimal("40"), new BigDecimal("18"), new BigDecimal("42.50"),
                new BigDecimal("765.00"), 10, new BigDecimal("15"),
                true, true, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("GET /api/dashboard/indicadores devolve todos os numeros do painel")
    void indicadores() throws Exception {
        when(service.indicadores()).thenReturn(new IndicadoresResponse(
                12L, 5L, 4L, new BigDecimal("1234.56"), 87L, List.of(emFalta()), List.of()));

        mvc.perform(get("/api/dashboard/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itensCadastrados").value(12))
                .andExpect(jsonPath("$.categorias").value(5))
                .andExpect(jsonPath("$.itensAbaixoDoMinimo").value(4))
                .andExpect(jsonPath("$.valorEmEstoque").value(1234.56))
                .andExpect(jsonPath("$.movimentacoesUltimos30Dias").value(87))
                .andExpect(jsonPath("$.alertasReposicao[0].codigo").value("WNMG080408"))
                .andExpect(jsonPath("$.alertasReposicao[0].abaixoDoMinimo").value(true))
                .andExpect(jsonPath("$.movimentacoesRecentes").isEmpty());
    }

    @Test
    @DisplayName("GET /api/alertas/reposicao usa o limite 50 quando nao informam")
    void limitePadrao() throws Exception {
        when(service.alertasReposicao(50)).thenReturn(List.of(emFalta()));

        mvc.perform(get("/api/alertas/reposicao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).alertasReposicao(50);
    }

    @Test
    @DisplayName("GET /api/alertas/reposicao respeita o limite informado")
    void limiteInformado() throws Exception {
        when(service.alertasReposicao(5)).thenReturn(List.of());

        mvc.perform(get("/api/alertas/reposicao").param("limite", "5"))
                .andExpect(status().isOk());

        verify(service).alertasReposicao(5);
    }

    @Test
    @DisplayName("limite zero ou negativo devolve 400 em vez de estourar erro 500")
    void recusaLimiteInvalido() throws Exception {
        for (String limite : List.of("0", "-1")) {
            mvc.perform(get("/api/alertas/reposicao").param("limite", limite))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados invalidos"))
                    .andExpect(jsonPath("$.erros.limite").exists());
        }

        verify(service, never()).alertasReposicao(ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("limite absurdo devolve 400, para nao puxar o catalogo inteiro")
    void recusaLimiteGrandeDemais() throws Exception {
        mvc.perform(get("/api/alertas/reposicao").param("limite", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.limite").exists());
    }

    @Test
    @DisplayName("limite que nao e numero devolve 400")
    void recusaLimiteNaoNumerico() throws Exception {
        mvc.perform(get("/api/alertas/reposicao").param("limite", "muitos"))
                .andExpect(status().isBadRequest());
    }
}
