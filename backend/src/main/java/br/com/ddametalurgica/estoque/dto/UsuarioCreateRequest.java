package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corpo do cadastro de usuario
@Schema(description = "Dados de cadastro de um usuário")
public record UsuarioCreateRequest(

        @Schema(example = "Fulana da Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "fulana@dda.com.br")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @Schema(example = "senha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String senha,

        @Schema(example = "OPERADOR")
        @NotNull(message = "O perfil é obrigatório")
        PerfilUsuario perfil
) {
}
