package br.com.ddametalurgica.estoque.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// Configuracao do login e das permissoes (ADMIN e OPERADOR).
// O login guarda a sessao em cookie, por isso o CSRF fica ligado.
@Configuration
public class SecurityConfig {

    // Criptografa a senha no cadastro e confere a senha no login
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AntPathRequestMatcher apiMatcher = new AntPathRequestMatcher("/api/**");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.svg",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()

                        // Pastilhas: qualquer um que esteja logado consulta;
                        // so ADMIN cadastra, edita, inativa ou muda o nivel minimo
                        .requestMatchers(HttpMethod.GET, "/api/pastilhas/**").authenticated()
                        .requestMatchers("/api/pastilhas/**").hasRole("ADMIN")

                        // Movimentacao e dashboard: OPERADOR tambem usa no dia a dia
                        .requestMatchers("/api/movimentacoes/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/alertas/**").authenticated()

                        // Gestao de usuarios: so ADMIN
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // Telas: cadastro/edicao de pastilha e gestao de usuario sao so do ADMIN
                        .requestMatchers("/pastilhas/nova", "/pastilhas/*/editar").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/", "/pastilhas", "/movimentacoes").authenticated()

                        // Qualquer coisa nova que alguem adicionar depois fica fechada por padrao
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?erro=true"))

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu=true")
                        .deleteCookies("JSESSIONID"))

                // Na API devolve 401/403 em vez de mandar para a tela de login,
                // assim o api.js consegue saber que a sessao expirou
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiMatcher)
                        .defaultAccessDeniedHandlerFor(this::acessoNegadoEmJson, apiMatcher))

                .csrf(Customizer.withDefaults());

        return http.build();
    }

    // Mesmo formato de erro que o ApiExceptionHandler usa no resto da API
    private void acessoNegadoEmJson(jakarta.servlet.http.HttpServletRequest requisicao,
                                     jakarta.servlet.http.HttpServletResponse resposta,
                                     org.springframework.security.access.AccessDeniedException excecao)
            throws IOException {
        resposta.setStatus(HttpStatus.FORBIDDEN.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.getWriter().write(
                "{\"title\":\"Acesso negado\",\"status\":403,"
                        + "\"detail\":\"Voce nao tem permissao para esta acao.\"}");
    }
}
