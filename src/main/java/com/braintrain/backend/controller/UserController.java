package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.UserDto;
import com.braintrain.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Api<UserDto>> getUser() {
        return userService.getUser();
    }

    @PutMapping("/update-information")
    public ResponseEntity<Api<UserDto>> updateUser(@RequestBody UserDto userDto) {
        return userService.updateUser(userDto);
    }

    @PostMapping("/notify-token")
    public ResponseEntity<Api<Void>> saveNotifyToken(@RequestParam String notifyToken) {
        return userService.saveNotifyToken(notifyToken);
    }

    @PutMapping("/lock")
    public ResponseEntity<Api<Void>> lock() {
        return userService.lock();
    }
}
