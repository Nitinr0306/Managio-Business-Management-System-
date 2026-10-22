package com.managio.modules.expenses.repository;

import com.managio.modules.expenses.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndBusinessId(long id, long businessId);
}
