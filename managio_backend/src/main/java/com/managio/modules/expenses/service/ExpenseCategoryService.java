package com.managio.modules.expenses.service;

import com.managio.modules.expenses.domain.ExpenseCategory;
import com.managio.modules.expenses.repository.ExpenseCategoryRepository;
import com.managio.modules.business.domain.BusinessType.CategoryTemplate;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categories;
    private final Clock clock;

    public record CategoryResponse(long id, String name, @Nullable String systemKey, boolean archived) {
        static CategoryResponse from(ExpenseCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSystemKey(), c.isArchived());
        }
    }

    /** BUS-2: every new business starts with its type's categories. */
    @Transactional
    public void seedDefaults(long businessId, List<CategoryTemplate> templates) {
        templates.forEach(t -> categories.save(ExpenseCategory.of(businessId, t.name(), t.systemKey(), clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(long businessId) {
        return categories.findByBusinessIdOrderByNameAsc(businessId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(BusinessContext ctx, String name) {
        return CategoryResponse.from(categories.save(ExpenseCategory.of(ctx.businessId(), name.trim(), null, clock.instant())));
    }

    @Transactional
    public CategoryResponse update(BusinessContext ctx, long categoryId, String name, boolean archived) {
        ExpenseCategory category = get(ctx.businessId(), categoryId);
        if (archived && ExpenseCategory.SALARIES.equals(category.getSystemKey())) {
            throw ApiException.rule("category.required", "Payroll uses the Salaries category, so it can't be archived.");
        }
        category.rename(name.trim());
        category.setArchived(archived);
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public ExpenseCategory get(long businessId, long categoryId) {
        return categories.findByIdAndBusinessId(categoryId, businessId)
                .orElseThrow(() -> ApiException.notFound("Expense category"));
    }

    @Transactional(readOnly = true)
    public ExpenseCategory salaries(long businessId) {
        return categories.findByBusinessIdAndSystemKey(businessId, ExpenseCategory.SALARIES)
                .orElseThrow(() -> ApiException.notFound("Salaries category"));
    }
}
