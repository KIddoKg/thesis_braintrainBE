package com.braintrain.backend.controller;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.ObjectiveDto;
import com.braintrain.backend.service.ObjectiveService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/objective")
@AllArgsConstructor
public class ObjectiveController {
    private final ObjectiveService objectiveService;

    @GetMapping
    public ResponseEntity<Api<List<ObjectiveDto>>> getAllObjectivesOfUser() {
        return objectiveService.getAllObjectivesOfUser();
    }
}
