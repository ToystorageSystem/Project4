package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.RegisterRequest;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Users register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequest("Email already exists");
        }

        Users user = new Users();

        user.setUserCode(request.getUserCode());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setIdentityCard(request.getIdentityCard());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }
}