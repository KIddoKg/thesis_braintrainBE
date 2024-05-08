package com.braintrain.backend.dto;

import com.braintrain.backend.entity.Gender;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String fullName;
    private String phone;
    private LocalDate dob;
    private Integer age;
    private String loginCode;
    private Gender gender;
    private boolean isMonitored;
    private boolean isLocked;
    private String profileUrl;
}
