package com.managio.platform.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Plain-SQL read models don't see entity changes Hibernate hasn't written yet. Call {@link #flush()} after
 * changing entities and before building a response with {@code JdbcClient}, so the response shows the change.
 */
@Component
public class Persistence {

    @PersistenceContext
    private EntityManager entityManager;

    public void flush() {
        entityManager.flush();
    }
}
