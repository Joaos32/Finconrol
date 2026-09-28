package com.fincontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinControlApplication.class, args);
    }
}
