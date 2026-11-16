package br.com.ddametalurgica.estoque.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Metadados da documentacao OpenAPI (Swagger UI em /swagger-ui.html) e
// complementos que o springdoc nao deduz sozinho dos controllers:
// autenticacao, CSRF, perfil exigido, respostas de erro e as rotas de login/logout.
@Configuration
public class OpenApiConfig {

    static final String SESSAO = "sessao";
    static final String CSRF = "csrf";
    private static final String PROBLEMA = "#/components/schemas/ProblemDetail";
    private static final String PROBLEMA_JSON = "application/problem+json";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Controle de Estoque de Pastilhas Industriais")
                        .version("0.1.0")
                        .description("""
                                MVP do sistema de controle de estoque da DDA Metalúrgica.

                                Projeto Aplicado III - ADS 4ª fase - UniSENAI.
                                Equipe: Brenno Meurer, Vinicius Barcellos, Pedro Ptelzi e Pedro Luis.

                                ## Autenticação
                                A API usa a sessão do login por formulário (`POST /login`), guardada no
                                cookie `JSESSIONID`. Sem sessão válida, as rotas de `/api` respondem **401**.

                                ## CSRF
                                Toda requisição que altera dados (POST, PUT, PATCH, DELETE) precisa enviar o
                                token CSRF no cabeçalho `X-CSRF-TOKEN`. As telas leem o token das tags
                                `<meta name="_csrf">` do layout. Sem o token, a resposta é **403**.

                                ## Perfis
                                - **OPERADOR**: consulta pastilhas, registra entradas e saídas, vê painel,
                                  indicadores e relatórios.
                                - **ADMIN**: tudo do operador, mais cadastro de pastilhas e gestão de usuários.

                                Cada operação informa o perfil exigido na descrição.

                                ## Erros
                                Os erros seguem o formato ProblemDetail (RFC 7807). Erros de validação trazem
                                o campo `erros`, com a mensagem de cada campo.
                                """)
                        .contact(new Contact().name("Equipe Projeto Aplicado III - UniSENAI")))
                // URL relativa: o documento funciona em qualquer host (local ou EC2)
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(SESSAO, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Cookie de sessão criado pelo POST /login"))
                        .addSecuritySchemes(CSRF, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN")
                                .description("Token CSRF, obrigatório em POST, PUT, PATCH e DELETE"))
                        .addSchemas("ProblemDetail", problemDetail()));
    }

    // Completa cada operacao gerada pelo springdoc com seguranca, perfil e respostas de erro
    @Bean
    public OpenApiCustomizer complementarOperacoes() {
        return openApi -> {
            openApi.getPaths().forEach((caminho, item) -> item.readOperationsMap().forEach((metodo, operacao) -> {
                boolean alteraDados = metodo != PathItem.HttpMethod.GET;
                exigirAutenticacao(operacao, alteraDados);
                informarPerfil(operacao, perfilExigido(caminho, metodo));
                adicionarErros(operacao, caminho, alteraDados);
            }));
            adicionarLoginLogout(openApi);
            // Ordem fixa das tags: o docs/openapi.yaml nao muda entre execucoes
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparing(Tag::getName));
            }
        };
    }

    // Espelha as regras de autorizacao da SecurityConfig
    static String perfilExigido(String caminho, PathItem.HttpMethod metodo) {
        if (caminho.startsWith("/api/usuarios")) {
            return "ADMIN";
        }
        if (caminho.startsWith("/api/pastilhas") && metodo != PathItem.HttpMethod.GET) {
            return "ADMIN";
        }
        return "ADMIN ou OPERADOR";
    }

    private static void exigirAutenticacao(Operation operacao, boolean alteraDados) {
        SecurityRequirement requisito = new SecurityRequirement().addList(SESSAO);
        if (alteraDados) {
            requisito.addList(CSRF);
        }
        operacao.setSecurity(List.of(requisito));
    }

    private static void informarPerfil(Operation operacao, String perfil) {
        String descricao = operacao.getDescription() == null ? "" : operacao.getDescription() + "\n\n";
        operacao.setDescription(descricao + "**Perfil:** " + perfil);
    }

    private static void adicionarErros(Operation operacao, String caminho, boolean alteraDados) {
        ApiResponses respostas = operacao.getResponses();
        // 201/204 declarados no controller substituem o 200 que o springdoc gera por padrao
        if (respostas.containsKey("201") || respostas.containsKey("204")) {
            respostas.remove("200");
        }
        boolean recebeDados = operacao.getRequestBody() != null
                || (operacao.getParameters() != null && !operacao.getParameters().isEmpty());
        if (recebeDados) {
            respostas.putIfAbsent("400", erro("Dados inválidos (corpo, parâmetro ou validação)"));
        }
        respostas.putIfAbsent("401", new ApiResponse().description("Sem sessão: faça login em POST /login"));
        respostas.putIfAbsent("403", new ApiResponse()
                .description(alteraDados ? "Perfil sem permissão ou token CSRF ausente" : "Perfil sem permissão")
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(PROBLEMA)))));
        if (caminho.contains("{id}")) {
            respostas.putIfAbsent("404", erro("Registro não encontrado"));
        }
        respostas.putIfAbsent("500", erro("Erro inesperado no servidor"));
    }

    private static ApiResponse erro(String descricao) {
        return new ApiResponse().description(descricao)
                .content(new Content().addMediaType(PROBLEMA_JSON, new MediaType().schema(new Schema<>().$ref(PROBLEMA))));
    }

    // /login e /logout sao do Spring Security (formLogin), fora dos controllers
    private static void adicionarLoginLogout(OpenAPI openApi) {
        Schema<?> formLogin = new ObjectSchema()
                .addProperty("username", new StringSchema().description("E-mail do usuário").example("admin@dda.com.br"))
                .addProperty("password", new StringSchema().description("Senha").format("password"))
                .addProperty("_csrf", new StringSchema().description("Token CSRF da página de login"))
                .required(List.of("username", "password", "_csrf"));

        openApi.path("/login", new PathItem().post(new Operation()
                .tags(List.of("Autenticação"))
                .summary("Login por formulário")
                .description("Cria a sessão (cookie JSESSIONID). Endpoint do Spring Security, usado pela tela de login.")
                .operationId("login")
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        "application/x-www-form-urlencoded", new MediaType().schema(formLogin))))
                .responses(new ApiResponses().addApiResponse("302", new ApiResponse()
                        .description("Sucesso: redireciona para /. Falha: redireciona para /login?erro=true")
                        .headers(Map.of("Location", new Header().schema(new StringSchema())))))));

        openApi.path("/logout", new PathItem().post(new Operation()
                .tags(List.of("Autenticação"))
                .summary("Logout")
                .description("Encerra a sessão e apaga o cookie JSESSIONID.")
                .operationId("logout")
                .security(List.of(new SecurityRequirement().addList(SESSAO).addList(CSRF)))
                .responses(new ApiResponses().addApiResponse("302", new ApiResponse()
                        .description("Redireciona para /login?saiu=true")
                        .headers(Map.of("Location", new Header().schema(new StringSchema())))))));
    }

    private static Schema<?> problemDetail() {
        return new ObjectSchema()
                .description("Erro no formato RFC 7807")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Operação não permitida"))
                .addProperty("status", new IntegerSchema().example(422))
                .addProperty("detail", new StringSchema()
                        .example("Saldo insuficiente: a pastilha CNMG120408 tem 10 UN em estoque, e a saída pede 25."))
                .addProperty("instance", new StringSchema().example("/api/movimentacoes/saida"))
                .addProperty("erros", new MapSchema()
                        .additionalProperties(new StringSchema())
                        .description("Só em erros de validação: mensagem por campo")
                        .example(Map.of("quantidade", "A quantidade deve ser maior que zero")));
    }
}
