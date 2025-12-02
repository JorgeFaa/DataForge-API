package com.dataforge.service;

import com.dataforge.exception.ResourceNotFoundException;
import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class DatabaseInstanceService {

    private final DatabaseInstanceRepository repository;
    private final DockerService dockerService;
    private final EncryptionUtil encryptionUtil;

    @Autowired
    public DatabaseInstanceService(DatabaseInstanceRepository repository, DockerService dockerService, EncryptionUtil encryptionUtil) {
        this.repository = repository;
        this.dockerService = dockerService;
        this.encryptionUtil = encryptionUtil;
    }

    public List<DatabaseInstance> getAllInstances() {
        return repository.findAll();
    }

    @Transactional
    public void deleteInstance(Long id) { // Changed return type to void
        DatabaseInstance instance = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + id)); // Throw exception

        dockerService.removeContainer(instance.getContainerId());
        repository.deleteById(id);
    }

    public boolean testConnection(Long dbId) {
        DatabaseInstance instance = repository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String user = instance.getDbUser();
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        try (Connection conn = DriverManager.getConnection(url, user, decryptedPassword)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Failed to test connection for DB ID " + dbId + ": " + e.getMessage());
            return false;
        }
    }
}
