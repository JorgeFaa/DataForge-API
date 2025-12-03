package com.dataforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // Import EnableCaching

@SpringBootApplication
@EnableCaching // Enable Spring's caching capabilities
public class DataforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataforgeApplication.class, args);
    }

}
