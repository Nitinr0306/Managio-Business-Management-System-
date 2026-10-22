package com.managio.modules.business.domain;

import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.managio.platform.tenancy.Permission.*;
/**
 * Business types and the templates they seed (D-005, docs/PRODUCT.md#business-types): vocabulary, default
 * optional modules, default roles and default expense categories. All types share one engine.
 */
public enum BusinessType {

    FITNESS("Gym or fitness studio", new Labels("Member", "Members", "Batch", "Batches", "Membership plan", "Membership plans"),
            EnumSet.of(Module.MEMBERSHIPS, Module.ATTENDANCE, Module.PAYROLL),
            List.of(RoleTemplate.frontDesk("Front desk"), RoleTemplate.coach("Trainer", "TRAINER", "Sees members and marks attendance.")),
            List.of("Equipment and maintenance", "Supplies")),

    SALON("Salon or spa", new Labels("Client", "Clients", "Group", "Groups", "Package", "Packages"),
            EnumSet.of(Module.CATALOG, Module.INVENTORY, Module.MEMBERSHIPS, Module.PAYROLL),
            List.of(RoleTemplate.frontDesk("Receptionist"),
                    new RoleTemplate("Stylist", "STYLIST", "Sees clients and their history.", EnumSet.of(CUSTOMERS_VIEW))),
            List.of("Products and supplies", "Equipment")),

    COACHING("Coaching centre", new Labels("Student", "Students", "Batch", "Batches", "Course", "Courses"),
            EnumSet.of(Module.MEMBERSHIPS, Module.GROUPS, Module.ATTENDANCE, Module.PAYROLL),
            List.of(RoleTemplate.frontDesk("Front office"), RoleTemplate.coach("Teacher", "TEACHER", "Sees students and marks attendance.")),
            List.of("Study material", "Equipment and maintenance")),

    SCHOOL("School or academy", new Labels("Student", "Students", "Class", "Classes", "Fee plan", "Fee plans"),
            EnumSet.of(Module.MEMBERSHIPS, Module.GROUPS, Module.ATTENDANCE, Module.PAYROLL),
            List.of(RoleTemplate.frontDesk("Front office"), RoleTemplate.coach("Teacher", "TEACHER", "Sees students and marks attendance.")),
            List.of("Study material", "Equipment and maintenance")),

    CLINIC("Clinic", new Labels("Patient", "Patients", "Group", "Groups", "Package", "Packages"),
            EnumSet.of(Module.CATALOG, Module.MEMBERSHIPS, Module.PAYROLL),
            List.of(RoleTemplate.frontDesk("Receptionist"),
                    new RoleTemplate("Practitioner", "PRACTITIONER", "Sees patients and their history.", EnumSet.of(CUSTOMERS_VIEW))),
            List.of("Medical supplies", "Equipment")),

    RETAIL("Shop", new Labels("Customer", "Customers", "Group", "Groups", "Plan", "Plans"),
            EnumSet.of(Module.CATALOG, Module.INVENTORY, Module.PAYROLL),
            List.of(new RoleTemplate("Cashier", "CASHIER", "Makes sales, takes payments and closes the day.",
                            EnumSet.of(CUSTOMERS_VIEW, CUSTOMERS_MANAGE, BILLING_VIEW, BILLING_COLLECT, DAYBOOK_CLOSE)),
                    new RoleTemplate("Stock keeper", "STOCK_KEEPER", "Keeps products and stock up to date.",
                            EnumSet.of(OFFERINGS_MANAGE))),
            List.of("Stock purchases", "Packaging")),

    OTHER("Other business", new Labels("Customer", "Customers", "Group", "Groups", "Plan", "Plans"),
            EnumSet.of(Module.MEMBERSHIPS, Module.CATALOG, Module.PAYROLL),
            List.of(new RoleTemplate("Staff", "STAFF", "Looks after customers and takes payments.",
                    EnumSet.of(CUSTOMERS_VIEW, CUSTOMERS_MANAGE, BILLING_VIEW, BILLING_COLLECT))),
            List.of("Supplies"));

    /** Owner and Manager exist in every business; Accountant too. Type-specific roles sit between them. */
    static final RoleTemplate OWNER = new RoleTemplate("Owner", "OWNER", "Full access, including settings, team and roles.",
            EnumSet.allOf(Permission.class));
    static final RoleTemplate MANAGER = new RoleTemplate("Manager", "MANAGER", "Runs daily operations and approves requests.",
            EnumSet.complementOf(EnumSet.of(BILLING_VOID, TEAM_MANAGE, PAYROLL_MANAGE, SETTINGS_MANAGE)));
    static final RoleTemplate ACCOUNTANT = new RoleTemplate("Accountant", "ACCOUNTANT",
            "Handles invoices, expenses, payroll and reports.",
            EnumSet.of(CUSTOMERS_VIEW, BILLING_VIEW, BILLING_COLLECT, BILLING_VOID, EXPENSES_VIEW, EXPENSES_RECORD,
                    DAYBOOK_CLOSE, PAYROLL_MANAGE, REPORTS_VIEW));

    /** Categories every business starts with; SALARIES is used by payroll. */
    static final List<CategoryTemplate> COMMON_CATEGORIES = List.of(
            new CategoryTemplate("Rent", "RENT"), new CategoryTemplate("Salaries", "SALARIES"),
            new CategoryTemplate("Utilities", "UTILITIES"), new CategoryTemplate("Marketing", "MARKETING"),
            new CategoryTemplate("Other", "OTHER"));

    private final String label;
    private final Labels labels;
    private final Set<Module> defaultModules;
    private final List<RoleTemplate> typeRoles;
    private final List<String> extraCategories;

    BusinessType(String label, Labels labels, Set<Module> defaultModules, List<RoleTemplate> typeRoles,
                 List<String> extraCategories) {
        this.label = label;
        this.labels = labels;
        this.defaultModules = defaultModules;
        this.typeRoles = typeRoles;
        this.extraCategories = extraCategories;
    }

    public String label() {
        return label;
    }

    public Labels labels() {
        return labels;
    }

    public Set<Module> defaultModules() {
        return EnumSet.copyOf(defaultModules);
    }

    /** Roles seeded for a new business, Owner first. */
    public List<RoleTemplate> roles() {
        return java.util.stream.Stream.of(List.of(OWNER, MANAGER), typeRoles, List.of(ACCOUNTANT))
                .flatMap(List::stream).toList();
    }

    public List<CategoryTemplate> expenseCategories() {
        return java.util.stream.Stream.concat(COMMON_CATEGORIES.stream(),
                extraCategories.stream().map(name -> new CategoryTemplate(name, null))).toList();
    }

    /** What a business calls its customers, groups and plans. */
    public record Labels(String customer, String customers, String group, String groups, String plan, String plans) {
    }

    public record RoleTemplate(String name, String systemKey, String description, Set<Permission> permissions) {

        static RoleTemplate frontDesk(String name) {
            return new RoleTemplate(name, "FRONT_DESK", "Signs up customers, takes payments, renews plans and closes the day.",
                    EnumSet.of(CUSTOMERS_VIEW, CUSTOMERS_MANAGE, ATTENDANCE_MARK, MEMBERSHIPS_MANAGE, BILLING_VIEW,
                            BILLING_COLLECT, DAYBOOK_CLOSE));
        }

        static RoleTemplate coach(String name, String systemKey, String description) {
            return new RoleTemplate(name, systemKey, description, EnumSet.of(CUSTOMERS_VIEW, ATTENDANCE_MARK));
        }
    }

    public record CategoryTemplate(String name, String systemKey) {
    }
}
