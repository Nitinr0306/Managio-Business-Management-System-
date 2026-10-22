package com.managio.modules.expenses.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A per-business expense category. Seeded categories carry a system key; SALARIES is used by payroll. */
@Entity
@Table(name = "expense_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCategory {

    public static final String SALARIES = "SALARIES";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private String name;
    private String systemKey;
    private boolean archived;
    private Instant createdAt;

    public static ExpenseCategory of(long businessId, String name, String systemKey, Instant now) {
        ExpenseCategory category = new ExpenseCategory();
        category.businessId = businessId;
        category.name = name;
        category.systemKey = systemKey;
        category.createdAt = now;
        return category;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
