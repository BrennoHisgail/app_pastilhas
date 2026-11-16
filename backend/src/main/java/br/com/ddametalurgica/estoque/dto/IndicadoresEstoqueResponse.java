package br.com.ddametalurgica.estoque.dto;

import java.util.List;

// Resultado dos indicadores: periodo usado, totais por classe e uma linha por pastilha,
// na ordem da Curva ABC (maior valor consumido primeiro)
public record IndicadoresEstoqueResponse(
        int dias,
        long classeA,
        long classeB,
        long classeC,
        long abaixoDoPontoDePedido,
        List<IndicadorPastilhaResponse> itens
) {
}
