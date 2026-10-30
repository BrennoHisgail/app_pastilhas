package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import java.math.BigDecimal;

// Quanto entrou ou saiu de uma pastilha num periodo, ja somado pelo banco
public record TotalMovimentado(Long pastilhaId, TipoMovimentacao tipo, BigDecimal quantidade) {
}
