package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Gera os relatorios em CSV (RF14).
//
// O arquivo sai no formato que o Excel em portugues abre direto:
// separador ";", decimal com virgula e o BOM no comeco para ele
// reconhecer os acentos em UTF-8.
@Service
public class RelatorioService {

    private static final String BOM = "﻿";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PastilhaRepository pastilhaRepository;
    private final MovimentacaoService movimentacaoService;

    public RelatorioService(PastilhaRepository pastilhaRepository, MovimentacaoService movimentacaoService) {
        this.pastilhaRepository = pastilhaRepository;
        this.movimentacaoService = movimentacaoService;
    }

    @Transactional(readOnly = true)
    public byte[] estoque() {
        StringBuilder csv = new StringBuilder(BOM);
        linha(csv, "Codigo", "Descricao", "Categoria", "Unidade", "Saldo", "Nivel minimo",
                "Custo unitario", "Valor em estoque", "Situacao");

        for (Pastilha p : pastilhaRepository.findByAtivoTrueOrderByCodigoAsc()) {
            linha(csv,
                    p.getCodigo(),
                    p.getDescricao(),
                    p.getCategoria(),
                    p.getUnidadeMedida().name(),
                    numero(p.getSaldoAtual()),
                    numero(p.getNivelMinimo()),
                    numero(p.getCustoUnitario()),
                    numero(p.getValorEmEstoque()),
                    p.isAbaixoDoMinimo() ? "Repor" : "Normal");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Usa os mesmos filtros da tela de historico, mas sem paginacao
    @Transactional(readOnly = true)
    public byte[] movimentacoes(Long pastilhaId, LocalDate de, LocalDate ate) {
        List<MovimentacaoResponse> lista = movimentacaoService
                .historico(pastilhaId, de, ate, Pageable.unpaged())
                .getContent();

        StringBuilder csv = new StringBuilder(BOM);
        linha(csv, "Data", "Codigo", "Descricao", "Tipo", "Quantidade", "Saldo apos",
                "Documento", "Origem/Destino", "Responsavel", "Observacao");

        for (MovimentacaoResponse m : lista) {
            linha(csv,
                    m.dataHora().format(DATA_HORA),
                    m.pastilhaCodigo(),
                    m.pastilhaDescricao(),
                    m.tipo().name(),
                    numero(m.quantidade()),
                    numero(m.saldoApos()),
                    m.documento(),
                    m.origemDestino(),
                    m.usuarioNome(),
                    m.observacao());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void linha(StringBuilder csv, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(campo(campos[i]));
        }
        csv.append("\r\n");
    }

    // Texto com ; aspas ou quebra de linha vai entre aspas, com as
    // aspas de dentro dobradas, senao quebraria as colunas
    static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    // 1234.500 vira 1234,5
    static String numero(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
