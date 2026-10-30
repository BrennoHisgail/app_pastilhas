package br.com.ddametalurgica.estoque;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.dto.PastilhaRequest;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Monta objetos prontos para os testes, para nao repetir o mesmo
 * bloco de setters em cada classe de teste.
 */
public final class Fabrica {

    private Fabrica() {
    }

    /** Pastilha ativa, com saldo 100 e nivel minimo 20. */
    public static Pastilha pastilha(String codigo) {
        return pastilha(codigo, new BigDecimal("100"), new BigDecimal("20"));
    }

    public static Pastilha pastilha(String codigo, BigDecimal saldo, BigDecimal nivelMinimo) {
        Pastilha pastilha = new Pastilha();
        pastilha.setCodigo(codigo);
        pastilha.setDescricao("Pastilha de teste " + codigo);
        pastilha.setCategoria("Torneamento");
        pastilha.setUnidadeMedida(UnidadeMedida.UN);
        pastilha.setNivelMinimo(nivelMinimo);
        pastilha.setCustoUnitario(new BigDecimal("10.00"));
        pastilha.setLeadTimeDias(7);
        pastilha.setEstoqueSeguranca(new BigDecimal("5"));
        // saldoAtual nao tem set de proposito: so entra por movimentacao
        if (saldo.signum() > 0) {
            pastilha.registrarEntrada(saldo);
        }
        return pastilha;
    }

    /**
     * Preenche o id da pastilha. No sistema quem gera o id e o banco;
     * nos testes de unidade nao existe banco, entao gravamos direto.
     */
    public static Pastilha comId(Pastilha pastilha, Long id) {
        ReflectionTestUtils.setField(pastilha, "id", id);
        return pastilha;
    }

    /** Requisicao de cadastro valida, usada como base nos testes. */
    public static PastilhaRequest requisicao(String codigo) {
        return new PastilhaRequest(
                codigo,
                "Pastilha de teste " + codigo,
                "Torneamento",
                UnidadeMedida.UN,
                new BigDecimal("20"),
                new BigDecimal("10.00"),
                7,
                new BigDecimal("5"));
    }

    /** Usuario ativo, perfil OPERADOR, com um hash de senha qualquer. */
    public static Usuario usuario(String email) {
        return usuario(email, PerfilUsuario.OPERADOR);
    }

    public static Usuario usuario(String email, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de teste");
        usuario.setEmail(email);
        usuario.definirSenha("$2a$10$hashDeTesteFalsoNaoUsarEmProducao.......");
        usuario.setPerfil(perfil);
        return usuario;
    }

    /**
     * Preenche o id do usuario. No sistema quem gera o id e o banco;
     * nos testes de unidade nao existe banco, entao gravamos direto.
     */
    public static Usuario comId(Usuario usuario, Long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
