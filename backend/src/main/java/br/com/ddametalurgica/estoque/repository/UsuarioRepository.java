package br.com.ddametalurgica.estoque.repository;

import br.com.ddametalurgica.estoque.domain.PerfilUsuario;
import br.com.ddametalurgica.estoque.domain.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorio do usuario. O JpaRepository ja traz save, findById,
// findAll e delete prontos, entao aqui so escrevemos as consultas nossas.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Usado pelo login (UsuarioDetailsService) e para nao deixar
    // cadastrar dois usuarios com o mesmo email
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Consulta da tela de gestao de usuarios. Quando o filtro vem vazio
    // ele nao filtra nada, por isso a comparacao com '' no comeco.
    @Query("SELECT u FROM Usuario u "
            + "WHERE (:busca = '' "
            + "       OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :busca, '%')) "
            + "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busca, '%'))) "
            + "  AND u.ativo = :ativo")
    Page<Usuario> buscar(@Param("busca") String busca, @Param("ativo") boolean ativo, Pageable pageable);

    // Usado para barrar a inativacao do ultimo administrador ativo
    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);
}
