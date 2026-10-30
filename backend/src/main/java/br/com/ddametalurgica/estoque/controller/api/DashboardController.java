package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.IndicadoresResponse;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller que alimenta o painel inicial do sistema
@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Indicadores", description = "Numeros do painel inicial")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard/indicadores")
    @Operation(summary = "Numeros do painel",
            description = "Itens cadastrados, itens em falta, valor em estoque e ultimas movimentacoes.")
    public IndicadoresResponse indicadores() {
        return service.indicadores();
    }

    @GetMapping("/alertas/reposicao")
    @Operation(summary = "Itens que precisam de reposicao",
            description = "Pastilhas que chegaram no nivel minimo, as mais criticas primeiro.")
    public List<PastilhaResponse> alertas(
            @Parameter(description = "Quantos itens trazer, de 1 a 200")
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "O limite deve ser no minimo 1")
            @Max(value = 200, message = "O limite deve ser no maximo 200") int limite) {
        return service.alertasReposicao(limite);
    }
}
