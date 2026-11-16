package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.domain.Movimentacao;
import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.dto.MovimentacaoRequest;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.exception.DadosInvalidosException;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.security.UsuarioDetails;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Classe que cuida das entradas e saidas do estoque.
// O @Transactional faz o saldo e a movimentacao serem gravados juntos:
// se der erro no meio, o banco desfaz os dois.
@Service
public class MovimentacaoService {

    private final MovimentacaoRepository repository;
    private final PastilhaRepository pastilhaRepository;

    public MovimentacaoService(MovimentacaoRepository repository, PastilhaRepository pastilhaRepository) {
        this.repository = repository;
        this.pastilhaRepository = pastilhaRepository;
    }

    @Transactional
    public MovimentacaoResponse registrarEntrada(MovimentacaoRequest dados) {
        return registrar(TipoMovimentacao.ENTRADA, dados);
    }

    @Transactional
    public MovimentacaoResponse registrarSaida(MovimentacaoRequest dados) {
        return registrar(TipoMovimentacao.SAIDA, dados);
    }

    @Transactional(readOnly = true)
    public MovimentacaoResponse buscarPorId(Long id) {
        return repository.findComDetalhesById(id)
                .map(MovimentacaoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentacao " + id + " nao encontrada."));
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> historico(Long pastilhaId, LocalDate de, LocalDate ate, Pageable pageable) {
        // Quando o usuario nao informa as datas usamos um periodo bem grande,
        // assim a consulta do repositorio nao precisa tratar campo nulo.
        LocalDateTime inicio = de == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.now().plusYears(1) : ate.atTime(LocalTime.MAX);

        if (inicio.isAfter(fim)) {
            throw new DadosInvalidosException("A data inicial nao pode ser posterior a data final.");
        }

        return repository
                .buscar(pastilhaId == null ? 0L : pastilhaId, inicio, fim, pageable)
                .map(MovimentacaoResponse::de);
    }

    // Metodo que a entrada e a saida usam, para nao repetir codigo
    private MovimentacaoResponse registrar(TipoMovimentacao tipo, MovimentacaoRequest dados) {
        // Trava a linha da pastilha ate o fim da transacao. Sem isso, duas
        // saidas simultaneas poderiam ler o mesmo saldo e zerar o estoque.
        Pastilha pastilha = pastilhaRepository.buscarParaMovimentar(dados.pastilhaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pastilha " + dados.pastilhaId() + " nao encontrada."));

        // A propria pastilha confere as regras (ativa, saldo suficiente) e
        // lanca OperacaoNaoPermitidaException se nao puder movimentar.
        // Nao precisa chamar save na pastilha: o JPA grava a mudanca
        // sozinho no fim da transacao
        if (tipo == TipoMovimentacao.ENTRADA) {
            pastilha.registrarEntrada(dados.quantidade());
        } else {
            pastilha.registrarSaida(dados.quantidade());
        }

        Movimentacao movimentacao = new Movimentacao(
                pastilha,
                tipo,
                dados.quantidade(),
                textoOuNulo(dados.documento()),
                textoOuNulo(dados.origemDestino()),
                textoOuNulo(dados.observacao()),
                usuarioLogado());

        return MovimentacaoResponse.de(repository.save(movimentacao));
    }

    // Quem esta lancando, ou null se ninguem estiver logado
    private Usuario usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioDetails detalhes)) {
            return null;
        }
        return detalhes.getUsuario();
    }

    // Deixa null quando o campo opcional vem vazio ou so com espacos
    private String textoOuNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
