package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import java.time.LocalDateTime;

// Dados do usuario que a API devolve.
// Nao tem o hash da senha, para ele nunca ir para a tela.
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil(),
                u.isAtivo(),
                u.getCriadoEm(),
                u.getAtualizadoEm());
    }
}
