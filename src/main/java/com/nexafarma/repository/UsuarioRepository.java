package com.nexafarma.repository;

import com.nexafarma.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    /**
     * Carga usuario con empleado y rol en la misma consulta (evita LazyInitializationException
     * fuera de sesión, p. ej. en CurrentUserAdvice / navbar).
     */
    @Query("""
            SELECT u FROM Usuario u
            JOIN FETCH u.empleado
            JOIN FETCH u.rol
            WHERE u.username = :username
            """)
    Optional<Usuario> findByUsernameWithEmpleadoYRol(@Param("username") String username);

    boolean existsByUsername(String username);

    Optional<Usuario> findByEmpleadoId(Long empleadoId);
}
