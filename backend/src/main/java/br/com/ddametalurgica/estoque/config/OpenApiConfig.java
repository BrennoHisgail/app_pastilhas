package br.com.ddametalurgica.estoque.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configura o titulo e a descricao que aparecem no Swagger,
// que fica em /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("API - Controle de Estoque de Pastilhas Industriais")
                .version("0.1.0")
                .description("""
                        MVP do sistema de controle de estoque da DDA Metalurgica.

                        Projeto Aplicado III - ADS 4a fase - UniSENAI.
                        Equipe: Brenno Meurer, Vinicius Barcellos, Pedro Ptelzi e Pedro Luis.
                        """)
                .contact(new Contact().name("Equipe Projeto Aplicado III - UniSENAI")));
    }
}
