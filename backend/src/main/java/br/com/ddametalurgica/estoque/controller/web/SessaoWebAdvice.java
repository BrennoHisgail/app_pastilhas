package br.com.ddametalurgica.estoque.controller.web;

import br.com.ddametalurgica.estoque.security.UsuarioDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Disponibiliza o usuario logado no model de todas as telas (nome e perfil no menu)
@ControllerAdvice(basePackages = "br.com.ddametalurgica.estoque.controller.web")
public class SessaoWebAdvice {

    @ModelAttribute("usuarioLogado")
    public UsuarioSessaoView usuarioLogado(@AuthenticationPrincipal UsuarioDetails usuario) {
        return usuario == null ? null : UsuarioSessaoView.de(usuario.getUsuario());
    }
}
