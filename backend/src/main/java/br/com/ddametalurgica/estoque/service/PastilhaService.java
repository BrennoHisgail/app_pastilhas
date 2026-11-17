package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.dto.PastilhaRequest;
import br.com.ddametalurgica.estoque.dto.PastilhaResponse;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Regras do cadastro de pastilhas
@Service
public class PastilhaService {

    private final PastilhaRepository repository;

    public PastilhaService(PastilhaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<PastilhaResponse> listar(String busca,
                                         String categoria,
                                         boolean ativo,
                                         boolean somenteAbaixoDoMinimo,
                                         Pageable pageable) {
        return repository
                .buscar(normalizar(busca), normalizar(categoria), ativo, somenteAbaixoDoMinimo, pageable)
                .map(PastilhaResponse::de);
    }

    @Transactional(readOnly = true)
    public PastilhaResponse buscarPorId(Long id) {
        return PastilhaResponse.de(obrigatoria(id));
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return repository.listarCategorias();
    }

    @Transactional
    public PastilhaResponse criar(PastilhaRequest dados) {
        if (repository.existsByCodigoIgnoreCase(dados.codigo().trim())) {
            throw new RegraNegocioException("Já existe uma pastilha com o código " + dados.codigo() + ".");
        }
        Pastilha pastilha = new Pastilha();
        aplicar(dados, pastilha);
        return PastilhaResponse.de(repository.save(pastilha));
    }

    @Transactional
    public PastilhaResponse atualizar(Long id, PastilhaRequest dados) {
        Pastilha pastilha = obrigatoria(id);
        String novoCodigo = dados.codigo().trim();

        boolean codigoMudou = !pastilha.getCodigo().equalsIgnoreCase(novoCodigo);
        if (codigoMudou && repository.existsByCodigoIgnoreCase(novoCodigo)) {
            throw new RegraNegocioException("Já existe uma pastilha com o código " + novoCodigo + ".");
        }

        aplicar(dados, pastilha);
        return PastilhaResponse.de(repository.save(pastilha));
    }

    @Transactional
    public PastilhaResponse alterarNivelMinimo(Long id, BigDecimal nivelMinimo) {
        Pastilha pastilha = obrigatoria(id);
        pastilha.setNivelMinimo(nivelMinimo);
        return PastilhaResponse.de(repository.save(pastilha));
    }

    // Exclusao logica, para preservar as movimentacoes da pastilha.
    // Idempotente como o DELETE do HTTP: inativar de novo nao gera erro.
    @Transactional
    public void inativar(Long id) {
        Pastilha pastilha = obrigatoria(id);
        if (!pastilha.isAtivo()) {
            return;
        }
        pastilha.inativar();
        repository.save(pastilha);
    }

    @Transactional
    public PastilhaResponse reativar(Long id) {
        Pastilha pastilha = obrigatoria(id);
        pastilha.reativar();
        return PastilhaResponse.de(repository.save(pastilha));
    }

    // Copia os dados do request para a entidade
    private void aplicar(PastilhaRequest dados, Pastilha pastilha) {
        pastilha.setCodigo(dados.codigo().trim());
        pastilha.setDescricao(dados.descricao().trim());
        pastilha.setCategoria(dados.categoria().trim());
        pastilha.setUnidadeMedida(dados.unidadeMedida());
        pastilha.setNivelMinimo(dados.nivelMinimo());
        pastilha.setCustoUnitario(dados.custoUnitario());
        pastilha.setLeadTimeDias(dados.leadTimeDias());
        pastilha.setEstoqueSeguranca(dados.estoqueSeguranca());
    }

    // Busca por id ou lanca RecursoNaoEncontradoException (404)
    private Pastilha obrigatoria(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pastilha " + id + " não encontrada."));
    }

    // A consulta usa '' como "sem filtro"
    private String normalizar(String filtro) {
        return filtro == null ? "" : filtro.trim();
    }
}
