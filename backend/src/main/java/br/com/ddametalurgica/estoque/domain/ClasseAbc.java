package br.com.ddametalurgica.estoque.domain;

import java.math.BigDecimal;

/**
 * Classe da Curva ABC de uma pastilha, pelo valor consumido no periodo.
 *
 * A: itens que somam ate 80% do valor consumido (poucos itens, muito dinheiro)
 * B: de 80% ate 95%
 * C: o resto, e tambem quem nao teve consumo
 */
public enum ClasseAbc {
    A, B, C;

    private static final BigDecimal LIMITE_A = new BigDecimal("80");
    private static final BigDecimal LIMITE_B = new BigDecimal("95");

    /**
     * Usa o percentual acumulado ANTES do item, assim o item que mais
     * consome sempre fica na classe A, mesmo sozinho passando de 80%.
     */
    public static ClasseAbc classificar(BigDecimal percentualAcumuladoAntes, BigDecimal valorConsumido) {
        if (valorConsumido.signum() == 0) {
            return C;
        }
        if (percentualAcumuladoAntes.compareTo(LIMITE_A) < 0) {
            return A;
        }
        if (percentualAcumuladoAntes.compareTo(LIMITE_B) < 0) {
            return B;
        }
        return C;
    }
}
