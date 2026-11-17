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
// Formato compativel com o Excel em portugues: separador ";", decimal com
// virgula e BOM no inicio para o UTF-8 ser reconhecido.
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
        linha(csv, "Código", "Descrição", "Categoria", "Unidade", "Saldo", "Nível mínimo",
                "Custo unitário", "Valor em estoque", "Situação");

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

    // Mesmos filtros do historico, sem paginacao
    @Transactional(readOnly = true)
    public byte[] movimentacoes(Long pastilhaId, LocalDate de, LocalDate ate) {
        List<MovimentacaoResponse> lista = movimentacaoService
                .historico(pastilhaId, de, ate, Pageable.unpaged())
                .getContent();

        StringBuilder csv = new StringBuilder(BOM);
        linha(csv, "Data", "Código", "Descrição", "Tipo", "Quantidade", "Saldo após",
                "Documento", "Origem/Destino", "Responsável", "Observação");

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

    // Escapa ; aspas e quebras de linha (RFC 4180): campo entre aspas e aspas internas dobradas
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
