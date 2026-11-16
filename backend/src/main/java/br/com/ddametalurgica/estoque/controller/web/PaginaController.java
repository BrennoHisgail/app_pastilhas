package br.com.ddametalurgica.estoque.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

// Rotas das telas: devolve apenas o template Thymeleaf.
// Os dados sao carregados pelo JavaScript de cada tela, via API REST.
@Controller
public class PaginaController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String painel() {
        return "painel";
    }

    @GetMapping("/pastilhas")
    public String pastilhas() {
        return "pastilhas/lista";
    }

    @GetMapping("/pastilhas/nova")
    public String novaPastilha(Model model) {
        model.addAttribute("pastilhaId", "");
        return "pastilhas/form";
    }

    @GetMapping("/pastilhas/{id}/editar")
    public String editarPastilha(@PathVariable Long id, Model model) {
        model.addAttribute("pastilhaId", id);
        return "pastilhas/form";
    }

    @GetMapping("/movimentacoes")
    public String movimentacoes() {
        return "movimentacoes";
    }

    @GetMapping("/indicadores")
    public String indicadores() {
        return "indicadores";
    }

    @GetMapping("/usuarios")
    public String usuarios() {
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuario(Model model) {
        model.addAttribute("usuarioId", "");
        return "usuarios/form";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuarioId", id);
        return "usuarios/form";
    }
}
