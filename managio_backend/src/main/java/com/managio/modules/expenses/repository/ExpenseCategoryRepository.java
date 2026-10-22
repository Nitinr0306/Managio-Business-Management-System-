package com.managio.modules.expenses.repository;

import com.managio.modules.expenses.domain.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByBusinessIdOrderByNameAsc(long businessId);

    Optional<ExpenseCategory> findByIdAndBusinessId(long id, long businessId);

    Optional<ExpenseCategory> findByBusinessIdAndSystemKey(long businessId, String systemKey);
}
