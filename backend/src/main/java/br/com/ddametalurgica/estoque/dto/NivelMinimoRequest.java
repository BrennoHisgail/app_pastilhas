package br.com.ddametalurgica.estoque.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

// Usado na tela que altera so o nivel minimo da pastilha
public record NivelMinimoRequest(

        @NotNull(message = "O nivel minimo e obrigatorio")
        @PositiveOrZero(message = "O nivel minimo nao pode ser negativo")
        BigDecimal nivelMinimo
) {
}
