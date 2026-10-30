package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.IndicadoresEstoqueResponse;
import br.com.ddametalurgica.estoque.service.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller dos indicadores de estoque: Ponto de Pedido, Giro,
// Cobertura e Curva ABC
@RestController
@RequestMapping("/api/indicadores")
@Validated
@Tag(name = "Indicadores", description = "Numeros do painel inicial")
public class IndicadorController {

    private final IndicadorService service;

    public IndicadorController(IndicadorService service) {
        this.service = service;
    }

    @GetMapping("/estoque")
    @Operation(summary = "Ponto de Pedido, Giro, Cobertura e Curva ABC",
            description = "Calcula os indicadores de cada pastilha ativa a partir das movimentacoes "
                    + "dos ultimos N dias. A lista vem na ordem da Curva ABC.")
    public IndicadoresEstoqueResponse estoque(
            @Parameter(description = "Quantos dias para tras olhar, de 1 a 365")
            @RequestParam(defaultValue = "90")
            @Min(value = 1, message = "O periodo deve ter no minimo 1 dia")
            @Max(value = 365, message = "O periodo deve ter no maximo 365 dias") int dias) {
        return service.calcular(dias);
    }
}
