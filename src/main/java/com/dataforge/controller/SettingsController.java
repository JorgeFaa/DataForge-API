package com.dataforge.controller;

import com.dataforge.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @GetMapping("/master-password/is-set")
    public ResponseEntity<Map<String, Boolean>> isMasterPasswordSet() {
        return ResponseEntity.ok(Map.of("isSet", settingsService.isMasterPasswordSet()));
    }

    @PostMapping("/master-password")
    public ResponseEntity<String> setMasterPassword(@RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password cannot be empty.");
        }

        if (settingsService.isMasterPasswordSet()) {
            return ResponseEntity.status(409).body("Master password has already been set."); // 409 Conflict
        }

        settingsService.setMasterPassword(password);
        return ResponseEntity.status(201).body("Master password has been set successfully.");
    }
}
