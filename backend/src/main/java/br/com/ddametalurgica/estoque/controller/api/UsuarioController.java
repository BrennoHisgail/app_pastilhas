package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.AlterarSenhaRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioCreateRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioResponse;
import br.com.ddametalurgica.estoque.dto.UsuarioUpdateRequest;
import br.com.ddametalurgica.estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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

// Endpoints da gestao de usuarios. Acesso restrito a ADMIN (ver SecurityConfig).
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários do sistema")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os usuários com filtro e paginação")
    public Page<UsuarioResponse> listar(
            @Parameter(description = "Trecho do nome ou do e-mail") @RequestParam(required = false) String busca,
            @Parameter(description = "false lista os usuários inativados") @RequestParam(defaultValue = "true") boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        return service.listar(busca, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo id")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Usuário criado",
            headers = @Header(name = "Location", description = "Endereço do registro criado"),
            content = @Content(schema = @Schema(implementation = UsuarioResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um usuário com o e-mail informado", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @Operation(summary = "Cadastra um usuário novo")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioCreateRequest dados) {
        UsuarioResponse criado = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o nome e o perfil de um usuário")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid UsuarioUpdateRequest dados) {
        return service.atualizar(id, dados);
    }

    @PatchMapping("/{id}/senha")
    @ApiResponse(responseCode = "204", description = "Senha alterada")
    @Operation(summary = "Redefine a senha de um usuário")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id, @RequestBody @Valid AlterarSenhaRequest dados) {
        service.alterarSenha(id, dados);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Usuário inativado (repetir o pedido também devolve 204)")
    @ApiResponse(responseCode = "409", description = "Inativar o próprio usuário ou o último administrador ativo", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @Operation(summary = "Inativa o usuário",
            description = "Não apaga do banco, só marca como inativo para não perder o histórico.")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Volta o usuário para ativo")
    public UsuarioResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}
