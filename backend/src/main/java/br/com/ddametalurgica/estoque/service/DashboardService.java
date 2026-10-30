package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.dto.IndicadoresResponse;
import br.com.ddametalurgica.estoque.dto.MovimentacaoResponse;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.repository.ResumoPastilhas;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Monta os numeros que aparecem no painel inicial
@Service
public class DashboardService {

    private static final int LIMITE_ALERTAS = 10;

    private final PastilhaRepository pastilhaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public DashboardService(PastilhaRepository pastilhaRepository, MovimentacaoRepository movimentacaoRepository) {
        this.pastilhaRepository = pastilhaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional(readOnly = true)
    public IndicadoresResponse indicadores() {
        List<PastilhaResponse> alertas = pastilhaRepository
                .listarAbaixoDoMinimo(PageRequest.of(0, LIMITE_ALERTAS))
                .stream()
                .map(PastilhaResponse::de)
                .toList();

        List<MovimentacaoResponse> recentes = movimentacaoRepository
                .findTop8ByOrderByDataHoraDesc()
                .stream()
                .map(MovimentacaoResponse::de)
                .toList();

        ResumoPastilhas resumo = pastilhaRepository.buscarResumo();

        return new IndicadoresResponse(
                resumo.ativas(),
                resumo.categorias(),
                resumo.abaixoDoMinimo(),
                resumo.valorEmEstoque(),
                movimentacaoRepository.contarDesde(LocalDateTime.now().minusDays(30)),
                alertas,
                recentes);
    }

    // Lista so os itens em falta, usado pela tela de reposicao
    @Transactional(readOnly = true)
    public List<PastilhaResponse> alertasReposicao(int limite) {
        return pastilhaRepository
                .listarAbaixoDoMinimo(PageRequest.of(0, limite))
                .stream()
                .map(PastilhaResponse::de)
                .toList();
    }
}
