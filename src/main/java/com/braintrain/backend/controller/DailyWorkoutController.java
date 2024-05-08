package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.DailyWorkoutDto;
import com.braintrain.backend.service.DailyWorkoutService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-workout")
@AllArgsConstructor
public class DailyWorkoutController {
    private final DailyWorkoutService dailyWorkoutService;

    @GetMapping
    public ResponseEntity<Api<DailyWorkoutDto>> getTodayTrack() {
        return dailyWorkoutService.getTodayTrack();
    }

    @PostMapping
    public ResponseEntity<Api<DailyWorkoutDto>> updateTodayTrack(@RequestParam double sleepHrs,
                                                                 @RequestParam int mood) {
        return dailyWorkoutService.updateTodayTrack(sleepHrs, mood);
    }
}
