package br.com.ddametalurgica.estoque.security;

import br.com.ddametalurgica.estoque.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Usado pelo Spring Security para buscar o usuario pelo email na hora do login
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmailIgnoreCase(email)
                .map(UsuarioDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }
}
