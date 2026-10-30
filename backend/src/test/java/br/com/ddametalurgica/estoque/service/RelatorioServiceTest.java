package br.com.ddametalurgica.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.ddametalurgica.estoque.Fabrica;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioService - exportacao em CSV")
class RelatorioServiceTest {

    @Mock
    private PastilhaRepository pastilhaRepository;

    @Mock
    private MovimentacaoService movimentacaoService;

    @InjectMocks
    private RelatorioService service;

    private String texto(byte[] arquivo) {
        return new String(arquivo, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("estoque: BOM, cabecalho e uma linha por pastilha com decimal em virgula")
    void estoque() {
        when(pastilhaRepository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(List.of(
                Fabrica.pastilha("CNMG120408", new BigDecimal("12.5"), new BigDecimal("20"))));

        String[] linhas = texto(service.estoque()).split("\r\n");

        assertThat(linhas[0]).startsWith("﻿Codigo;Descricao;Categoria");
        // saldo 12,5 abaixo do minimo 20; valor = 12,5 x 10 = 125
        assertThat(linhas[1]).isEqualTo(
                "CNMG120408;Pastilha de teste CNMG120408;Torneamento;UN;12,5;20;10;125;Repor");
    }

    @Test
    @DisplayName("movimentacoes: usa os filtros do historico e mostra o responsavel")
    void movimentacoes() {
        MovimentacaoResponse m = new MovimentacaoResponse(
                1L, 7L, "CNMG120408", "Pastilha", TipoMovimentacao.SAIDA,
                new BigDecimal("3.000"), new BigDecimal("97.000"), LocalDateTime.of(2026, 10, 5, 14, 30),
                "OP 55", "Torno 2", "obs; com ponto e virgula", "Operador Demo");
        when(movimentacaoService.historico(eq(7L), any(), any(), any())).thenReturn(new PageImpl<>(List.of(m)));

        String[] linhas = texto(service.movimentacoes(7L, null, null)).split("\r\n");

        assertThat(linhas).hasSize(2);
        assertThat(linhas[1]).isEqualTo(
                "05/10/2026 14:30;CNMG120408;Pastilha;SAIDA;3;97;OP 55;Torno 2;Operador Demo;\"obs; com ponto e virgula\"");
    }

    @Test
    @DisplayName("campo com aspas fica entre aspas e com as aspas dobradas")
    void escapaAspas() {
        assertThat(RelatorioService.campo("pastilha \"especial\"")).isEqualTo("\"pastilha \"\"especial\"\"\"");
        assertThat(RelatorioService.campo(null)).isEmpty();
    }
}
