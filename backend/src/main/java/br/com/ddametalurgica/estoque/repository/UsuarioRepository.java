package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Consultas de usuario. O CRUD basico vem do JpaRepository.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Login (UsuarioDetailsService) e validacao de e-mail unico
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Pesquisa da gestao de usuarios. Filtro vazio ('') desativa a busca por texto.
    @Query("SELECT u FROM Usuario u "
            + "WHERE (:busca = '' "
            + "       OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :busca, '%')) "
            + "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busca, '%'))) "
            + "  AND u.ativo = :ativo")
    Page<Usuario> buscar(@Param("busca") String busca, @Param("ativo") boolean ativo, Pageable pageable);

    // Impede inativar o ultimo administrador ativo
    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);
}
