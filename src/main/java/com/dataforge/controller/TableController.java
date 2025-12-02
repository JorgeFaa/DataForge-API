package com.dataforge.controller;

import com.dataforge.dto.ColumnDefinition;
import com.dataforge.dto.CreateTableRequest;
import com.dataforge.dto.ModifyColumnRequest;
import com.dataforge.dto.TableRelationship;
import com.dataforge.dto.TableSchemaResponse;
import com.dataforge.service.TableManagerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/databases/{dbId}/tables")
public class TableController {

    @Autowired
    private TableManagerService tableManagerService;

    @PostMapping
    public ResponseEntity<String> createTable(@PathVariable Long dbId, @Valid @RequestBody CreateTableRequest request) {
        tableManagerService.createTable(dbId, request);
        return ResponseEntity.ok().body("Table '" + request.tableName() + "' created successfully.");
    }

    @GetMapping
    public ResponseEntity<List<String>> listTables(@PathVariable Long dbId) {
        List<String> tables = tableManagerService.listTables(dbId);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<TableSchemaResponse> getTableSchema(@PathVariable Long dbId, @PathVariable String tableName) {
        TableSchemaResponse schema = tableManagerService.getTableSchema(dbId, tableName);
        return ResponseEntity.ok(schema);
    }

    @DeleteMapping("/{tableName}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long dbId, @PathVariable String tableName) {
        tableManagerService.deleteTable(dbId, tableName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tableName}/columns")
    public ResponseEntity<String> addColumn(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @Valid @RequestBody ColumnDefinition columnDefinition) {
        
        tableManagerService.addColumn(dbId, tableName, columnDefinition);
        return ResponseEntity.status(201).body("Column '" + columnDefinition.name() + "' added to table '" + tableName + "' successfully.");
    }

    @PutMapping("/{tableName}/columns/{oldColumnName}")
    public ResponseEntity<String> modifyColumn(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @PathVariable String oldColumnName,
            @Valid @RequestBody ModifyColumnRequest request) {
        
        tableManagerService.modifyColumn(dbId, tableName, oldColumnName, request);
        return ResponseEntity.ok().body("Column '" + oldColumnName + "' in table '" + tableName + "' modified successfully.");
    }

    @DeleteMapping("/{tableName}/columns/{columnName}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @PathVariable String columnName) {
        tableManagerService.deleteColumn(dbId, tableName, columnName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tableName}/foreign-keys/{constraintName}")
    public ResponseEntity<Void> dropForeignKey(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @PathVariable String constraintName) {
        tableManagerService.dropForeignKey(dbId, tableName, constraintName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tableName}/foreign-keys")
    public ResponseEntity<List<TableRelationship>> getTableForeignKeys(
            @PathVariable Long dbId,
            @PathVariable String tableName) {
        List<TableRelationship> relationships = tableManagerService.getTableRelationships(dbId, tableName);
        return ResponseEntity.ok(relationships);
    }
}
