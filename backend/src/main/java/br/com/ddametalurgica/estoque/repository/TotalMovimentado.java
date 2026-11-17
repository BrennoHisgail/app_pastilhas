package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import java.math.BigDecimal;

// Projecao: total de entrada ou saida de uma pastilha no periodo, somado no banco
public record TotalMovimentado(Long pastilhaId, TipoMovimentacao tipo, BigDecimal quantidade) {
}
