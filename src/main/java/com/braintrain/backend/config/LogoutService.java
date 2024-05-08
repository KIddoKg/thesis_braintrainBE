package com.braintrain.backend.config;

import com.braintrain.backend.entity.Token;
import com.braintrain.backend.repository.TokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {
    private final TokenRepository tokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        // extract jwt from the request
        jwt = authHeader.substring(7);
        Token storedToken = tokenRepository.findByToken(jwt).orElse(null);

        if (storedToken != null) {
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            tokenRepository.save(storedToken);
            setLogoutResponse(response, HttpStatus.OK, "", "");
        } else {
            setLogoutResponse(
                    response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "internal_server_error",
                    "Token không đúng hoặc đã xảy ra lỗi server trong quá trình đăng xuất");
        }
    }

    private void setLogoutResponse(HttpServletResponse response, HttpStatus status, String errorCode, String errorMessage) {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = new HashMap<>();

            // metadata
            Map<String, Boolean> metadata = new HashMap<>();
            metadata.put("success", status.is2xxSuccessful());
            responseBody.put("metadata", metadata);

            // error
            Map<String, String> errorDetails = new HashMap<>();
            if (!status.is2xxSuccessful()) {
                errorDetails.put("code", errorCode);
                errorDetails.put("message", errorMessage);
                responseBody.put("error", errorDetails);
            } else {
                responseBody.put("error", null);
            }

            // data
            responseBody.put("data", null);

            writer.write(objectMapper.writeValueAsString(responseBody));
        } catch (IOException e) {
            // do nothing
        }
    }
}
