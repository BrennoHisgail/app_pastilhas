package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Consultas de pastilha. O CRUD basico vem do JpaRepository.
public interface PastilhaRepository extends JpaRepository<Pastilha, Long> {

    // Validacao de codigo unico no cadastro e na edicao
    boolean existsByCodigoIgnoreCase(String codigo);

    // Lock pessimista (SELECT ... FOR UPDATE) usado na movimentacao: com duas
    // saidas simultaneas, a segunda espera a primeira e le o saldo ja atualizado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pastilha p WHERE p.id = :id")
    Optional<Pastilha> buscarParaMovimentar(@Param("id") Long id);

    // Pesquisa do catalogo. Filtro vazio ('') desativa a condicao correspondente.
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

    // Categorias distintas das pastilhas ativas (filtro da tela de consulta)
    @Query("SELECT DISTINCT p.categoria FROM Pastilha p WHERE p.ativo = TRUE ORDER BY p.categoria")
    List<String> listarCategorias();

    // Os 4 numeros do painel numa consulta so: pastilhas ativas, categorias,
    // itens em falta e valor em estoque. O COALESCE devolve 0 em vez de null
    // quando nao ha nenhuma pastilha.
    @Query("SELECT new br.com.ddametalurgica.estoque.repository.ResumoPastilhas("
            + "COUNT(p), COUNT(DISTINCT p.categoria), "
            + "COALESCE(SUM(CASE WHEN p.saldoAtual <= p.nivelMinimo THEN 1L ELSE 0L END), 0L), "
            + "COALESCE(SUM(p.saldoAtual * p.custoUnitario), 0)) "
            + "FROM Pastilha p WHERE p.ativo = TRUE")
    ResumoPastilhas buscarResumo();

    // Pastilhas ativas, base dos indicadores e do relatorio CSV
    List<Pastilha> findByAtivoTrueOrderByCodigoAsc();

    // Itens no nivel minimo ou abaixo, ordenados pela maior falta
    @Query("SELECT p FROM Pastilha p "
            + "WHERE p.ativo = TRUE AND p.saldoAtual <= p.nivelMinimo "
            + "ORDER BY (p.nivelMinimo - p.saldoAtual) DESC, p.codigo ASC")
    List<Pastilha> listarAbaixoDoMinimo(Pageable pageable);
}
