package com.nitin.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ManagioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagioApplication.class, args);
    }
}