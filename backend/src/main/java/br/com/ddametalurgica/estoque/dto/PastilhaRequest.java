package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Dados que o formulario manda para cadastrar ou editar uma pastilha.
//
// Nao tem campo de saldo aqui de proposito: o saldo so muda
// registrando entrada ou saida na tela de movimentacao.
@Schema(description = "Dados de cadastro/edicao de uma pastilha")
public record PastilhaRequest(

        @Schema(example = "CNMG120408")
        @NotBlank(message = "O codigo e obrigatorio")
        @Size(max = 40, message = "O codigo deve ter no maximo 40 caracteres")
        String codigo,

        @Schema(example = "Pastilha de torneamento CNMG 120408 metal duro")
        @NotBlank(message = "A descricao e obrigatoria")
        @Size(max = 200, message = "A descricao deve ter no maximo 200 caracteres")
        String descricao,

        @Schema(example = "Torneamento")
        @NotBlank(message = "A categoria e obrigatoria")
        @Size(max = 80, message = "A categoria deve ter no maximo 80 caracteres")
        String categoria,

        @Schema(example = "UN")
        @NotNull(message = "A unidade de medida e obrigatoria")
        UnidadeMedida unidadeMedida,

        @Schema(description = "Saldo a partir do qual o item entra na lista de reposicao", example = "50")
        @NotNull(message = "O nivel minimo e obrigatorio")
        @PositiveOrZero(message = "O nivel minimo nao pode ser negativo")
        BigDecimal nivelMinimo,

        @Schema(example = "38.90")
        @NotNull(message = "O custo unitario e obrigatorio")
        @PositiveOrZero(message = "O custo unitario nao pode ser negativo")
        BigDecimal custoUnitario,

        @Schema(description = "Tempo de reposicao em dias (t do Ponto de Pedido)", example = "10")
        @NotNull(message = "O lead time e obrigatorio")
        @PositiveOrZero(message = "O lead time nao pode ser negativo")
        Integer leadTimeDias,

        @Schema(description = "Reserva tecnica (Qs do Ponto de Pedido)", example = "20")
        @NotNull(message = "O estoque de seguranca e obrigatorio")
        @PositiveOrZero(message = "O estoque de seguranca nao pode ser negativo")
        BigDecimal estoqueSeguranca
) {
}
