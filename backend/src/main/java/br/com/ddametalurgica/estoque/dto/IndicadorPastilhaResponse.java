package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import java.math.BigDecimal;

// Indicadores de uma pastilha no periodo escolhido.
// coberturaDias e giro vem nulos quando nao da para calcular
// (pastilha sem consumo ou sem estoque no periodo).
public record IndicadorPastilhaResponse(
        Long id,
        String codigo,
        String descricao,
        UnidadeMedida unidadeMedida,
        BigDecimal saldoAtual,
        BigDecimal consumoPeriodo,
        BigDecimal consumoMedioDiario,
        BigDecimal pontoDePedido,
        boolean abaixoDoPontoDePedido,
        BigDecimal coberturaDias,
        BigDecimal giro,
        BigDecimal valorConsumido,
        BigDecimal percentualAcumulado,
        String classeAbc
) {
}
