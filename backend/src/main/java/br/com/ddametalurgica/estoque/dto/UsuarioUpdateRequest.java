package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados para editar um usuario. O email e o status ativo nao mudam por aqui.
@Schema(description = "Dados de edicao de um usuario")
public record UsuarioUpdateRequest(

        @Schema(example = "Fulana da Silva")
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
        String nome,

        @Schema(example = "OPERADOR")
        @NotNull(message = "O perfil e obrigatorio")
        PerfilUsuario perfil
) {
}
