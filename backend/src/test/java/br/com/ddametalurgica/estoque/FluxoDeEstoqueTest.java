package br.com.ddametalurgica.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de ponta a ponta: sobe a aplicacao inteira e faz as chamadas
 * HTTP de verdade, passando por controller, service, repositorio e banco.
 *
 * E o teste que confere se o saldo fecha no final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Fluxo completo - do cadastro ao painel")
class FluxoDeEstoqueTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private PastilhaRepository pastilhaRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @BeforeEach
    void limparBanco() {
        movimentacaoRepository.deleteAll();
        pastilhaRepository.deleteAll();
    }

    /** Cadastra uma pastilha pela API e devolve o id que o banco gerou. */
    private long cadastrar(String codigo) throws Exception {
        String corpo = mvc.perform(post("/api/pastilhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao(codigo))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(corpo).get("id").asLong();
    }

    private void movimentar(String caminho, long pastilhaId, String quantidade, int statusEsperado) throws Exception {
        mvc.perform(post("/api/movimentacoes/" + caminho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pastilhaId\":" + pastilhaId + ",\"quantidade\":" + quantidade + "}"))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    @DisplayName("cadastro, entradas e saidas fecham o saldo e alimentam o historico")
    void fluxoCompleto() throws Exception {
        long id = cadastrar("CNMG120408");

        // a pastilha nasce zerada e ja aparece como abaixo do minimo (0 <= 20)
        mvc.perform(get("/api/pastilhas/" + id))
                .andExpect(jsonPath("$.saldoAtual").value(0))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(true));

        movimentar("entrada", id, "100", 201);
        movimentar("entrada", id, "50", 201);
        movimentar("saida", id, "30", 201);

        // 0 + 100 + 50 - 30 = 120
        mvc.perform(get("/api/pastilhas/" + id))
                .andExpect(jsonPath("$.saldoAtual").value(120))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(false))
                .andExpect(jsonPath("$.valorEmEstoque").value(1200.00));

        // o historico guarda o saldo depois de cada lancamento, do mais novo para o mais antigo
        mvc.perform(get("/api/movimentacoes").param("pastilhaId", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].tipo").value("SAIDA"))
                .andExpect(jsonPath("$.content[0].saldoApos").value(120))
                .andExpect(jsonPath("$.content[1].saldoApos").value(150))
                .andExpect(jsonPath("$.content[2].saldoApos").value(100));
    }

    @Test
    @DisplayName("a saida maior que o saldo e recusada e o estoque nao muda")
    void saidaAcimaDoSaldoNaoAlteraEstoque() throws Exception {
        long id = cadastrar("WNMG080408");
        movimentar("entrada", id, "10", 201);

        movimentar("saida", id, "11", 409);

        mvc.perform(get("/api/pastilhas/" + id)).andExpect(jsonPath("$.saldoAtual").value(10));
        // a movimentacao recusada nao pode ter sido gravada
        assertThat(movimentacaoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("pastilha inativada nao aceita mais movimentacao")
    void pastilhaInativaNaoMovimenta() throws Exception {
        long id = cadastrar("APKT1604");
        movimentar("entrada", id, "10", 201);

        mvc.perform(delete("/api/pastilhas/" + id)).andExpect(status().isNoContent());

        movimentar("entrada", id, "5", 409);
        movimentar("saida", id, "5", 409);
        assertThat(movimentacaoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("o painel soma o que foi cadastrado e movimentado")
    void painelRefleteOEstoque() throws Exception {
        long comEstoque = cadastrar("CNMG120408");
        long emFalta = cadastrar("WNMG080408");

        movimentar("entrada", comEstoque, "100", 201);
        movimentar("entrada", emFalta, "5", 201);

        String corpo = mvc.perform(get("/api/dashboard/indicadores"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode painel = json.readTree(corpo);
        assertThat(painel.get("itensCadastrados").asLong()).isEqualTo(2);
        assertThat(painel.get("categorias").asLong()).isEqualTo(1);
        // so a de saldo 5 esta abaixo do minimo 20
        assertThat(painel.get("itensAbaixoDoMinimo").asLong()).isEqualTo(1);
        // 100x10 + 5x10 = 1050
        assertThat(painel.get("valorEmEstoque").asDouble()).isEqualTo(1050.0);
        assertThat(painel.get("movimentacoesUltimos30Dias").asLong()).isEqualTo(2);
        assertThat(painel.get("alertasReposicao")).hasSize(1);
        assertThat(painel.get("alertasReposicao").get(0).get("codigo").asText()).isEqualTo("WNMG080408");
        assertThat(painel.get("movimentacoesRecentes")).hasSize(2);
    }

    @Test
    @DisplayName("a lista de reposicao traz so quem chegou no nivel minimo")
    void alertasDeReposicao() throws Exception {
        long comEstoque = cadastrar("CNMG120408");
        cadastrar("WNMG080408");
        movimentar("entrada", comEstoque, "100", 201);

        mvc.perform(get("/api/alertas/reposicao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("WNMG080408"));
    }

    @Test
    @DisplayName("codigo repetido e barrado pela API")
    void codigoRepetido() throws Exception {
        cadastrar("CNMG120408");

        mvc.perform(post("/api/pastilhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Fabrica.requisicao("cnmg120408"))))
                .andExpect(status().isConflict());

        assertThat(pastilhaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a busca e o filtro de categoria funcionam na API")
    void buscaEFiltro() throws Exception {
        cadastrar("CNMG120408");
        cadastrar("WNMG080408");

        mvc.perform(get("/api/pastilhas").param("busca", "cnmg"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].codigo").value("CNMG120408"));

        mvc.perform(get("/api/pastilhas").param("categoria", "Torneamento"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/pastilhas/categorias"))
                .andExpect(jsonPath("$[0]").value("Torneamento"));
    }

    @Test
    @DisplayName("inativar tira da lista de ativas e reativar traz de volta, sem perder o historico")
    void inativarEReativar() throws Exception {
        long id = cadastrar("CNMG120408");
        movimentar("entrada", id, "40", 201);

        mvc.perform(delete("/api/pastilhas/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pastilhas")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/pastilhas").param("ativo", "false"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // o historico da pastilha inativa continua consultavel
        mvc.perform(get("/api/movimentacoes").param("pastilhaId", String.valueOf(id)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/pastilhas/" + id + "/reativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.saldoAtual").value(40));
    }

    @Test
    @DisplayName("as telas em Thymeleaf renderizam sem erro")
    void telasRenderizam() throws Exception {
        long id = cadastrar("CNMG120408");

        for (String rota : new String[]{"/", "/pastilhas", "/pastilhas/nova", "/movimentacoes"}) {
            mvc.perform(get(rota)).andExpect(status().isOk());
        }
        mvc.perform(get("/pastilhas/" + id + "/editar")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a documentacao do Swagger continua sendo gerada")
    void swaggerDisponivel() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/pastilhas']").exists())
                .andExpect(jsonPath("$.paths['/api/movimentacoes/entrada']").exists());
    }
}
