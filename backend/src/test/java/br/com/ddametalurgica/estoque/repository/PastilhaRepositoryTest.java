package br.com.ddametalurgica.estoque.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.data.domain.Sort;

// Testa as consultas escritas na mao do PastilhaRepository contra um
// banco de verdade (H2 em memoria). Um erro de digitacao no JPQL so
// aparece quando a consulta roda, por isso estes testes existem.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PastilhaRepository - consultas do cadastro e do painel")
class PastilhaRepositoryTest {

    @Autowired
    private PastilhaRepository repository;

    @Autowired
    private TestEntityManager em;

    private final Pageable primeiraPagina = PageRequest.of(0, 20, Sort.by("codigo"));

    @BeforeEach
    void carregarMassa() {
        // saldo 100 / minimo 20 -> tranquila
        salvar("CNMG120408", "Pastilha de torneamento metal duro", "Torneamento",
                new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("10.00"), true);
        // saldo 5 / minimo 20 -> precisa repor, a mais critica
        salvar("WNMG080408", "Pastilha de torneamento revestida", "Torneamento",
                new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("2.00"), true);
        // saldo 30 / minimo 30 -> no limite, tambem precisa repor
        salvar("APKT1604", "Pastilha de fresamento PDER", "Fresamento",
                new BigDecimal("30"), new BigDecimal("30"), new BigDecimal("1.00"), true);
        // inativa: nao pode aparecer em nada
        salvar("ANTIGA01", "Pastilha descontinuada", "Furacao",
                new BigDecimal("999"), new BigDecimal("0"), new BigDecimal("50.00"), false);
        em.flush();
        em.clear();
    }

    private void salvar(String codigo, String descricao, String categoria, BigDecimal saldo,
                        BigDecimal nivelMinimo, BigDecimal custo, boolean ativo) {
        Pastilha pastilha = Fabrica.pastilha(codigo, saldo, nivelMinimo);
        pastilha.setDescricao(descricao);
        pastilha.setCategoria(categoria);
        pastilha.setCustoUnitario(custo);
        if (!ativo) {
            pastilha.inativar();
        }
        em.persist(pastilha);
    }

    @Test
    @DisplayName("busca vazia traz todas as ativas")
    void buscaVaziaTrazTodasAsAtivas() {
        Page<Pastilha> pagina = repository.buscar("", "", true, false, primeiraPagina);

        assertThat(pagina.getContent())
                .extracting(Pastilha::getCodigo)
                .containsExactly("APKT1604", "CNMG120408", "WNMG080408");
    }

    @Test
    @DisplayName("ativo=false lista só as inativadas")
    void listaInativas() {
        Page<Pastilha> pagina = repository.buscar("", "", false, false, primeiraPagina);

        assertThat(pagina.getContent()).extracting(Pastilha::getCodigo).containsExactly("ANTIGA01");
    }

    @Test
    @DisplayName("busca acha por trecho do código, sem diferenciar maiuscula de minuscula")
    void buscaPorCodigo() {
        assertThat(repository.buscar("cnmg", "", true, false, primeiraPagina).getContent())
                .extracting(Pastilha::getCodigo).containsExactly("CNMG120408");
    }

    @Test
    @DisplayName("busca também acha por trecho da descrição")
    void buscaPorDescricao() {
        assertThat(repository.buscar("revestida", "", true, false, primeiraPagina).getContent())
                .extracting(Pastilha::getCodigo).containsExactly("WNMG080408");
    }

    @Test
    @DisplayName("filtro de categoria traz só a categoria escolhida")
    void filtraPorCategoria() {
        assertThat(repository.buscar("", "Torneamento", true, false, primeiraPagina).getContent())
                .extracting(Pastilha::getCodigo).containsExactly("CNMG120408", "WNMG080408");
    }

    @Test
    @DisplayName("filtro de reposição traz as que chegaram no nível mínimo, inclusive as iguais")
    void filtraAbaixoDoMinimo() {
        assertThat(repository.buscar("", "", true, true, primeiraPagina).getContent())
                .extracting(Pastilha::getCodigo).containsExactly("APKT1604", "WNMG080408");
    }

    @Test
    @DisplayName("os filtros se somam: categoria mais reposição")
    void combinaFiltros() {
        assertThat(repository.buscar("", "Torneamento", true, true, primeiraPagina).getContent())
                .extracting(Pastilha::getCodigo).containsExactly("WNMG080408");
    }

    @Test
    @DisplayName("a paginação respeita o tamanho da página e conta o total")
    void pagina() {
        Page<Pastilha> pagina = repository.buscar("", "", true, false, PageRequest.of(0, 2, Sort.by("codigo")));

        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("código duplicado e detectado ignorando a caixa das letras")
    void detectaCodigoDuplicado() {
        assertThat(repository.existsByCodigoIgnoreCase("cnmg120408")).isTrue();
        assertThat(repository.existsByCodigoIgnoreCase("NAO_EXISTE")).isFalse();
    }

    @Test
    @DisplayName("as categorias vem sem repetir, em ordem, e sem as inativas")
    void listaCategorias() {
        List<String> categorias = repository.listarCategorias();

        assertThat(categorias).containsExactly("Fresamento", "Torneamento");
    }

    @Test
    @DisplayName("o resumo do painel ignora as pastilhas inativas")
    void resumoDoPainel() {
        ResumoPastilhas resumo = repository.buscarResumo();

        assertThat(resumo.ativas()).isEqualTo(3);
        assertThat(resumo.categorias()).isEqualTo(2);
        assertThat(resumo.abaixoDoMinimo()).isEqualTo(2);
        // 100x10 + 5x2 + 30x1 = 1040; a inativa (999x50) fica de fora
        assertThat(resumo.valorEmEstoque()).isEqualByComparingTo("1040");
    }

    @Test
    @DisplayName("com o banco vazio o resumo vem zerado, não nulo")
    void resumoComBancoVazio() {
        repository.deleteAll();
        em.flush();

        ResumoPastilhas resumo = repository.buscarResumo();

        assertThat(resumo.ativas()).isZero();
        assertThat(resumo.abaixoDoMinimo()).isZero();
        assertThat(resumo.valorEmEstoque()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a lista de reposição vem com a mais critica primeiro")
    void listaReposicaoOrdenadaPelaFalta() {
        // WNMG080408 falta 15 (20-5); APKT1604 falta 0 (30-30)
        List<Pastilha> criticas = repository.listarAbaixoDoMinimo(PageRequest.of(0, 10));

        assertThat(criticas).extracting(Pastilha::getCodigo).containsExactly("WNMG080408", "APKT1604");
    }

    @Test
    @DisplayName("a lista de reposição respeita o limite pedido")
    void listaReposicaoRespeitaOLimite() {
        assertThat(repository.listarAbaixoDoMinimo(PageRequest.of(0, 1)))
                .extracting(Pastilha::getCodigo)
                .containsExactly("WNMG080408");
    }

    @Test
    @DisplayName("buscarParaMovimentar traz a pastilha travada para atualizacao")
    void buscaParaMovimentar() {
        Long id = repository.findAll().stream()
                .filter(p -> p.getCodigo().equals("CNMG120408"))
                .findFirst().orElseThrow().getId();

        assertThat(repository.buscarParaMovimentar(id))
                .get()
                .extracting(Pastilha::getCodigo)
                .isEqualTo("CNMG120408");
        assertThat(repository.buscarParaMovimentar(-1L)).isEmpty();
    }
}
