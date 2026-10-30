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

// Classe com as regras da gestao de usuarios.
// O controller nao acessa o repositorio direto, passa sempre por aqui.
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
            throw new RegraNegocioException("Ja existe um usuario com o email " + email + ".");
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

    // Inativa o usuario em vez de apagar, porque existem movimentacoes
    // no banco que apontam para ele
    @Transactional
    public void inativar(Long id) {
        Usuario usuario = obrigatorio(id);
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("O usuario " + usuario.getEmail() + " ja esta inativo.");
        }
        if (id.equals(usuarioAutenticadoId())) {
            throw new RegraNegocioException("Voce nao pode inativar o proprio usuario.");
        }
        if (usuario.getPerfil() == PerfilUsuario.ADMIN && repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN) <= 1) {
            throw new RegraNegocioException("Nao e possivel inativar o ultimo administrador ativo.");
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

    // Busca o usuario e ja dispara o erro 404 se nao existir
    private Usuario obrigatorio(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario " + id + " nao encontrado."));
    }

    // Id de quem esta logado, ou null se ninguem estiver
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
