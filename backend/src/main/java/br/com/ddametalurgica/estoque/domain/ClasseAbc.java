package br.com.ddametalurgica.estoque.domain;

import java.math.BigDecimal;

// Classe da Curva ABC pelo valor consumido no periodo:
// A = ate 80% do valor acumulado, B = ate 95%, C = restante (inclui itens sem consumo)
public enum ClasseAbc {
    A, B, C;

    private static final BigDecimal LIMITE_A = new BigDecimal("80");
    private static final BigDecimal LIMITE_B = new BigDecimal("95");

    // Usa o percentual acumulado ANTES do item: o item de maior consumo
    // fica sempre em A, mesmo que sozinho passe de 80%
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
