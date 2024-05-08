package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.config.JwtService;
import com.braintrain.backend.dto.AuthenticationRequest;
import com.braintrain.backend.dto.AuthenticationResponse;
import com.braintrain.backend.entity.Otp;
import com.braintrain.backend.entity.Token;
import com.braintrain.backend.entity.User;
import com.braintrain.backend.entity.UserRole;
import com.braintrain.backend.mapper.UserMapper;
import com.braintrain.backend.repository.OtpRepository;
import com.braintrain.backend.repository.TokenRepository;
import com.braintrain.backend.repository.UserRepository;
import com.braintrain.backend.validator.PhoneValidator;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final TokenRepository tokenRepository;

    private final PhoneValidator phoneValidator;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ApiService apiService;
    private final ObjectiveService objectiveService;

    public ResponseEntity<Api<String>> signup(AuthenticationRequest signupRequest) {
        // change phone number format from 0********* to +84*********
        signupRequest.setPhone(fixPhoneFormat(signupRequest.getPhone()));

        // check whether the phone number is valid
        boolean validPhone = phoneValidator.test(signupRequest.getPhone());
        if (!validPhone) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "invalid_phone",
                            "Số điện thoại không hợp lệ",
                            null));
        }

        // check whether the phone number is already registered and validated
        String otpCode;
        Optional<User> userExists = userRepository.findByPhone(signupRequest.getPhone());
        if (userExists.isPresent() && userExists.get().getEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "phone_already_used",
                            "Số điện thoại " + signupRequest.getPhone() + " đã được đăng ký và kích hoạt",
                            null));
        } else if (userExists.isPresent()) {
            User userNotEnabled = userExists.get();

//            sendOtpToUser(userNotEnabled);
            otpCode = generateOtp(userNotEnabled);
        } else {
            User user = User.builder()
                    .phone(signupRequest.getPhone())
                    .password(passwordEncoder.encode(signupRequest.getPassword()))
                    .userRole(UserRole.USER)
                    .enabled(false)
                    .build();
            userRepository.save(user);

//            sendOtpToUser(user);
            otpCode = generateOtp(user);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(otpCode));
    }

    public ResponseEntity<Api<Void>> verifyAccount(String otpCode) {
        Optional<Otp> otpOptional = otpRepository.findByOtp(otpCode);
        if (otpOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi("invalid_otp", "Mã otp không đúng"));
        }
        Otp otp = otpOptional.get();

        // check whether the otp is confirmed, i.e. account is activated
        if (otp.getConfirmedAt() != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi(
                            "account_already_activated",
                            "Tài khoản đã được kích hoạt"));
        }
        // check whether the otp expired
        LocalDateTime expiresAt = otp.getExpiresAt();
        if (expiresAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi("otp_expired", "Mã otp đã hết hiệu lực"));
        }

        String phone = otp.getUser().getPhone();
        Optional<User> userOptional = userRepository.findByPhone(phone);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi(
                            "phone_not_found",
                            "Số điện thoại " + phone + " chưa được đăng ký"));
        }
        User user = userOptional.get();
        user.setEnabled(true);
        userRepository.save(user);

        otp.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otp);

        // create all objectives for new user
        objectiveService.createAllObjectives(user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    public ResponseEntity<Api<AuthenticationResponse>> login(AuthenticationRequest loginRequest) {
        // change phone number format from 0********* to +84*********
        loginRequest.setPhone(fixPhoneFormat(loginRequest.getPhone()));

        Optional<User> userOptional = userRepository.findByPhone(loginRequest.getPhone());
        // if phone not found, return inform api
        // if password is incorrect, return 403
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "phone_not_found",
                            "Số điện thoại " + loginRequest.getPhone() + " chưa được đăng ký",
                            null));
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPassword()
                )
        );

        User user = userOptional.get();

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // revoke and expire all the existing jwt tokens
        revokeAllUserJwtToken(user);
        saveUserJwtToken(user, jwtToken);
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .userDto(userMapper.mapToDto(user))
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(authResponse));
    }

    public ResponseEntity<Api<AuthenticationResponse>> refreshToken(String refreshToken) {
        String userPhone = jwtService.extractUsername(refreshToken);

        if (userPhone != null) {
            var user = this.userRepository.findByPhone(userPhone)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                // create and save a new access token
                var accessToken = jwtService.generateToken(user);
                revokeAllUserJwtToken(user);
                saveUserJwtToken(user, accessToken);

                var authResponse = AuthenticationResponse.builder()
                        .userDto(userMapper.mapToDto(user))
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                return ResponseEntity.status(HttpStatus.OK)
                        .body(apiService.generateSuccessApi(authResponse));
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiService.generateApi(
                        "internal_server_error",
                        "Lỗi server",
                        null));
    }

    public ResponseEntity<Api<Void>> resendOtp(String phone) {
        // change phone number format from 0********* to +84*********
        phone = fixPhoneFormat(phone);

        Optional<User> userOptional = userRepository.findByPhone(phone);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi(
                            "phone_not_found",
                            "Số điện thoại " + phone + " chưa được đăng ký"));
        }
        User user = userOptional.get();

        Optional<Otp> oldOtp = otpRepository.findUnconfirmedOtpByUser(user.getId());
        // disable all previous OTP
        if (oldOtp.isPresent()) {
            oldOtp.stream().toList().forEach(otp -> {
                otp.setConfirmedAt(LocalDateTime.now());
                otpRepository.save(otp);
            });
        }
//        sendOtpToUser(user);
        generateOtp(user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    public ResponseEntity<Api<Void>> resetPassword(String phone, String password) {
        // change phone number format from 0********* to +84*********
        phone = fixPhoneFormat(phone);

        Optional<User> userOptional = userRepository.findByPhone(phone);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi(
                            "phone_not_found",
                            "Số điện thoại " + phone + " chưa được đăng ký"));
        }
        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    public User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String phone = userDetails.getUsername();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("No user found"));
    }

    void deleteOtpAndTokenByUser(User user) {
        otpRepository.deleteAllByUser(user);
        tokenRepository.deleteAllByUser(user);
    }

    private void sendOtpToUser(User user) {
        String otp = generateOtp(user);
        String body = "Xin chào, mã xác thực của bạn là " + otp +
                ". Hãy sử dụng mã để xác thực số điện thoại được sử dụng cho ứng dụng BrainTrain. " +
                "Mã sẽ hết hiệu lực trong vòng 5 phút nữa. Xin cảm ơn.";
        otpService.sendOtp(user.getPhone(), body);
    }

    private String generateOtp(User user) {
        // check whether the otp exists with another phone number
        String otpCode;
        do {
            otpCode = new DecimalFormat("0000")
                    .format(new Random().nextInt(9999));
        } while (otpRepository.existsByOtp(otpCode));

        Otp otp = new Otp(
                otpCode,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5),
                user
        );
        otpRepository.save(otp);
        return otpCode;
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

    private String fixPhoneFormat(String phone) {
        if (phone.startsWith("0")) {
            phone = "+84" + phone.substring(1);
        }
        return phone;
    }
}
