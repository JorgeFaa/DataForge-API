package com.dataforge.controller;

import com.dataforge.dto.CreateDbUserRequest;
import com.dataforge.dto.DbUserInfo;
import com.dataforge.dto.ManagePermissionsRequest;
import com.dataforge.dto.UserPermissionInfo;
import com.dataforge.service.DbUserManagerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/databases/{dbId}/db-users")
public class DbUserController {

    @Autowired
    private DbUserManagerService dbUserManagerService;

    @PostMapping
    public ResponseEntity<String> createDbUser(
            @PathVariable Long dbId,
            @Valid @RequestBody CreateDbUserRequest request) {
        
        dbUserManagerService.createDbUser(dbId, request.username(), request.password());
        return ResponseEntity.status(201).body("Database user '" + request.username() + "' created successfully in database ID " + dbId);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteDbUser(
            @PathVariable Long dbId,
            @PathVariable String username) {
        
        dbUserManagerService.deleteDbUser(dbId, username);
        return ResponseEntity.ok().body("Database user '" + username + "' deleted successfully from database ID " + dbId);
    }

    @PostMapping("/{username}/permissions")
    public ResponseEntity<String> grantPermissions(
            @PathVariable Long dbId,
            @PathVariable String username,
            @Valid @RequestBody ManagePermissionsRequest request) {
        
        dbUserManagerService.grantPermissions(dbId, username, request);
        return ResponseEntity.ok().body("Permissions " + request.privileges() + " granted to user '" + username + "' on table '" + request.tableName() + "' in database ID " + dbId);
    }

    @DeleteMapping("/{username}/permissions")
    public ResponseEntity<String> revokePermissions(
            @PathVariable Long dbId,
            @PathVariable String username,
            @Valid @RequestBody ManagePermissionsRequest request) {
        
        dbUserManagerService.revokePermissions(dbId, username, request);
        return ResponseEntity.ok().body("Permissions " + request.privileges() + " revoked from user '" + username + "' on table '" + request.tableName() + "' in database ID " + dbId);
    }

    @GetMapping
    public ResponseEntity<List<DbUserInfo>> listDbUsers(
            @PathVariable Long dbId) {
        List<DbUserInfo> users = dbUserManagerService.listDbUsers(dbId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}/permissions")
    public ResponseEntity<List<UserPermissionInfo>> listUserPermissions(
            @PathVariable Long dbId,
            @PathVariable String username) {
        List<UserPermissionInfo> permissions = dbUserManagerService.listUserPermissions(dbId, username);
        return ResponseEntity.ok(permissions);
    }
}
