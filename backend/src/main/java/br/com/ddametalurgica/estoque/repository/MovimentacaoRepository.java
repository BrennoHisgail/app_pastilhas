package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.Movimentacao;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    // Consulta do historico. O service manda id 0 quando o usuario
    // nao escolheu nenhuma pastilha, ai a consulta traz todas.
    // O EntityGraph traz a pastilha e o usuario junto, senao o JPA faria consultas a mais por linha
    @EntityGraph(attributePaths = {"pastilha", "usuario"})
    @Query("SELECT m FROM Movimentacao m "
            + "WHERE (:pastilhaId = 0 OR m.pastilha.id = :pastilhaId) "
            + "  AND m.dataHora BETWEEN :inicio AND :fim "
            + "ORDER BY m.dataHora DESC")
    Page<Movimentacao> buscar(@Param("pastilhaId") Long pastilhaId,
                              @Param("inicio") LocalDateTime inicio,
                              @Param("fim") LocalDateTime fim,
                              Pageable pageable);

    // As ultimas movimentacoes que aparecem no painel.
    // O Spring Data monta a consulta sozinho pelo nome do metodo.
    @EntityGraph(attributePaths = {"pastilha", "usuario"})
    List<Movimentacao> findTop8ByOrderByDataHoraDesc();

    // Soma o que entrou e o que saiu de cada pastilha desde uma data.
    // Usado pelos indicadores (consumo, giro, Curva ABC).
    @Query("SELECT new br.com.ddametalurgica.estoque.repository.TotalMovimentado("
            + "m.pastilha.id, m.tipo, SUM(m.quantidade)) "
            + "FROM Movimentacao m WHERE m.dataHora >= :desde "
            + "GROUP BY m.pastilha.id, m.tipo")
    List<TotalMovimentado> somarPorPastilhaDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT COUNT(m) FROM Movimentacao m WHERE m.dataHora >= :desde")
    long contarDesde(@Param("desde") LocalDateTime desde);
}
