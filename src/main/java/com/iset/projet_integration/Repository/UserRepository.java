package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
    Optional<User> findByIdentifiant(String identifiant);

    // Dans UserRepository.java
    List<User> findAllByEmail(String email);
}