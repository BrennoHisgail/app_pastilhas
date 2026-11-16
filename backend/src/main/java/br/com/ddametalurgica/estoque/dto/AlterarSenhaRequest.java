package br.com.ddametalurgica.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Redefinicao de senha feita pelo ADMIN
@Schema(description = "Nova senha do usuário")
public record AlterarSenhaRequest(

        @Schema(example = "novaSenha123")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String novaSenha
) {
}
