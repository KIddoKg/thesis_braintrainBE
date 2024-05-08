package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.AuthenticationRequest;
import com.braintrain.backend.dto.AuthenticationResponse;
import com.braintrain.backend.entity.Gender;
import com.braintrain.backend.service.AuthService;
import com.braintrain.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private AuthService authService;
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Api<String>> signup(@RequestBody AuthenticationRequest signupRequest) {
        return authService.signup(signupRequest);
    }

    @GetMapping("/verify-account/{otp}")
    public ResponseEntity<Api<Void>> verifyAccount(@PathVariable String otp) {
        return authService.verifyAccount(otp);
    }

    @PutMapping("/add-information/{phone}")
    public ResponseEntity<Api<Void>> addInformation(@PathVariable String phone,
                                               @RequestParam String fullName,
                                               @RequestParam LocalDate dob,
                                               @RequestParam Gender gender) {
        return userService.addInformation(phone, fullName, dob, gender);
    }

    @PostMapping("/login")
    public ResponseEntity<Api<AuthenticationResponse>> login(@RequestBody AuthenticationRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Api<AuthenticationResponse>> refreshToken(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/resend-otp/{phone}")
    public ResponseEntity<Api<Void>> resendOtp(@PathVariable String phone) {
        return authService.resendOtp(phone);
    }

    @GetMapping("/reset-password")
    public ResponseEntity<Api<Void>> resetPassword(@RequestParam String phone,
                                              @RequestParam String password) {
        return authService.resetPassword(phone, password);
    }
}
