package com.example.devices;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Devices API", version = "1.0.0",
        description = "Device management REST API"))
@SpringBootApplication
public class DevicesApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevicesApiApplication.class, args);
    }
}
