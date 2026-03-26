package com.skillserver.security;

import com.skillserver.common.exception.NotFoundException;
import com.skillserver.domain.entity.UserEntity;
import com.skillserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return AppUserPrincipal.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .role(user.getRole().name())
            .active(user.isActive())
            .build();
    }
}
