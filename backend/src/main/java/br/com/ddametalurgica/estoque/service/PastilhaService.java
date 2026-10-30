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

// Classe com as regras do cadastro de pastilhas.
// O controller nao acessa o repositorio direto, passa sempre por aqui.
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
            throw new RegraNegocioException("Ja existe uma pastilha com o codigo " + dados.codigo() + ".");
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
            throw new RegraNegocioException("Ja existe uma pastilha com o codigo " + novoCodigo + ".");
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

    // Inativa a pastilha em vez de apagar, porque existem
    // movimentacoes no banco que apontam para ela
    @Transactional
    public void inativar(Long id) {
        Pastilha pastilha = obrigatoria(id);
        if (!pastilha.isAtivo()) {
            throw new RegraNegocioException("A pastilha " + pastilha.getCodigo() + " ja esta inativa.");
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

    // Copia os dados que vieram da tela para o objeto da pastilha
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

    // Busca a pastilha e ja dispara o erro 404 se nao existir
    private Pastilha obrigatoria(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pastilha " + id + " nao encontrada."));
    }

    // Troca null por string vazia, que e o que a consulta espera
    private String normalizar(String filtro) {
        return filtro == null ? "" : filtro.trim();
    }
}
