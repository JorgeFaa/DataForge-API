package com.dataforge.controller;

import com.dataforge.dto.SqlQueryRequest;
import com.dataforge.dto.SqlQueryResponse;
import com.dataforge.service.QueryService;
import com.dataforge.service.SettingsService; // Import SettingsService
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/databases/{dbId}/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @Autowired
    private SettingsService settingsService; // Inject SettingsService

    @PostMapping
    public ResponseEntity<SqlQueryResponse> executeQuery(
            @PathVariable Long dbId,
            @Valid @RequestBody SqlQueryRequest request) {
        
        // Verify the master password before executing the query
        if (!settingsService.verifyMasterPassword(request.masterPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(SqlQueryResponse.forError("Invalid master password."));
        }

        SqlQueryResponse response = queryService.executeQuery(dbId, request.sql());
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
