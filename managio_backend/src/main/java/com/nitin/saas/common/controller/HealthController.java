package com.nitin.saas.common.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.name:Managio}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .application(appName)
                .version(appVersion)
                .profile(activeProfile)
                .build());
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", appName);
        info.put("version", appVersion);
        info.put("profile", activeProfile);
        info.put("timestamp", LocalDateTime.now());

        Map<String, Object> build = new HashMap<>();
        build.put("time", LocalDateTime.now());
        build.put("javaVersion", System.getProperty("java.version"));
        info.put("build", build);

        return ResponseEntity.ok(info);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthResponse {
        private String status;
        private LocalDateTime timestamp;
        private String application;
        private String version;
        private String profile;
    }
}