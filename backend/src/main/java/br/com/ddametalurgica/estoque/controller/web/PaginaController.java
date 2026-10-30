package br.com.ddametalurgica.estoque.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

// Controller das telas. Ele so devolve o nome do arquivo HTML
// que o Thymeleaf tem que abrir.
//
// Os dados nao vem por aqui: cada tela busca o que precisa
// chamando a API com JavaScript.
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
}
