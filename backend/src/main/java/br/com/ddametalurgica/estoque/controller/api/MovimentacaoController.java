package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.MovimentacaoRequest;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.service.MovimentacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller das entradas e saidas de estoque
@RestController
@RequestMapping("/api/movimentacoes")
@Tag(name = "Movimentacoes", description = "Entradas, saidas e historico do estoque")
public class MovimentacaoController {

    private final MovimentacaoService service;

    public MovimentacaoController(MovimentacaoService service) {
        this.service = service;
    }

    @PostMapping("/entrada")
    @Operation(summary = "Registra uma entrada",
            description = "Soma a quantidade no saldo da pastilha.")
    public ResponseEntity<MovimentacaoResponse> entrada(@RequestBody @Valid MovimentacaoRequest dados) {
        MovimentacaoResponse criada = service.registrarEntrada(dados);
        return ResponseEntity.created(URI.create("/api/movimentacoes/" + criada.id())).body(criada);
    }

    @PostMapping("/saida")
    @Operation(summary = "Registra uma saida",
            description = "Subtrai do saldo. Se nao tiver estoque suficiente devolve erro.")
    public ResponseEntity<MovimentacaoResponse> saida(@RequestBody @Valid MovimentacaoRequest dados) {
        MovimentacaoResponse criada = service.registrarSaida(dados);
        return ResponseEntity.created(URI.create("/api/movimentacoes/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Consulta o historico",
            description = "Filtra por pastilha e por periodo, do mais novo para o mais antigo.")
    public Page<MovimentacaoResponse> historico(
            @Parameter(description = "Filtra por pastilha; ausente traz todas") @RequestParam(required = false) Long pastilhaId,
            @Parameter(description = "Data inicial, formato aaaa-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Data final, formato aaaa-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        return service.historico(pastilhaId, de, ate, pageable);
    }
}
