package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Corpo do cadastro e da edicao de pastilha.
// Sem campo de saldo: o saldo so muda por movimentacao de entrada ou saida.
@Schema(description = "Dados de cadastro/edição de uma pastilha")
public record PastilhaRequest(

        @Schema(example = "CNMG120408")
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 40, message = "O código deve ter no máximo 40 caracteres")
        String codigo,

        @Schema(example = "Pastilha de torneamento CNMG 120408 metal duro")
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
        String descricao,

        @Schema(example = "Torneamento")
        @NotBlank(message = "A categoria é obrigatória")
        @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres")
        String categoria,

        @Schema(example = "UN")
        @NotNull(message = "A unidade de medida é obrigatória")
        UnidadeMedida unidadeMedida,

        @Schema(description = "Saldo a partir do qual o item entra na lista de reposição", example = "50")
        @NotNull(message = "O nível mínimo é obrigatório")
        @PositiveOrZero(message = "O nível mínimo não pode ser negativo")
        BigDecimal nivelMinimo,

        @Schema(example = "38.90")
        @NotNull(message = "O custo unitário é obrigatório")
        @PositiveOrZero(message = "O custo unitário não pode ser negativo")
        BigDecimal custoUnitario,

        @Schema(description = "Tempo de reposição em dias (t do Ponto de Pedido)", example = "10")
        @NotNull(message = "O lead time é obrigatório")
        @PositiveOrZero(message = "O lead time não pode ser negativo")
        Integer leadTimeDias,

        @Schema(description = "Reserva técnica (Qs do Ponto de Pedido)", example = "20")
        @NotNull(message = "O estoque de segurança é obrigatório")
        @PositiveOrZero(message = "O estoque de segurança não pode ser negativo")
        BigDecimal estoqueSeguranca
) {
}
