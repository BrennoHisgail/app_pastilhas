package br.com.ddametalurgica.estoque.dto;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.UnidadeMedida;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Dados da pastilha que a API devolve.
// A entidade Pastilha nunca sai do service, sempre convertemos para este record.
public record PastilhaResponse(
        Long id,
        String codigo,
        String descricao,
        String categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal nivelMinimo,
        BigDecimal saldoAtual,
        BigDecimal custoUnitario,
        BigDecimal valorEmEstoque,
        Integer leadTimeDias,
        BigDecimal estoqueSeguranca,
        boolean abaixoDoMinimo,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static PastilhaResponse de(Pastilha p) {
        return new PastilhaResponse(
                p.getId(),
                p.getCodigo(),
                p.getDescricao(),
                p.getCategoria(),
                p.getUnidadeMedida(),
                p.getNivelMinimo(),
                p.getSaldoAtual(),
                p.getCustoUnitario(),
                p.getValorEmEstoque(),
                p.getLeadTimeDias(),
                p.getEstoqueSeguranca(),
                p.isAbaixoDoMinimo(),
                p.isAtivo(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
