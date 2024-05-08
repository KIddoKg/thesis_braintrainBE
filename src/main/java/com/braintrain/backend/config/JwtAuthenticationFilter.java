package com.braintrain.backend.config;

import com.braintrain.backend.repository.TokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phone;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            phone = jwtService.extractUsername(jwt);

            // if user is not authenticated, check user against database
            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(phone);

                // allow the process only when the token does not expire and not revoke
                boolean isTokenValid = tokenRepository.findByToken(jwt)
                        .map(token -> !token.isExpired() && !token.isRevoked())
                        .orElse(false);

                // check whether the token valid or not
                // if valid, update the SecurityContextHolder
                if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                    // update the SecurityContextHolder
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // extend the authentication token with the details of the request
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // update
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // for the next filters to be executed
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            setExpiredTokenResponse(response);
        }
    }

    private void setExpiredTokenResponse(HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseBody = new HashMap<>();

            // metadata
            Map<String, Boolean> metadata = new HashMap<>();
            metadata.put("success", false);
            responseBody.put("metadata", metadata);

            // error
            Map<String, String> errorDetails = new HashMap<>();
            if (!HttpStatus.UNAUTHORIZED.is2xxSuccessful()) {
                errorDetails.put("code", "token_expired");
                errorDetails.put("message", "Token đã hết hiệu lực");
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
