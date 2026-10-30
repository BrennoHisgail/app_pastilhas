package br.com.ddametalurgica.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Usado na tela que redefine a senha de um usuario
@Schema(description = "Nova senha do usuario")
public record AlterarSenhaRequest(

        @Schema(example = "novaSenha123")
        @NotBlank(message = "A nova senha e obrigatoria")
        @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres")
        String novaSenha
) {
}
