package com.managio.modules.payroll.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** The salary owed to a team member for one month. What has been paid is the sum of its salary expenses. */
@Entity
@Table(name = "payroll_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private long teamMemberId;
    private LocalDate period;
    private BigDecimal amount;
    private String notes;
    private Instant createdAt;

    @Version
    private long version;

    public void adjust(BigDecimal amount, String notes) {
        this.amount = amount;
        this.notes = notes;
    }
}
