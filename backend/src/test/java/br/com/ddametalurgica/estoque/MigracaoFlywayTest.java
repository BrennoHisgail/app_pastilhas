package br.com.ddametalurgica.estoque;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Sobe a aplicacao do jeito que ela sobe em producao: quem cria as
 * tabelas e o Flyway, e o Hibernate so confere se as entidades batem
 * com o banco (ddl-auto: validate).
 *
 * Se alguem mexer numa entidade e esquecer de escrever a migration,
 * este teste quebra aqui, e nao na hora de publicar.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:estoque_migracao;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@DisplayName("Migrations do Flyway")
class MigracaoFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("as migrations criam um banco que bate com as entidades e ja traz a massa de exemplo")
    void migrationsBatemComAsEntidades() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // se o contexto subiu, o ddl-auto: validate ja aprovou o mapeamento;
        // aqui conferimos que todas as migrations rodaram ate o fim.
        // O filtro por version deixa de fora a linha que o proprio Flyway
        // grava quando cria o schema.
        assertThat(jdbc.queryForList(
                "SELECT version FROM flyway_schema_history "
                        + "WHERE version IS NOT NULL AND success = TRUE ORDER BY installed_rank",
                String.class))
                .containsExactly("1", "2", "3", "4", "5");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pastilha", Integer.class))
                .isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimentacao", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class))
                .isEqualTo(2);

        // a massa de exemplo precisa nascer com itens em falta, senao a
        // tela de alertas abre vazia na apresentacao
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pastilha WHERE saldo_atual <= nivel_minimo", Integer.class))
                .isPositive();
    }
}
