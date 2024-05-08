package com.braintrain.backend.mapper;

import com.braintrain.backend.dto.PlayingTurnDto;
import com.braintrain.backend.entity.PlayingTurn;
import com.braintrain.backend.service.AuthService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public abstract class PlayingTurnMapper {
    protected AuthService authService;

    @Autowired
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Mapping(target = "noOfLevels", expression = "java(playingTurnDto.getGameName().getNoOfLevels())")
    @Mapping(target = "createdDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "user", expression = "java(authService.getCurrentUser())")
    public abstract PlayingTurn map(PlayingTurnDto playingTurnDto);

    @Mapping(target = "createdDate", expression = "java(convertToEpochMillis(playingTurn.getCreatedDate()))")
    public abstract PlayingTurnDto mapToDto(PlayingTurn playingTurn);

    long convertToEpochMillis(LocalDateTime createdDate) {
        return createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
