package br.com.ddametalurgica.estoque.controller.web;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;

// Dados do usuario logado que aparecem no menu de cima
public record UsuarioSessaoView(String nome, String iniciais, boolean admin) {

    public static UsuarioSessaoView de(Usuario usuario) {
        return new UsuarioSessaoView(
                usuario.getNome(),
                iniciais(usuario.getNome()),
                usuario.getPerfil() == PerfilUsuario.ADMIN);
    }

    // "Joao da Silva" vira "JS"
    private static String iniciais(String nome) {
        String[] partes = nome.trim().split("\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase();
    }
}
