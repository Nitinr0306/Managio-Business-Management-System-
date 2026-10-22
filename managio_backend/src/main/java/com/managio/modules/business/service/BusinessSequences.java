package com.managio.modules.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gap-free, per-business numbers for customers, invoices and receipts (D-014). The sequence row stays locked
 * until the caller's transaction ends, and a rolled-back transaction gives its number back.
 */
@Component
@RequiredArgsConstructor
public class BusinessSequences {

    public enum Sequence { CUSTOMER, INVOICE, RECEIPT }

    private final JdbcClient jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public int next(long businessId, Sequence sequence) {
        return jdbc.sql("""
                        INSERT INTO business_sequences (business_id, name, next_value) VALUES (:businessId, :name, 2)
                        ON CONFLICT (business_id, name)
                            DO UPDATE SET next_value = business_sequences.next_value + 1
                        RETURNING next_value - 1
                        """)
                .param("businessId", businessId)
                .param("name", sequence.name())
                .query(Integer.class)
                .single();
    }
}
