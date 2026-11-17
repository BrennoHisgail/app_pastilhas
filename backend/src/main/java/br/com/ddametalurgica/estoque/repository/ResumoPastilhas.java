package br.com.ddametalurgica.estoque.repository;

import java.math.BigDecimal;

// Projecao com os contadores do painel (PastilhaRepository.buscarResumo)
public record ResumoPastilhas(long ativas, long categorias, long abaixoDoMinimo, BigDecimal valorEmEstoque) {
}
