package br.com.ddametalurgica.estoque.controller.api;

import br.com.ddametalurgica.estoque.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller dos relatorios em CSV (RF14). O navegador baixa o arquivo
// direto, por isso o cabecalho Content-Disposition: attachment.
@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatorios", description = "Exportacao do estoque e das movimentacoes em CSV")
public class RelatorioController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/estoque.csv")
    @Operation(summary = "Posicao do estoque em CSV", description = "Todas as pastilhas ativas com saldo e valor.")
    public ResponseEntity<byte[]> estoque() {
        return arquivo("estoque-" + LocalDate.now() + ".csv", service.estoque());
    }

    @GetMapping("/movimentacoes.csv")
    @Operation(summary = "Movimentacoes em CSV", description = "Mesmos filtros do historico, sem paginacao.")
    public ResponseEntity<byte[]> movimentacoes(
            @Parameter(description = "Filtra por pastilha; ausente traz todas") @RequestParam(required = false) Long pastilhaId,
            @Parameter(description = "Data inicial (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Data final (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return arquivo("movimentacoes-" + LocalDate.now() + ".csv", service.movimentacoes(pastilhaId, de, ate));
    }

    private ResponseEntity<byte[]> arquivo(String nome, byte[] conteudo) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nome).build().toString())
                .body(conteudo);
    }
}
