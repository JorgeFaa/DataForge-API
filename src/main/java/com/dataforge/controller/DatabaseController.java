package com.dataforge.controller;

import com.dataforge.dto.CreateDatabaseRequest;
import com.dataforge.dto.CreateDatabaseResponse;
import com.dataforge.dto.DatabaseInstanceInfo;
import com.dataforge.model.DatabaseInstance;
import com.dataforge.service.DatabaseInstanceService;
import com.dataforge.service.DockerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/databases")
public class DatabaseController {

    @Autowired
    private DockerService dockerService;

    @Autowired
    private DatabaseInstanceService instanceService;

    @PostMapping
    public ResponseEntity<CreateDatabaseResponse> createDatabase(@Valid @RequestBody CreateDatabaseRequest request) {
        DatabaseInstance instance = dockerService.createPostgresContainer(
            request.dbName(),
            request.user(),
            request.password()
        );

        CreateDatabaseResponse response = new CreateDatabaseResponse(
            instance.getContainerId(),
            instance.getDbName(),
            instance.getDbUser(),
            instance.getHost(),
            instance.getPort()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DatabaseInstanceInfo>> getAllDatabases() {
        List<DatabaseInstanceInfo> instances = instanceService.getAllInstances().stream()
                .map(instance -> new DatabaseInstanceInfo(
                        instance.getId(),
                        instance.getDbName(),
                        instance.getDbUser(),
                        instance.getHost(),
                        instance.getPort()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(instances);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatabase(@PathVariable Long id) {
        instanceService.deleteInstance(id); // Service now throws ResourceNotFoundException
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{dbId}/test-connection")
    public ResponseEntity<String> testDatabaseConnection(@PathVariable Long dbId) {
        boolean isConnected = instanceService.testConnection(dbId);
        if (isConnected) {
            return ResponseEntity.ok("Connection to database ID " + dbId + " successful.");
        } else {
            // This will be caught by GlobalExceptionHandler if ResourceNotFoundException is thrown
            // Otherwise, it's a connection failure, which is a SERVICE_UNAVAILABLE
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Failed to connect to database ID " + dbId + ". It might be down or connection details are incorrect.");
        }
    }
}
