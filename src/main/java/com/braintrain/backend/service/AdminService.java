package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.config.JwtService;
import com.braintrain.backend.dto.AuthenticationRequest;
import com.braintrain.backend.dto.AuthenticationResponse;
import com.braintrain.backend.dto.UserDto;
import com.braintrain.backend.entity.Token;
import com.braintrain.backend.entity.User;
import com.braintrain.backend.entity.UserRole;
import com.braintrain.backend.repository.TokenRepository;
import com.braintrain.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class AdminService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    private final ApiService apiService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ResponseEntity<Api<Void>> signup(AuthenticationRequest signupRequest) {
        try {
            User user = User.builder()
                    .phone(signupRequest.getPhone())
                    .password(passwordEncoder.encode(signupRequest.getPassword()))
                    .userRole(UserRole.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(apiService.generateSuccessApi(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateFailApi(
                            "internal_server_error",
                            "Error: " + e.getMessage()
                    ));
        }
    }

    public ResponseEntity<Api<AuthenticationResponse>> login(AuthenticationRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByPhone(loginRequest.getPhone());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "admin_not_found",
                            "Admin " + loginRequest.getPhone() + " chưa được đăng ký",
                            null));
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPassword()
                )
        );

        User user = userOptional.get();
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .build();

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // revoke and expire all the existing jwt tokens
        revokeAllUserJwtToken(user);
        saveUserJwtToken(user, jwtToken);
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .userDto(userDto)
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(authResponse));
    }

    private void revokeAllUserJwtToken(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    private void saveUserJwtToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }
}
