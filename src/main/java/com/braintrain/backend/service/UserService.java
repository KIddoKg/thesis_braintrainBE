package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.GhostUserDto;
import com.braintrain.backend.dto.UserDto;
import com.braintrain.backend.entity.*;
import com.braintrain.backend.mapper.UserMapper;
import com.braintrain.backend.repository.PlayingTurnRepository;
import com.braintrain.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@AllArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PlayingTurnRepository playingTurnRepository;

    private final AuthService authService;
    private final UserMapper userMapper;
    private final ApiService apiService;
    private final PasswordEncoder passwordEncoder;
    private final DailyWorkoutService dailyWorkoutService;
    private final PlayingTurnService playingTurnService;
    private final ObjectiveService objectiveService;

    public ResponseEntity<Api<UserDto>> getUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(userMapper.mapToDto(user)));
    }

    public ResponseEntity<Api<Void>> addInformation(String phone, String fullName, LocalDate dob, Gender gender) {
        try {
            // change phone number format from 0********* to +84*********
            if (phone.startsWith("0")) {
                phone = "+84" + phone.substring(1);
            }

            Optional<User> userOptional = userRepository.findByPhone(phone);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(apiService.generateFailApi(
                                "phone_not_found",
                                "Số điện thoại " + phone + " chưa được đăng ký"));
            }
            User user = userOptional.get();
            user.setFullName(fullName);
            user.setDob(dob);
            user.setGender(gender);
            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(apiService.generateSuccessApi(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateFailApi(
                            "internal_server_error",
                            "Error: " + e.getMessage()));
        }
    }

    public ResponseEntity<Api<UserDto>> updateUser(UserDto userDto) {
        try {
            User user = authService.getCurrentUser();

            if (userDto.getFullName() != null && !userDto.getFullName().isEmpty()) {
                user.setFullName(userDto.getFullName());
            }

            if (userDto.getPhone() != null && !userDto.getPhone().isEmpty()) {
                user.setPhone(userDto.getPhone().startsWith("0")
                        ? "+84" + userDto.getPhone().substring(1)
                        : userDto.getPhone());
            }

            if (userDto.getDob() != null) {
                user.setDob(userDto.getDob());
            }

            user.setLoginCode(userDto.getLoginCode());

            if (userDto.getGender() != null) {
                user.setGender(userDto.getGender());
            }

            if (userDto.getProfileUrl() != null && !userDto.getProfileUrl().isEmpty()) {
                user.setProfileUrl(userDto.getProfileUrl());
            }

            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(apiService.generateSuccessApi(userMapper.mapToDto(user)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateApi(
                            "internal_server_error",
                            "Error: " + e.getMessage(),
                            null));
        }
    }

    public ResponseEntity<Api<List<UserDto>>> getAllUsers(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "pageNumber_starts_at_1",
                            "pageNumber bắt đầu từ trang 1",
                            null
                    ));
        } else {
            --pageNumber;
        }
        Page<User> allUsers = userRepository.findByUserRole(UserRole.USER, PageRequest.of(pageNumber, pageSize));

        if (allUsers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "no_user_in_database",
                            "Không có user nào trong cơ sở dữ liệu",
                            null
                    ));
        }
        List<UserDto> returnUsers = allUsers.stream()
                .filter(user -> user.getDob() != null)
                .map(userMapper::mapToDto)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generatePaginationApi(
                        returnUsers,
                        allUsers.getTotalElements(),
                        allUsers.getNumber() + 1,
                        allUsers.getSize())
                );
    }

    public ResponseEntity<Api<User>> getUserById(UUID id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(value -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(apiService.generatePaginationApi(value, 1, 1, 1)))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(apiService.generateApi(
                                "user_not_found",
                                "User có id " + id + " không tồn tại trong cơ sở dữ liệu",
                                null
                        )));
    }

    public ResponseEntity<Api<User>> getUserByPhone(String phone) {
        // change phone number format from 0********* to +84*********
        if (phone.startsWith("0")) {
            phone = "+84" + phone.substring(1);
        }
        Optional<User> user = userRepository.findByPhone(phone);
        return user.map(value -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(apiService.generatePaginationApi(value, 1, 1, 1)))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(apiService.generateApi(
                                "user_not_found",
                                "User không tồn tại trong cơ sở dữ liệu",
                                null
                        )));
    }

    public ResponseEntity<Api<User>> adminUpdateUser(User updatedUser) {
        try {
            Optional<User> userOptional = userRepository.findById(updatedUser.getId());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(apiService.generateApi(
                                "user_not_found",
                                "User có id " + updatedUser.getId() + " không tồn tại trong cơ sở dữ liệu",
                                null
                        ));
            }
            User user = userOptional.get();

            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                user.setPhone(user.getPhone().startsWith("0")
                        ? "+84" + user.getPhone().substring(1)
                        : user.getPhone());
            }
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                user.setFullName(user.getFullName());
            }
            if (user.getDob() != null) {
                user.setDob(user.getDob());
            }
            user.setLoginCode(updatedUser.getLoginCode());
            if (user.getGender() != null) {
                user.setGender(user.getGender());
            }
            user.setNotifyToken(updatedUser.getNotifyToken());

            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(apiService.generateSuccessApi(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateApi(
                            "internal_server_error",
                            "Error: " + e.getMessage(),
                            null));
        }
    }

    public ResponseEntity<Api<List<GhostUserDto>>> getGhostUsers() {
        List<Object[]> resultList = playingTurnRepository.findTheLatestPlayingTurnOfAllUsers();
        List<GhostUserDto> ghostUserList = new ArrayList<>();

        for (Object[] tuple : resultList) {
            byte[] userIdBytes = (byte[]) tuple[0];
            UUID userId = bytesToUUID(userIdBytes);

            String fullName = (String) tuple[1];
            String phone = (String) tuple[2];

            Timestamp timestamp = (Timestamp) tuple[3];
            LocalDateTime latestDateTime = timestamp.toLocalDateTime();
            long latestDate = latestDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            int noPlayDays = (int) Duration.between(latestDateTime, LocalDateTime.now()).toDays();

            if (latestDateTime.isBefore(LocalDateTime.now().minusDays(2))) {
                ghostUserList.add(new GhostUserDto(userId, fullName, phone, latestDate, noPlayDays));
            }
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(ghostUserList));
    }

    public ResponseEntity<Api<List<UserDto>>> getAllMonitoredUsers(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "pageNumber_starts_at_1",
                            "pageNumber bắt đầu từ trang 1",
                            null
                    ));
        } else {
            --pageNumber;
        }
        Page<User> userList = userRepository.findByIsMonitored(true, PageRequest.of(pageNumber, pageSize));

        List<UserDto> returnUsers = userList
                .stream()
                .filter(user -> user.getDob() != null)
                .map(userMapper::mapToDto)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generatePaginationApi(
                        returnUsers,
                        userList.getTotalElements(),
                        userList.getNumber() + 1,
                        userList.getSize())
                );
    }

    public ResponseEntity<Api<Void>> setUserMonitored(UUID userId, boolean setMonitored) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateFailApi(
                            "user_not_found",
                            "User với id " + userId + " không tồn tại trong cơ sở dữ liệu"
                    ));
        }
        User user = userOptional.get();
        user.setMonitored(setMonitored);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    public ResponseEntity<Api<HashMap<String, String>>> getNotifyToken(String phone) {
        Optional<User> userOptional = userRepository.findByPhone(fixPhoneFormat(phone));
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "user_not_found",
                            "User có số điện thoại " + phone + " không tồn tại trong cơ sở dữ liệu",
                            null
                    ));
        }
        User user = userOptional.get();
        HashMap<String, String> userNotifyToken = new HashMap<>();
        userNotifyToken.put("notifyToken", user.getNotifyToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(userNotifyToken));
    }

    public ResponseEntity<Api<Void>> saveNotifyToken(String notifyToken) {
        try {
            User user = authService.getCurrentUser();
            user.setNotifyToken(notifyToken);
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

    public ResponseEntity<Api<Void>> lock() {
        User user = authService.getCurrentUser();
        user.setLocked(true);
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    public ResponseEntity<Api<List<UserDto>>> getLockedUsers(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "pageNumber_starts_at_1",
                            "pageNumber bắt đầu từ trang 1",
                            null
                    ));
        } else {
            --pageNumber;
        }
        Page<User> lockedUsers = userRepository.findByIsLocked(PageRequest.of(pageNumber, pageSize));

        if (lockedUsers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "no_locked_user_in_database",
                            "Không có locked user nào trong cơ sở dữ liệu",
                            null
                    ));
        }
        List<UserDto> returnUsers = lockedUsers.stream()
                .map(userMapper::mapToDto)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generatePaginationApi(
                        returnUsers,
                        lockedUsers.getTotalElements(),
                        lockedUsers.getNumber() + 1,
                        lockedUsers.getSize())
                );
    }

    public ResponseEntity<Api<Void>> delete(List<User> users) {
        // delete all records that references the user being deleted
        for (User user : users) {
            dailyWorkoutService.delete(user);
            objectiveService.delete(user);
            playingTurnService.delete(user);
            authService.deleteOtpAndTokenByUser(user);
        }

        userRepository.deleteAll(users);
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(null));
    }

    private String fixPhoneFormat(String phone) {
        if (phone.startsWith("0")) {
            phone = "+84" + phone.substring(1);
        }
        return phone;
    }

    private static UUID bytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSignificantBits = byteBuffer.getLong();
        long leastSignificantBits = byteBuffer.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
