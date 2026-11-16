package br.com.ddametalurgica.estoque.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Confere que o documento OpenAPI gerado descreve o comportamento real da API
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Documento OpenAPI - fiel ao comportamento da API")
class OpenApiDocumentacaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private JsonNode documento;

    @BeforeEach
    void carregarDocumento() throws Exception {
        String corpo = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        documento = json.readTree(corpo);
    }

    private JsonNode operacao(String caminho, String metodo) {
        return documento.path("paths").path(caminho).path(metodo);
    }

    @Test
    @DisplayName("documento em YAML abre sem login")
    void yamlPublico() throws Exception {
        int status = mvc.perform(get("/v3/api-docs.yaml")).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("declara a sessao por cookie e o cabecalho CSRF")
    void esquemasDeSeguranca() {
        JsonNode esquemas = documento.path("components").path("securitySchemes");
        assertThat(esquemas.path("sessao").path("in").asText()).isEqualTo("cookie");
        assertThat(esquemas.path("sessao").path("name").asText()).isEqualTo("JSESSIONID");
        assertThat(esquemas.path("csrf").path("name").asText()).isEqualTo("X-CSRF-TOKEN");
    }

    @Test
    @DisplayName("criacao documenta 201 e exclusao documenta 204, sem o 200 padrao")
    void statusDeSucesso() {
        assertThat(operacao("/api/pastilhas", "post").path("responses").has("201")).isTrue();
        assertThat(operacao("/api/pastilhas", "post").path("responses").has("200")).isFalse();
        assertThat(operacao("/api/movimentacoes/saida", "post").path("responses").has("422")).isTrue();
        assertThat(operacao("/api/pastilhas/{id}", "delete").path("responses").has("204")).isTrue();
        assertThat(operacao("/api/pastilhas/{id}", "delete").path("responses").has("200")).isFalse();
        assertThat(operacao("/api/usuarios/{id}/senha", "patch").path("responses").has("204")).isTrue();
    }

    @Test
    @DisplayName("toda rota da API documenta o sucesso, 401, 403 e 500, exige sessao e, se altera dados, CSRF")
    void errosESegurancaEmTodaRota() {
        documento.path("paths").fields().forEachRemaining(caminho -> {
            if (!caminho.getKey().startsWith("/api")) {
                return;
            }
            caminho.getValue().fields().forEachRemaining(op -> {
                JsonNode respostas = op.getValue().path("responses");
                assertThat(respostas.has("401")).as(op.getKey() + " " + caminho.getKey()).isTrue();
                assertThat(respostas.has("403")).as(op.getKey() + " " + caminho.getKey()).isTrue();
                assertThat(respostas.has("500")).as(op.getKey() + " " + caminho.getKey()).isTrue();
                boolean temSucesso = respostas.has("200") || respostas.has("201") || respostas.has("204");
                assertThat(temSucesso).as("resposta 2xx em " + op.getKey() + " " + caminho.getKey()).isTrue();
                JsonNode seguranca = op.getValue().path("security").get(0);
                assertThat(seguranca.has("sessao")).isTrue();
                assertThat(seguranca.has("csrf")).isEqualTo(!op.getKey().equals("get"));
            });
        });
    }

    @Test
    @DisplayName("login e logout aparecem no documento")
    void loginELogout() {
        assertThat(operacao("/login", "post").isMissingNode()).isFalse();
        assertThat(operacao("/logout", "post").isMissingNode()).isFalse();
    }

    @Test
    @DisplayName("o perfil escrito em cada operacao bate com o que a SecurityConfig bloqueia")
    void perfilDocumentadoBateComASeguranca() throws Exception {
        List<String> divergencias = new ArrayList<>();
        var caminhos = documento.path("paths").fields();
        while (caminhos.hasNext()) {
            Map.Entry<String, JsonNode> caminho = caminhos.next();
            if (!caminho.getKey().startsWith("/api")) {
                continue;
            }
            var operacoes = caminho.getValue().fields();
            while (operacoes.hasNext()) {
                Map.Entry<String, JsonNode> op = operacoes.next();
                boolean soAdmin = op.getValue().path("description").asText().endsWith("**Perfil:** ADMIN");
                String url = caminho.getKey().replace("{id}", "1");
                HttpMethod metodo = HttpMethod.valueOf(op.getKey().toUpperCase());

                var pedido = request(metodo, url)
                        .with(user("operador@dda.com.br").roles("OPERADOR"))
                        .with(csrf());
                if (metodo != HttpMethod.GET) {
                    // corpo vazio: se passar da autorizacao, para na validacao (400) sem gravar nada
                    pedido.contentType(MediaType.APPLICATION_JSON).content("{}");
                }
                int status = mvc.perform(pedido).andReturn().getResponse().getStatus();

                boolean bloqueado = status == 403;
                if (bloqueado != soAdmin) {
                    divergencias.add(op.getKey().toUpperCase() + " " + caminho.getKey()
                            + ": documento diz " + (soAdmin ? "ADMIN" : "ADMIN ou OPERADOR")
                            + ", OPERADOR recebeu " + status);
                }
            }
        }
        assertThat(divergencias).isEmpty();
    }
}
