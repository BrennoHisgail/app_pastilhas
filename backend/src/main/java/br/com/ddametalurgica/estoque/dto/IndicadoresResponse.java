package br.com.ddametalurgica.estoque.dto;

import java.math.BigDecimal;
import java.util.List;

// Record com todos os numeros que o painel mostra de uma vez,
// assim a tela faz uma chamada so na API.
public record IndicadoresResponse(
        long itensCadastrados,
        long categorias,
        long itensAbaixoDoMinimo,
        BigDecimal valorEmEstoque,
        long movimentacoesUltimos30Dias,
        List<PastilhaResponse> alertasReposicao,
        List<MovimentacaoResponse> movimentacoesRecentes
) {
}
