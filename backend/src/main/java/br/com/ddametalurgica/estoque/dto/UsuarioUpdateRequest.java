package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corpo da edicao de usuario. E-mail e situacao (ativo) nao sao alterados por aqui.
@Schema(description = "Dados de edição de um usuário")
public record UsuarioUpdateRequest(

        @Schema(example = "Fulana da Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "OPERADOR")
        @NotNull(message = "O perfil é obrigatório")
        PerfilUsuario perfil
) {
}
