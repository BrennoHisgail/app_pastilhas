package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.NivelMinimoRequest;
import br.com.ddametalurgica.estoque.dto.PastilhaRequest;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.service.PastilhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Endpoints do cadastro de pastilhas. Sem regra de negocio: delega ao PastilhaService.
@RestController
@RequestMapping("/api/pastilhas")
@Tag(name = "Pastilhas", description = "Cadastro e consulta das pastilhas")
public class PastilhaController {

    private final PastilhaService service;

    public PastilhaController(PastilhaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as pastilhas com filtros e paginação")
    public Page<PastilhaResponse> listar(
            @Parameter(description = "Trecho do código ou da descrição") @RequestParam(required = false) String busca,
            @Parameter(description = "Categoria exata") @RequestParam(required = false) String categoria,
            @Parameter(description = "false lista as pastilhas inativadas") @RequestParam(defaultValue = "true") boolean ativo,
            @Parameter(description = "true traz só os itens que precisam de reposição")
            @RequestParam(defaultValue = "false") boolean abaixoDoMinimo,
            @ParameterObject @PageableDefault(size = 20, sort = "codigo", direction = Sort.Direction.ASC) Pageable pageable) {

        return service.listar(busca, categoria, ativo, abaixoDoMinimo, pageable);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Lista as categorias cadastradas")
    public List<String> listarCategorias() {
        return service.listarCategorias();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma pastilha pelo id")
    public PastilhaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma pastilha nova",
            description = "O saldo não é informado no cadastro: ele entra por uma movimentação de entrada.")
    public ResponseEntity<PastilhaResponse> criar(@RequestBody @Valid PastilhaRequest dados) {
        PastilhaResponse criada = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/pastilhas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados de uma pastilha")
    public PastilhaResponse atualizar(@PathVariable Long id, @RequestBody @Valid PastilhaRequest dados) {
        return service.atualizar(id, dados);
    }

    @PatchMapping("/{id}/nivel-minimo")
    @Operation(summary = "Altera só o nível mínimo da pastilha")
    public PastilhaResponse alterarNivelMinimo(@PathVariable Long id, @RequestBody @Valid NivelMinimoRequest dados) {
        return service.alterarNivelMinimo(id, dados.nivelMinimo());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa a pastilha",
            description = "Não apaga do banco, só marca como inativa para não perder o histórico.")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Volta a pastilha para ativa")
    public PastilhaResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}
