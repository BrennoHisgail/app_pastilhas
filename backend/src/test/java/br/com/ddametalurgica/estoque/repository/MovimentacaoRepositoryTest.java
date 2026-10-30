package br.com.ddametalurgica.estoque.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("MovimentacaoRepository - consultas do historico e do painel")
class MovimentacaoRepositoryTest {

    @Autowired
    private MovimentacaoRepository repository;

    @Autowired
    private TestEntityManager em;

    private final Pageable primeiraPagina = PageRequest.of(0, 20);

    private Pastilha pastilhaA;
    private Pastilha pastilhaB;

    @BeforeEach
    void carregarMassa() {
        pastilhaA = em.persist(Fabrica.pastilha("A1"));
        pastilhaB = em.persist(Fabrica.pastilha("B2"));

        lancar(pastilhaA, TipoMovimentacao.ENTRADA, "10", LocalDateTime.now().minusDays(40));
        lancar(pastilhaA, TipoMovimentacao.SAIDA, "3", LocalDateTime.now().minusDays(5));
        lancar(pastilhaA, TipoMovimentacao.ENTRADA, "7", LocalDateTime.now().minusDays(1));
        lancar(pastilhaB, TipoMovimentacao.ENTRADA, "20", LocalDateTime.now().minusDays(2));

        em.flush();
        em.clear();
    }

    private void lancar(Pastilha pastilha, TipoMovimentacao tipo, String quantidade, LocalDateTime quando) {
        Movimentacao movimentacao = new Movimentacao(
                pastilha, tipo, new BigDecimal(quantidade), "DOC", "origem", null);
        // A data normalmente e o momento da gravacao; aqui forcamos para
        // conseguir montar um historico espalhado no tempo.
        ReflectionTestUtils.setField(movimentacao, "dataHora", quando);
        em.persist(movimentacao);
    }

    @Test
    @DisplayName("id zero significa sem filtro de pastilha: traz todas")
    void semFiltroDePastilha() {
        Page<Movimentacao> pagina = repository.buscar(
                0L, LocalDateTime.now().minusYears(1), LocalDateTime.now(), primeiraPagina);

        assertThat(pagina.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("filtra pelas movimentacoes de uma pastilha so")
    void filtraPorPastilha() {
        Page<Movimentacao> pagina = repository.buscar(
                pastilhaA.getId(), LocalDateTime.now().minusYears(1), LocalDateTime.now(), primeiraPagina);

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent())
                .allSatisfy(m -> assertThat(m.getPastilha().getCodigo()).isEqualTo("A1"));
    }

    @Test
    @DisplayName("o historico vem da mais nova para a mais antiga")
    void ordenaDaMaisNova() {
        Page<Movimentacao> pagina = repository.buscar(
                0L, LocalDateTime.now().minusYears(1), LocalDateTime.now(), primeiraPagina);

        assertThat(pagina.getContent())
                .isSortedAccordingTo((a, b) -> b.getDataHora().compareTo(a.getDataHora()));
    }

    @Test
    @DisplayName("o periodo corta o que ficou de fora das datas")
    void filtraPorPeriodo() {
        Page<Movimentacao> pagina = repository.buscar(
                0L, LocalDateTime.now().minusDays(10), LocalDateTime.now(), primeiraPagina);

        // fica de fora so a entrada de 40 dias atras
        assertThat(pagina.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("periodo sem nenhuma movimentacao devolve pagina vazia")
    void periodoVazio() {
        Page<Movimentacao> pagina = repository.buscar(
                0L, LocalDateTime.now().minusYears(5), LocalDateTime.now().minusYears(4), primeiraPagina);

        assertThat(pagina).isEmpty();
    }

    @Test
    @DisplayName("as ultimas movimentacoes do painel vem com a pastilha carregada junto")
    void ultimasDoPainel() {
        var ultimas = repository.findTop8ByOrderByDataHoraDesc();

        assertThat(ultimas).hasSize(4);
        assertThat(ultimas.get(0).getPastilha().getCodigo()).isEqualTo("A1");
        assertThat(ultimas)
                .isSortedAccordingTo((a, b) -> b.getDataHora().compareTo(a.getDataHora()));
    }

    @Test
    @DisplayName("contarDesde conta so o que aconteceu dentro do periodo")
    void contaDesde() {
        assertThat(repository.contarDesde(LocalDateTime.now().minusDays(30))).isEqualTo(3);
        assertThat(repository.contarDesde(LocalDateTime.now().minusDays(3))).isEqualTo(2);
        assertThat(repository.contarDesde(LocalDateTime.now().plusDays(1))).isZero();
    }

    @Test
    @DisplayName("a paginacao do historico funciona")
    void paginaOHistorico() {
        Page<Movimentacao> pagina = repository.buscar(
                0L, LocalDateTime.now().minusYears(1), LocalDateTime.now(), PageRequest.of(1, 3));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("a pastilha B tem so a movimentacao dela")
    void movimentacoesDaOutraPastilha() {
        Page<Movimentacao> pagina = repository.buscar(
                pastilhaB.getId(), LocalDateTime.now().minusYears(1), LocalDateTime.now(), primeiraPagina);

        assertThat(pagina.getContent())
                .singleElement()
                .satisfies(m -> assertThat(m.getQuantidade()).isEqualByComparingTo("20"));
    }
}
