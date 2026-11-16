package br.com.ddametalurgica.estoque.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

// Corpo do PATCH que altera apenas o nivel minimo
public record NivelMinimoRequest(

        @NotNull(message = "O nível mínimo é obrigatório")
        @PositiveOrZero(message = "O nível mínimo não pode ser negativo")
        BigDecimal nivelMinimo
) {
}
