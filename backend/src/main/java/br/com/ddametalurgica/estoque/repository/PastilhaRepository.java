package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorio da pastilha. O JpaRepository ja traz save, findById,
// findAll e delete prontos, entao aqui so escrevemos as consultas nossas.
public interface PastilhaRepository extends JpaRepository<Pastilha, Long> {

    // Usado para nao deixar cadastrar dois codigos iguais
    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Pastilha> findByCodigoIgnoreCase(String codigo);

    // Busca a pastilha travando a linha no banco (SELECT ... FOR UPDATE).
    // Usado na movimentacao: se duas saidas chegarem juntas, a segunda
    // espera a primeira terminar e ja le o saldo atualizado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pastilha p WHERE p.id = :id")
    Optional<Pastilha> buscarParaMovimentar(@Param("id") Long id);

    // Consulta da tela de pesquisa. Quando o filtro vem vazio ele nao
    // filtra nada, por isso a comparacao com '' no comeco de cada linha.
    @Query("SELECT p FROM Pastilha p "
            + "WHERE (:busca = '' "
            + "       OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%')) "
            + "       OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))) "
            + "  AND (:categoria = '' OR LOWER(p.categoria) = LOWER(:categoria)) "
            + "  AND p.ativo = :ativo "
            + "  AND (:somenteAbaixoDoMinimo = FALSE OR p.saldoAtual <= p.nivelMinimo)")
    Page<Pastilha> buscar(@Param("busca") String busca,
                          @Param("categoria") String categoria,
                          @Param("ativo") boolean ativo,
                          @Param("somenteAbaixoDoMinimo") boolean somenteAbaixoDoMinimo,
                          Pageable pageable);

    // Preenche o combo de categorias da tela de consulta
    @Query("SELECT DISTINCT p.categoria FROM Pastilha p WHERE p.ativo = TRUE ORDER BY p.categoria")
    List<String> listarCategorias();

    @Query("SELECT COUNT(p) FROM Pastilha p WHERE p.ativo = TRUE")
    long contarAtivas();

    @Query("SELECT COUNT(p) FROM Pastilha p WHERE p.ativo = TRUE AND p.saldoAtual <= p.nivelMinimo")
    long contarAbaixoDoMinimo();

    @Query("SELECT COUNT(DISTINCT p.categoria) FROM Pastilha p WHERE p.ativo = TRUE")
    long contarCategorias();

    // COALESCE para devolver 0 em vez de null quando nao ha nenhuma pastilha
    @Query("SELECT COALESCE(SUM(p.saldoAtual * p.custoUnitario), 0) FROM Pastilha p WHERE p.ativo = TRUE")
    BigDecimal somarValorEmEstoque();

    // Junta os 4 contadores acima numa consulta so, para o painel
    @Query("SELECT new br.com.ddametalurgica.estoque.repository.ResumoPastilhas("
            + "COUNT(p), COUNT(DISTINCT p.categoria), "
            + "COALESCE(SUM(CASE WHEN p.saldoAtual <= p.nivelMinimo THEN 1L ELSE 0L END), 0L), "
            + "COALESCE(SUM(p.saldoAtual * p.custoUnitario), 0)) "
            + "FROM Pastilha p WHERE p.ativo = TRUE")
    ResumoPastilhas buscarResumo();

    // Todas as pastilhas ativas, usado nos indicadores e no relatorio
    List<Pastilha> findByAtivoTrueOrderByCodigoAsc();

    // Lista os itens que precisam de reposicao, os mais criticos primeiro
    @Query("SELECT p FROM Pastilha p "
            + "WHERE p.ativo = TRUE AND p.saldoAtual <= p.nivelMinimo "
            + "ORDER BY (p.nivelMinimo - p.saldoAtual) DESC, p.codigo ASC")
    List<Pastilha> listarAbaixoDoMinimo(Pageable pageable);
}
