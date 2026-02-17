package com.nitin.saas.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String token, long expirySeconds) {
        long expiryTime = Instant.now().getEpochSecond() + expirySeconds;
        blacklistedTokens.put(token, expiryTime);
        log.debug("Token blacklisted, total blacklisted: {}", blacklistedTokens.size());
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long expiryTime = blacklistedTokens.get(token);
        if (expiryTime == null) {
            return false;
        }

        if (Instant.now().getEpochSecond() > expiryTime) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }

    @Override
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        long now = Instant.now().getEpochSecond();
        int removedCount = 0;

        var iterator = blacklistedTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() < now) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired blacklisted tokens, remaining: {}",
                    removedCount, blacklistedTokens.size());
        }
    }
}