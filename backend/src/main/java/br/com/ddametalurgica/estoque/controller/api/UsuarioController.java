package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.dto.AlterarSenhaRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioCreateRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioResponse;
import br.com.ddametalurgica.estoque.dto.UsuarioUpdateRequest;
import br.com.ddametalurgica.estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

// Controller da gestao de usuarios. So ADMIN acessa (ver SecurityConfig).
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestao de usuarios do sistema")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os usuarios com filtro e paginacao")
    public Page<UsuarioResponse> listar(
            @Parameter(description = "Trecho do nome ou do email") @RequestParam(required = false) String busca,
            @Parameter(description = "false lista os usuarios inativados") @RequestParam(defaultValue = "true") boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        return service.listar(busca, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuario pelo id")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um usuario novo")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioCreateRequest dados) {
        UsuarioResponse criado = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o nome e o perfil de um usuario")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid UsuarioUpdateRequest dados) {
        return service.atualizar(id, dados);
    }

    @PatchMapping("/{id}/senha")
    @Operation(summary = "Redefine a senha de um usuario")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id, @RequestBody @Valid AlterarSenhaRequest dados) {
        service.alterarSenha(id, dados);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa o usuario",
            description = "Nao apaga do banco, so marca como inativo para nao perder o historico.")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Volta o usuario para ativo")
    public UsuarioResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}
