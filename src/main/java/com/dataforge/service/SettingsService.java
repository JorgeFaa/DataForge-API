package com.dataforge.service;

import com.dataforge.model.AppSetting;
import com.dataforge.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SettingsService {

    private static final String MASTER_PASSWORD_KEY = "MASTER_PASSWORD";

    @Autowired
    private AppSettingRepository settingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean isMasterPasswordSet() {
        return settingsRepository.findById(MASTER_PASSWORD_KEY).isPresent();
    }

    public void setMasterPassword(String password) {
        if (isMasterPasswordSet()) {
            throw new IllegalStateException("Master password has already been set.");
        }
        String hashedPassword = passwordEncoder.encode(password);
        settingsRepository.save(new AppSetting(MASTER_PASSWORD_KEY, hashedPassword));
    }

    public boolean verifyMasterPassword(String rawPassword) {
        Optional<AppSetting> setting = settingsRepository.findById(MASTER_PASSWORD_KEY);
        if (setting.isEmpty()) {
            return false; // No password set, so verification fails
        }
        return passwordEncoder.matches(rawPassword, setting.get().getSettingValue());
    }
}
