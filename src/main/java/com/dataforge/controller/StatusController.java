package com.dataforge.controller;

import com.dataforge.service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @Autowired
    private DockerService dockerService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("dockerConnected", dockerService.isDockerConnected());
        
        if (!dockerService.isDockerConnected()) {
            status.put("message", "Failed to connect to Docker. Please ensure Docker Desktop is installed and running.");
        } else {
            status.put("message", "API is running and connected to Docker.");
        }
        
        return ResponseEntity.ok(status);
    }
}
