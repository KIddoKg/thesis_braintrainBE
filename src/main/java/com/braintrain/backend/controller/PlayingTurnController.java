package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.PlayingTurnDto;
import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.entity.GameType;
import com.braintrain.backend.service.PlayingTurnService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playing-turn")
@AllArgsConstructor
public class PlayingTurnController {
    private final PlayingTurnService playingTurnService;

    @PostMapping
    public ResponseEntity<Api<Void>> savePlayingTurn(@RequestBody PlayingTurnDto playingTurnDto) {
        return playingTurnService.save(playingTurnDto);
    }

    @GetMapping("/by-game-type")
    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDates(
            @RequestParam GameType gameType,
            @RequestParam long fromDate,
            @RequestParam long toDate) {
        return playingTurnService.getPlayingTurnsBetweenDates(gameType, fromDate, toDate);
    }

    @GetMapping("/by-game-name")
    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDatesForEachGame(
            @RequestParam GameName gameName,
            @RequestParam int level,
            @RequestParam long fromDate,
            @RequestParam long toDate) {
        return playingTurnService.getPlayingTurnsBetweenDatesForEachGame(gameName, level, fromDate, toDate);
    }

    @GetMapping("/best-game/{gameName}")
    public ResponseEntity<Api<PlayingTurnDto>> getBestTurnByEachGame(@PathVariable GameName gameName) {
        return playingTurnService.getBestTurnByEachGame(gameName);
    }
}
