package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Dados da movimentacao que a API devolve para a tela.
// Usamos record para nao precisar escrever get de cada campo.
public record MovimentacaoResponse(
        Long id,
        Long pastilhaId,
        String pastilhaCodigo,
        String pastilhaDescricao,
        TipoMovimentacao tipo,
        BigDecimal quantidade,
        BigDecimal saldoApos,
        LocalDateTime dataHora,
        String documento,
        String origemDestino,
        String observacao
) {

    // Converte a entidade do banco neste record
    public static MovimentacaoResponse de(Movimentacao m) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getPastilha().getId(),
                m.getPastilha().getCodigo(),
                m.getPastilha().getDescricao(),
                m.getTipo(),
                m.getQuantidade(),
                m.getSaldoApos(),
                m.getDataHora(),
                m.getDocumento(),
                m.getOrigemDestino(),
                m.getObservacao());
    }
}
