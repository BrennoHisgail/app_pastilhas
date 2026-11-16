package br.com.ddametalurgica.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Corpo da requisicao de entrada ou saida
@Schema(description = "Dados de uma movimentação de estoque")
public record MovimentacaoRequest(

        @Schema(example = "1")
        @NotNull(message = "A pastilha é obrigatória")
        Long pastilhaId,

        @Schema(example = "25")
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        BigDecimal quantidade,

        @Schema(description = "Nota fiscal, ordem de produção ou requisição", example = "NF 12345")
        @Size(max = 60, message = "O documento deve ter no máximo 60 caracteres")
        String documento,

        @Schema(description = "Fornecedor na entrada, setor ou ordem na saída", example = "Fornecedor Sandvik")
        @Size(max = 160, message = "A origem/destino deve ter no máximo 160 caracteres")
        String origemDestino,

        @Size(max = 400, message = "A observação deve ter no máximo 400 caracteres")
        String observacao
) {
}
