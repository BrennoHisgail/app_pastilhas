package br.com.ddametalurgica.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Dados que a tela envia para registrar uma entrada ou saida
@Schema(description = "Dados de uma movimentacao de estoque")
public record MovimentacaoRequest(

        @Schema(example = "1")
        @NotNull(message = "A pastilha e obrigatoria")
        Long pastilhaId,

        @Schema(example = "25")
        @NotNull(message = "A quantidade e obrigatoria")
        @Positive(message = "A quantidade deve ser maior que zero")
        BigDecimal quantidade,

        @Schema(description = "Nota fiscal, ordem de producao ou requisicao", example = "NF 12345")
        @Size(max = 60, message = "O documento deve ter no maximo 60 caracteres")
        String documento,

        @Schema(description = "Fornecedor na entrada, setor ou ordem na saida", example = "Fornecedor Sandvik")
        @Size(max = 160, message = "A origem/destino deve ter no maximo 160 caracteres")
        String origemDestino,

        @Size(max = 400, message = "A observacao deve ter no maximo 400 caracteres")
        String observacao
) {
}
