package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Resposta da API para uma movimentacao
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
        String observacao,
        String usuarioNome
) {

    // Converte a entidade em DTO
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
                m.getObservacao(),
                m.getUsuario() == null ? null : m.getUsuario().getNome());
    }
}
