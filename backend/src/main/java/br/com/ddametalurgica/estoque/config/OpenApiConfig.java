package br.com.ddametalurgica.estoque.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Metadados da documentacao OpenAPI (Swagger UI em /swagger-ui.html)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("API - Controle de Estoque de Pastilhas Industriais")
                .version("0.1.0")
                .description("""
                        MVP do sistema de controle de estoque da DDA Metalúrgica.

                        Projeto Aplicado III - ADS 4ª fase - UniSENAI.
                        Equipe: Brenno Meurer, Vinicius Barcellos, Pedro Ptelzi e Pedro Luis.
                        """)
                .contact(new Contact().name("Equipe Projeto Aplicado III - UniSENAI")))
                // URL relativa: o documento funciona em qualquer host (local ou EC2)
                .servers(List.of(new Server().url("/")));
    }
}
