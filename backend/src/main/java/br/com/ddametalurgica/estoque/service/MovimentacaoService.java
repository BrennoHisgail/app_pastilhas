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

// Registro de entradas e saidas e consulta do historico.
// @Transactional: o saldo da pastilha e a movimentacao sao gravados juntos;
// em caso de erro, nenhum dos dois e persistido.
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentação " + id + " não encontrada."));
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> historico(Long pastilhaId, LocalDate de, LocalDate ate, Pageable pageable) {
        // Datas ausentes viram limites amplos, para a consulta nao tratar parametro nulo
        LocalDateTime inicio = de == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.now().plusYears(1) : ate.atTime(LocalTime.MAX);

        if (inicio.isAfter(fim)) {
            throw new DadosInvalidosException("A data inicial não pode ser posterior à data final.");
        }

        return repository
                .buscar(pastilhaId == null ? 0L : pastilhaId, inicio, fim, pageable)
                .map(MovimentacaoResponse::de);
    }

    // Fluxo comum de entrada e saida
    private MovimentacaoResponse registrar(TipoMovimentacao tipo, MovimentacaoRequest dados) {
        // Trava a linha da pastilha ate o fim da transacao: sem isso, duas saidas
        // simultaneas poderiam ler o mesmo saldo e deixar o estoque negativo.
        Pastilha pastilha = pastilhaRepository.buscarParaMovimentar(dados.pastilhaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pastilha " + dados.pastilhaId() + " não encontrada."));

        // A Pastilha valida as regras (ativa, saldo suficiente). Como a entidade esta
        // gerenciada pelo JPA, o novo saldo e gravado no commit, sem save explicito.
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

    // Usuario autenticado, ou null fora de uma requisicao autenticada
    private Usuario usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioDetails detalhes)) {
            return null;
        }
        return detalhes.getUsuario();
    }

    // Campo opcional vazio ou so com espacos vira null
    private String textoOuNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
