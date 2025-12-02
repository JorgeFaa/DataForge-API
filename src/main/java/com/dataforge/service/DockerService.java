package com.dataforge.service;

import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil; // Import the EncryptionUtil
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final DatabaseInstanceRepository repository;
    private final EncryptionUtil encryptionUtil; // Inject EncryptionUtil

    @Autowired
    public DockerService(DatabaseInstanceRepository repository, EncryptionUtil encryptionUtil) {
        this.repository = repository;
        this.encryptionUtil = encryptionUtil; // Assign it
        
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public DatabaseInstance createPostgresContainer(String dbName, String user, String password) {
        ExposedPort exposedPort = ExposedPort.tcp(5432);
        PortBinding portBinding = new PortBinding(Ports.Binding.empty(), exposedPort);
        HostConfig hostConfig = new HostConfig().withPortBindings(portBinding);

        CreateContainerResponse container = dockerClient.createContainerCmd("postgres:13")
                .withEnv("POSTGRES_DB=" + dbName, "POSTGRES_USER=" + user, "POSTGRES_PASSWORD=" + password)
                .withExposedPorts(exposedPort)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();

        Ports.Binding[] bindings = dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getPorts().getBindings().get(exposedPort);
        String hostPortStr = bindings[0].getHostPortSpec();
        int hostPort = Integer.parseInt(hostPortStr);

        DatabaseInstance instance = new DatabaseInstance();
        instance.setContainerId(containerId);
        instance.setDbName(dbName);
        instance.setDbUser(user);
        instance.setDbPassword(encryptionUtil.encrypt(password)); // Encrypt the password before saving
        instance.setHost("localhost");
        instance.setPort(hostPort);

        return repository.save(instance);
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            System.out.println("Container " + containerId + " not found, maybe already stopped or removed.");
        }

        try {
            dockerClient.removeContainerCmd(containerId).exec();
            System.out.println("Container " + containerId + " removed successfully.");
        } catch (NotFoundException e) {
            System.out.println("Container " + containerId + " not found, maybe already removed.");
        }
    }
}
