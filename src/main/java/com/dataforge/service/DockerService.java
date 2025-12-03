package com.dataforge.service;

import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
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
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DockerService {

    private final DatabaseInstanceRepository repository;
    private final EncryptionUtil encryptionUtil;
    
    private DockerClient dockerClient;
    private boolean isDockerConnected = false;

    @Autowired
    public DockerService(DatabaseInstanceRepository repository, EncryptionUtil encryptionUtil) {
        this.repository = repository;
        this.encryptionUtil = encryptionUtil;
    }

    @PostConstruct
    public void init() {
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
            this.dockerClient.pingCmd().exec();
            this.isDockerConnected = true;
            System.out.println("Successfully connected to Docker daemon.");
        } catch (Exception e) {
            this.isDockerConnected = false;
            System.err.println("!!! FAILED TO CONNECT TO DOCKER DAEMON !!!");
            System.err.println("Please ensure Docker is installed and running.");
            System.err.println("Error: " + e.getMessage());
        }
    }

    public boolean isDockerConnected() {
        return this.isDockerConnected;
    }

    public DatabaseInstance createPostgresContainer(String dbName, String user, String password) {
        if (!isDockerConnected) {
            throw new IllegalStateException("Cannot create container: Not connected to Docker daemon.");
        }

        System.out.println("Ensuring postgres:17 image exists locally...");
        try {
            dockerClient.pullImageCmd("postgres").withTag("17").start().awaitCompletion();
            System.out.println("postgres:17 image is ready.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed while waiting for postgres:17 image to pull", e);
        }

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
        instance.setDbPassword(encryptionUtil.encrypt(password));
        // For a desktop app, the API runs on the host, so localhost is the correct address
        // to connect to the port exposed by the Docker container on the host.
        instance.setHost("localhost"); 
        instance.setPort(hostPort);

        return repository.save(instance);
    }

    public void removeContainer(String containerId) {
        if (!isDockerConnected) {
            throw new IllegalStateException("Cannot remove container: Not connected to Docker daemon.");
        }
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
