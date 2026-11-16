package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import br.com.ddametalurgica.estoque.dto.AlterarSenhaRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioCreateRequest;
import br.com.ddametalurgica.estoque.dto.UsuarioResponse;
import br.com.ddametalurgica.estoque.dto.UsuarioUpdateRequest;
import br.com.ddametalurgica.estoque.exception.RecursoNaoEncontradoException;
import br.com.ddametalurgica.estoque.exception.RegraNegocioException;
import br.com.ddametalurgica.estoque.repository.UsuarioRepository;
import br.com.ddametalurgica.estoque.security.UsuarioDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Regras da gestao de usuarios
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String busca, boolean ativo, Pageable pageable) {
        return repository.buscar(normalizar(busca), ativo, pageable).map(UsuarioResponse::de);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.de(obrigatorio(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest dados) {
        String email = dados.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um usuário com o e-mail " + email + ".");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(dados.perfil());
        usuario.definirSenha(encoder.encode(dados.senha()));
        return UsuarioResponse.de(repository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest dados) {
        Usuario usuario = obrigatorio(id);
        usuario.setNome(dados.nome().trim());
        usuario.setPerfil(dados.perfil());
        return UsuarioResponse.de(repository.save(usuario));
    }

    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest dados) {
        Usuario usuario = obrigatorio(id);
        usuario.definirSenha(encoder.encode(dados.novaSenha()));
        repository.save(usuario);
    }

    // Exclusao logica, para preservar as movimentacoes do usuario.
    // Idempotente como o DELETE do HTTP: inativar de novo nao gera erro.
    @Transactional
    public void inativar(Long id) {
        Usuario usuario = obrigatorio(id);
        if (!usuario.isAtivo()) {
            return;
        }
        if (id.equals(usuarioAutenticadoId())) {
            throw new RegraNegocioException("Você não pode inativar o próprio usuário.");
        }
        if (usuario.getPerfil() == PerfilUsuario.ADMIN && repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN) <= 1) {
            throw new RegraNegocioException("Não é possível inativar o último administrador ativo.");
        }
        usuario.inativar();
        repository.save(usuario);
    }

    @Transactional
    public UsuarioResponse reativar(Long id) {
        Usuario usuario = obrigatorio(id);
        usuario.reativar();
        return UsuarioResponse.de(repository.save(usuario));
    }

    // Busca por id ou lanca RecursoNaoEncontradoException (404)
    private Usuario obrigatorio(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    // Id do usuario autenticado, ou null fora de uma requisicao autenticada
    private Long usuarioAutenticadoId() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioDetails detalhes)) {
            return null;
        }
        return detalhes.getUsuario().getId();
    }

    private String normalizar(String filtro) {
        return filtro == null ? "" : filtro.trim();
    }
}
