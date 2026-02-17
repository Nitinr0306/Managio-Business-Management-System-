package com.nitin.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class ManagioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagioApplication.class, args);
    }

}
