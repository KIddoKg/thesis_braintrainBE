package com.braintrain.backend.mapper;

import com.braintrain.backend.dto.UserDto;
import com.braintrain.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Mapping(target = "isMonitored", source = "monitored")
    @Mapping(target = "isLocked", source = "locked")
    @Mapping(target = "age", expression = "java(calculateAge(user.getDob()))")
    public abstract UserDto mapToDto(User user);

    int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }
}
