package com.dataforge.controller;

import com.dataforge.service.DynamicCrudService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/db/{dbId}/tables/{tableName}")
public class DynamicCrudController {

    @Autowired
    private DynamicCrudService crudService;

    @PostMapping
    public ResponseEntity<String> createRecord(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @RequestBody Map<String, Object> record) {
        
        if (record == null || record.isEmpty()) {
            return ResponseEntity.badRequest().body("Record data cannot be empty.");
        }

        crudService.createRecord(dbId, tableName, record);
        return ResponseEntity.status(201).body("Record created successfully.");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> readRecords(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDirection,
            @RequestParam Map<String, String> allParams // To capture all query parameters
    ) {
        allParams.remove("page");
        allParams.remove("limit");
        allParams.remove("orderBy");
        allParams.remove("orderDirection");

        List<Map<String, Object>> records = crudService.readRecords(dbId, tableName, page, limit, allParams, orderBy, orderDirection);
        return ResponseEntity.ok(records);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateRecord(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @PathVariable Object id, // The ID of the record to update
            @RequestBody Map<String, Object> updates) {
        
        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.badRequest().body("Update data cannot be empty.");
        }

        int affectedRows = crudService.updateRecord(dbId, tableName, id, updates);
        if (affectedRows > 0) {
            return ResponseEntity.ok().body("Record updated successfully. Affected rows: " + affectedRows);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record with ID " + id + " not found in table " + tableName);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord( // Changed from ResponseEntity<Void> to ResponseEntity<?>
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @PathVariable Object id) {
        int affectedRows = crudService.deleteRecord(dbId, tableName, id);
        if (affectedRows > 0) {
            return ResponseEntity.noContent().build(); // Returns ResponseEntity<Void>
        } else {
            // Now this is valid because of the wildcard '?'
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record with ID " + id + " not found in table " + tableName); // Returns ResponseEntity<String>
        }
    }
}
