package br.com.ddametalurgica.estoque.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// Contrato da API versionado em docs/openapi.yaml.
//
// O teste falha se o documento gerado pelo codigo for diferente do arquivo
// versionado: qualquer mudanca na API tem que atualizar o contrato no mesmo PR.
// Para regravar o arquivo depois de uma mudanca intencional:
//
//   ./mvnw test -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Contrato OpenAPI - docs/openapi.yaml igual ao gerado pelo codigo")
class ContratoOpenApiTest {

    private static final Path CONTRATO = Path.of(System.getProperty("basedir", "."), "..", "docs", "openapi.yaml")
            .normalize();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("o contrato versionado esta atualizado")
    void contratoAtualizado() throws Exception {
        String gerado = mvc.perform(get("/v3/api-docs.yaml"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean("contrato.atualizar")) {
            Files.writeString(CONTRATO, gerado, StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRATO).as("arquivo do contrato").exists();
        // Compara o conteudo (arvore YAML), nao o texto: quebra de linha e ordem de chaves nao contam
        JsonNode esperado = YAML.readTree(Files.readString(CONTRATO, StandardCharsets.UTF_8));
        JsonNode atual = YAML.readTree(gerado);

        assertThat(diferencas(esperado, atual))
                .as("docs/openapi.yaml desatualizado. Se a mudanca na API foi intencional, rode:%n"
                        + "  ./mvnw test -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true%n"
                        + "e inclua o arquivo no commit. Trechos diferentes")
                .isEmpty();
    }

    // Lista as rotas e secoes que mudaram, para a falha apontar onde olhar
    private static List<String> diferencas(JsonNode esperado, JsonNode atual) {
        List<String> diferentes = new ArrayList<>();
        TreeSet<String> secoes = new TreeSet<>();
        esperado.fieldNames().forEachRemaining(secoes::add);
        atual.fieldNames().forEachRemaining(secoes::add);

        for (String secao : secoes) {
            JsonNode e = esperado.path(secao);
            JsonNode a = atual.path(secao);
            if (e.equals(a)) {
                continue;
            }
            if (secao.equals("paths") || secao.equals("components")) {
                TreeSet<String> itens = new TreeSet<>();
                e.fieldNames().forEachRemaining(itens::add);
                a.fieldNames().forEachRemaining(itens::add);
                for (String item : itens) {
                    if (!e.path(item).equals(a.path(item))) {
                        diferentes.add(secao + " -> " + item);
                    }
                }
            } else {
                diferentes.add(secao);
            }
        }
        return diferentes;
    }
}
