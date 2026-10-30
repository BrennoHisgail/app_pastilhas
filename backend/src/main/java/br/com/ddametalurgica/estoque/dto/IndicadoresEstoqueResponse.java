package br.com.ddametalurgica.estoque.dto;

import java.util.List;

// Resultado da tela de indicadores: o periodo usado e uma linha por pastilha,
// ja na ordem da Curva ABC (quem mais consumiu dinheiro primeiro)
public record IndicadoresEstoqueResponse(
        int dias,
        long classeA,
        long classeB,
        long classeC,
        long abaixoDoPontoDePedido,
        List<IndicadorPastilhaResponse> itens
) {
}
