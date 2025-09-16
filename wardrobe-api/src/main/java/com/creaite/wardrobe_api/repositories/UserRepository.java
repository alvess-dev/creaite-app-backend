package com.creaite.wardrobe_api.repositories;
import com.creaite.wardrobe_api.domain.user.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
