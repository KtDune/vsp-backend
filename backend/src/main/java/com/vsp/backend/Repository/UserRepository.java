package com.vsp.backend.Repository;

import com.vsp.backend.Model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndIsActiveTrue(Long id);

    List<User> findByUsernameIgnoreCaseAndIsActiveTrue(String username);

    Optional<User> findFirstByUsernameIgnoreCaseAndIsActiveTrue(String username);

    Optional<User> findByEmailIgnoreCaseAndIsActiveTrue(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
