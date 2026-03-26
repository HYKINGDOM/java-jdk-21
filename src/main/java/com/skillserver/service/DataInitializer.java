package com.skillserver.service;

import com.skillserver.domain.entity.UserEntity;
import com.skillserver.domain.enums.SystemRole;
import com.skillserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createUserIfAbsent("admin", "admin@skill.local", "admin123", SystemRole.ADMIN);
        createUserIfAbsent("member", "member@skill.local", "member123", SystemRole.MEMBER);
    }

    private void createUserIfAbsent(String username, String email, String rawPassword, SystemRole role) {
        userRepository.findByUsername(username).orElseGet(() -> userRepository.save(UserEntity.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .role(role)
            .active(true)
            .build()));
    }
}
