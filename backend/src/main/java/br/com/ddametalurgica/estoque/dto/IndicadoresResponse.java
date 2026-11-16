package br.com.ddametalurgica.estoque.dto;

import java.math.BigDecimal;
import java.util.List;

// Dados do painel inicial agregados numa unica resposta (uma chamada por carregamento)
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
