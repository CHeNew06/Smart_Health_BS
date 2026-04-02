package com.example.smart_health_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartHealthManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHealthManagementApplication.class, args);
    }

}
