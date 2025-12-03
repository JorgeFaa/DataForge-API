package com.dataforge.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AppSetting {

    @Id
    private String settingKey;
    private String settingValue;

    public AppSetting() {}

    public AppSetting(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }

    // Getters and Setters
    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
