package com.managio.modules.business.service;

import com.managio.modules.business.domain.Business;
import com.managio.modules.business.repository.BusinessRepository;
import com.managio.modules.team.domain.Role;
import com.managio.modules.team.repository.RoleRepository;
import com.managio.modules.business.dto.BusinessDtos.BusinessResponse;
import com.managio.modules.business.dto.BusinessDtos.CreateBusinessRequest;
import com.managio.modules.business.domain.BusinessType.RoleTemplate;
import com.managio.modules.expenses.service.ExpenseCategoryService;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.web.Normalize;
import com.managio.modules.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;

/** Creates a business with everything its type needs to be usable straight away (BUS-1, BUS-2). */
@Service
@RequiredArgsConstructor
public class BusinessOnboarding {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final BusinessRepository businesses;
    private final RoleRepository roles;
    private final TeamService team;
    private final ExpenseCategoryService expenseCategories;
    private final ActivityLog activity;
    private final Clock clock;

    @Transactional
    public BusinessResponse create(long userId, CreateBusinessRequest request) {
        Business business = Business.create(newCode(), request.name().trim(), request.type(), userId);
        business.updateProfile(business.getName(), business.getType(), Normalize.phone(request.phone()), null, null,
                Normalize.text(request.city()), null, null, "India", null);
        business = businesses.save(business);

        Role owner = null;
        for (RoleTemplate template : request.type().roles()) {
            Role role = roles.save(Role.of(business.getId(), template.name(), template.description(),
                    template.permissions(), template.systemKey()));
            if (role.isOwnerRole()) {
                owner = role;
            }
        }
        team.addOwner(business.getId(), userId, owner.getId(), LocalDate.now(clock.withZone(business.zone())));
        expenseCategories.seedDefaults(business.getId(), request.type().expenseCategories());
        activity.recordUser(business.getId(), userId, null, "BUSINESS_CREATED", "BUSINESS", business.getId(),
                "Created " + business.getName());
        return BusinessResponse.from(business);
    }

    /** Four digits and four letters, e.g. 4821KRWD; letters skip I and O to avoid confusion with 1 and 0. */
    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(String.format("%04d", RANDOM.nextInt(10_000)));
            for (int i = 0; i < 4; i++) {
                sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
            }
            code = sb.toString();
        } while (businesses.existsByCode(code));
        return code;
    }
}
