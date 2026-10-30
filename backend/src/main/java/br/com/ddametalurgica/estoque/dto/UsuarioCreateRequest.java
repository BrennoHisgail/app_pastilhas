package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados para cadastrar um usuario novo.
@Schema(description = "Dados de cadastro de um usuario")
public record UsuarioCreateRequest(

        @Schema(example = "Fulana da Silva")
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
        String nome,

        @Schema(example = "fulana@dda.com.br")
        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "Informe um email valido")
        @Size(max = 160, message = "O email deve ter no maximo 160 caracteres")
        String email,

        @Schema(example = "senha123")
        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres")
        String senha,

        @Schema(example = "OPERADOR")
        @NotNull(message = "O perfil e obrigatorio")
        PerfilUsuario perfil
) {
}
