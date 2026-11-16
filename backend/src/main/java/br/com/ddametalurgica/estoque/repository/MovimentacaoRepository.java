package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.Movimentacao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    // Historico filtrado. pastilhaId = 0 desativa o filtro de pastilha.
    // @EntityGraph carrega pastilha e usuario no mesmo SELECT (evita N+1).
    @EntityGraph(attributePaths = {"pastilha", "usuario"})
    @Query("SELECT m FROM Movimentacao m "
            + "WHERE (:pastilhaId = 0 OR m.pastilha.id = :pastilhaId) "
            + "  AND m.dataHora BETWEEN :inicio AND :fim "
            + "ORDER BY m.dataHora DESC")
    Page<Movimentacao> buscar(@Param("pastilhaId") Long pastilhaId,
                              @Param("inicio") LocalDateTime inicio,
                              @Param("fim") LocalDateTime fim,
                              Pageable pageable);

    // Busca por id ja com pastilha e usuario carregados
    @EntityGraph(attributePaths = {"pastilha", "usuario"})
    Optional<Movimentacao> findComDetalhesById(Long id);

    // Ultimas movimentacoes exibidas no painel (query derivada do nome do metodo)
    @EntityGraph(attributePaths = {"pastilha", "usuario"})
    List<Movimentacao> findTop8ByOrderByDataHoraDesc();

    // Totais de entrada e saida por pastilha desde uma data (base dos indicadores)
    @Query("SELECT new br.com.ddametalurgica.estoque.repository.TotalMovimentado("
            + "m.pastilha.id, m.tipo, SUM(m.quantidade)) "
            + "FROM Movimentacao m WHERE m.dataHora >= :desde "
            + "GROUP BY m.pastilha.id, m.tipo")
    List<TotalMovimentado> somarPorPastilhaDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT COUNT(m) FROM Movimentacao m WHERE m.dataHora >= :desde")
    long contarDesde(@Param("desde") LocalDateTime desde);
}
