package br.com.ddametalurgica.estoque.repository;

import java.math.BigDecimal;

// Contadores do painel, que vem juntos do PastilhaRepository.buscarResumo()
public record ResumoPastilhas(long ativas, long categorias, long abaixoDoMinimo, BigDecimal valorEmEstoque) {
}
