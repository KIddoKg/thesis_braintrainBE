package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.RankingDto;
import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.service.RankingService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@AllArgsConstructor
public class RankingController {
    private final RankingService rankingService;

    @GetMapping("/weekly/{gameName}")
    public ResponseEntity<Api<RankingDto>> getWeeklyRankingByGameName(@PathVariable GameName gameName) {
        return rankingService.getRankingByGameName(gameName, 'w');
    }

    @GetMapping("/monthly/{gameName}")
    public ResponseEntity<Api<RankingDto>> getMonthlyRankingByGameName(@PathVariable GameName gameName) {
        return rankingService.getRankingByGameName(gameName, 'm');
    }

    @GetMapping("/all/{gameName}")
    public ResponseEntity<Api<RankingDto>> getAllRankingByGameName(@PathVariable GameName gameName) {
        return rankingService.getAllRankingByGameName(gameName);
    }
}
