package com.managio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ManagioApplication {

    public static void main(String[] args) {
        // Times are stored in UTC and business dates use each business's own zone, so the host zone must not matter.
        // The PostgreSQL driver sends the JVM zone on connect, and some hosts' legacy names (Asia/Calcutta) are refused.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ManagioApplication.class, args);
    }
}
