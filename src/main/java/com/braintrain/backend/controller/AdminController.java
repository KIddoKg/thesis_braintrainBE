package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.*;
import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.entity.GameType;
import com.braintrain.backend.entity.User;
import com.braintrain.backend.service.AdminService;
import com.braintrain.backend.service.PlayingTurnService;
import com.braintrain.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminController {
    private final UserService userService;
    private final AdminService adminService;
    private final PlayingTurnService playingTurnService;

    @PostMapping("/signup")
    public ResponseEntity<Api<Void>> signup(@RequestBody AuthenticationRequest signupRequest) {
        return adminService.signup(signupRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<Api<AuthenticationResponse>> login(@RequestBody AuthenticationRequest loginRequest) {
        return adminService.login(loginRequest);
    }

    @GetMapping("/all-users")
    public ResponseEntity<Api<List<UserDto>>> getAllUsers(@RequestParam int pageNumber,
                                                          @RequestParam int pageSize) {
        return userService.getAllUsers(pageNumber, pageSize);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Api<User>> getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId);
    }

    @GetMapping("/user-phone/{phone}")
    public ResponseEntity<Api<User>> getUserByPhone(@PathVariable String phone) {
        return userService.getUserByPhone(phone);
    }

    @PutMapping("/update-user")
    public ResponseEntity<Api<User>> updateUser(@RequestBody User updatedUser) {
        return userService.adminUpdateUser(updatedUser);
    }

    @GetMapping("/ghost-users")
    public ResponseEntity<Api<List<GhostUserDto>>> getGhostUsers() {
        return userService.getGhostUsers();
    }

    @GetMapping("/monitor-user")
    public ResponseEntity<Api<List<UserDto>>> getAllMonitoredUsers(@RequestParam int pageNumber,
                                                                   @RequestParam int pageSize) {
        return userService.getAllMonitoredUsers(pageNumber, pageSize);
    }

    @PutMapping("/monitor-user")
    public ResponseEntity<Api<Void>> setUserMonitored(@RequestParam UUID userId,
                                                      @RequestParam boolean setMonitored) {
        return userService.setUserMonitored(userId, setMonitored);
    }

    @GetMapping("/notify-token/{phone}")
    public ResponseEntity<Api<HashMap<String, String>>> getNotifyToken(@PathVariable String phone) {
        return userService.getNotifyToken(phone);
    }

    @GetMapping("/chart/by-game-type")
    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDates(
            @RequestParam UUID userId,
            @RequestParam GameType gameType,
            @RequestParam long fromDate,
            @RequestParam long toDate) {
        return playingTurnService.adminGetPlayingTurnsBetweenDates(userId, gameType, fromDate, toDate);
    }

    @GetMapping("/chart/by-game-name")
    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDatesForEachGame(
            @RequestParam UUID userId,
            @RequestParam GameName gameName,
            @RequestParam int level,
            @RequestParam long fromDate,
            @RequestParam long toDate) {
        return playingTurnService.adminGetPlayingTurnsBetweenDatesForEachGame(
                userId,
                gameName,
                level,
                fromDate,
                toDate
        );
    }

    @GetMapping("/delete-user")
    public ResponseEntity<Api<List<UserDto>>> getLockedUser(@RequestParam int pageNumber,
                                                            @RequestParam int pageSize) {
        return userService.getLockedUsers(pageNumber, pageSize);
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<Api<Void>> deleteUser(@RequestBody List<User> users) {
        return userService.delete(users);
    }
}
