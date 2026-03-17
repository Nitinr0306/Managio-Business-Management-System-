package com.nitin.saas.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed token blacklist.
 *
 * Activated when {@code app.security.blacklist.provider=redis} is set in properties.
 * Falls back to {@link InMemoryTokenBlacklistService} when Redis is not configured.
 *
 * Why Redis?  In a multi-pod deployment an in-memory blacklist only exists on the
 * pod that served the logout request.  A load balancer may route the next request
 * to a different pod that has never seen the blacklist entry → the token appears valid.
 *
 * Redis stores the raw token string as a key with a TTL equal to the token's
 * remaining lifetime.  Value is "1" (arbitrary — we only care about key existence).
 *
 * Prerequisites (add to pom.xml):
 *   <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-data-redis</artifactId>
 *   </dependency>
 *
 * application.properties:
 *   spring.data.redis.host=${REDIS_HOST:localhost}
 *   spring.data.redis.port=${REDIS_PORT:6379}
 *   spring.data.redis.password=${REDIS_PASSWORD:}
 *   app.security.blacklist.provider=redis
 */
@Service
@ConditionalOnProperty(name = "app.security.blacklist.provider", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    @Override
    public void blacklist(String token, long expirySeconds) {
        try {
            String key = PREFIX + token;
            redis.opsForValue().set(key, "1", Duration.ofSeconds(expirySeconds));
            log.debug("Token blacklisted in Redis (TTL {}s)", expirySeconds);
        } catch (Exception ex) {
            // Non-fatal: if Redis is temporarily unavailable we log and continue.
            // The token will remain valid until natural expiry (max 15 min for access tokens).
            log.error("Failed to blacklist token in Redis: {}", ex.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redis.hasKey(PREFIX + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.error("Failed to check Redis blacklist: {}. Treating token as NOT blacklisted.", ex.getMessage());
            return false;  // fail-open: prefer availability over strict invalidation
        }
    }

    /**
     * No-op: Redis TTL handles expiry automatically.
     * Method exists to satisfy the TokenBlacklistService contract.
     */
    @Override
    public void cleanup() {
        // Redis TTL cleans up entries automatically — nothing to do.
        log.debug("RedisTokenBlacklistService.cleanup() called — no-op, Redis handles TTL");
    }
}