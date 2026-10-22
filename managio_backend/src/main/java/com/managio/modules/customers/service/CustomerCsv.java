package com.managio.modules.customers.service;

import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.repository.CustomerRepository;
import com.managio.modules.business.service.BusinessSequences;
import com.managio.modules.business.service.BusinessSequences.Sequence;
import com.managio.modules.customers.domain.Customer.Details;
import com.managio.platform.util.Csv;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Customer CSV export and import. Import is forgiving: valid rows are added, invalid or duplicate rows are
 * skipped and reported with their row number, so a long file never fails because of one bad line.
 */
@Component
@RequiredArgsConstructor
public class CustomerCsv {

    static final int MAX_ROWS = 5_000;
    static final long MAX_BYTES = 2 * 1024 * 1024;
    static final List<String> COLUMNS = List.of("full_name", "phone", "email", "date_of_birth", "gender", "address",
            "guardian_name", "guardian_phone", "joined_on", "notes");
    /** Strict, so 31/02/2020 is rejected instead of being quietly moved to 29 February. */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT));
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerRepository customers;
    private final BusinessSequences sequences;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Clock clock;

    public record ImportResult(int added, int skipped, List<RowError> errors) {
    }

    public record RowError(int row, String message) {
    }

    public byte[] export(long businessId) {
        List<List<String>> rows = jdbc.sql("""
                        SELECT number, full_name, phone, email, date_of_birth, gender, address, guardian_name,
                               guardian_phone, joined_on, status, notes
                        FROM customers WHERE business_id = :businessId ORDER BY number""")
                .param("businessId", businessId)
                .query((rs, i) -> {
                    List<String> row = new ArrayList<>();
                    for (int c = 1; c <= 12; c++) {
                        row.add(rs.getString(c));
                    }
                    return row;
                })
                .list();
        List<String> header = List.of("number", "full_name", "phone", "email", "date_of_birth", "gender", "address",
                "guardian_name", "guardian_phone", "joined_on", "status", "notes");
        return ("﻿" + Csv.write(header, rows)).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ImportResult importFile(BusinessContext ctx, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.invalidField("file", "Choose a CSV file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.invalidField("file", "The file is larger than 2 MB. Split it into smaller files.");
        }
        List<List<String>> rows;
        try {
            rows = Csv.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw ApiException.invalidField("file", "The file could not be read");
        }
        if (rows.isEmpty()) {
            throw ApiException.invalidField("file", "The file is empty");
        }
        Map<String, Integer> columns = headerIndex(rows.getFirst());
        if (!columns.containsKey("full_name")) {
            throw ApiException.invalidField("file", "The first row must be a header with a full_name column");
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw ApiException.invalidField("file", "Import at most " + MAX_ROWS + " customers at a time");
        }

        Set<String> knownPhones = new HashSet<>(jdbc.sql(
                        "SELECT phone FROM customers WHERE business_id = :businessId AND status = 'ACTIVE' AND phone IS NOT NULL")
                .param("businessId", ctx.businessId()).query(String.class).list());
        LocalDate today = ctx.today(clock);
        List<RowError> errors = new ArrayList<>();
        int added = 0;
        for (int r = 1; r < rows.size(); r++) {
            int rowNumber = r + 1;
            List<String> row = rows.get(r);
            try {
                Details details = CustomerService.clean(details(row, columns));
                if (details.phone() != null && !knownPhones.add(details.phone())) {
                    errors.add(new RowError(rowNumber, "Skipped: phone " + details.phone() + " already belongs to a customer"));
                    continue;
                }
                LocalDate joinedOn = Objects.requireNonNullElse(date(value(row, columns, "joined_on"), "joined_on"), today);
                int number = sequences.next(ctx.businessId(), Sequence.CUSTOMER);
                customers.save(Customer.create(ctx.businessId(), number, details, joinedOn, ctx.userId()));
                added++;
            } catch (ApiException e) {
                String message = e.fieldErrors().isEmpty() ? e.getMessage() : String.join("; ", e.fieldErrors().values());
                errors.add(new RowError(rowNumber, message));
            }
        }
        activity.record(ctx, "CUSTOMERS_IMPORTED", "CUSTOMER", null, "Imported " + added + " customers from CSV");
        return new ImportResult(added, errors.size(), errors);
    }

    private static Details details(List<String> row, Map<String, Integer> columns) {
        String email = Normalize.email(value(row, columns, "email"));
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw ApiException.invalidField("email", "Invalid email " + email);
        }
        String fullName = value(row, columns, "full_name");
        if (fullName != null && fullName.length() > 150) {
            throw ApiException.invalidField("full_name", "Name is longer than 150 characters");
        }
        return new Details(fullName, value(row, columns, "phone"), email,
                date(value(row, columns, "date_of_birth"), "date_of_birth"), truncate(value(row, columns, "gender"), 10),
                truncate(value(row, columns, "address"), 300), truncate(value(row, columns, "guardian_name"), 150),
                value(row, columns, "guardian_phone"), truncate(value(row, columns, "notes"), 4000));
    }

    private static Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.putIfAbsent(header.get(i).trim().toLowerCase(Locale.ROOT).replace(' ', '_'), i);
        }
        return index;
    }

    private static String value(List<String> row, Map<String, Integer> columns, String column) {
        Integer index = columns.get(column);
        return index == null || index >= row.size() ? null : Normalize.text(row.get(index));
    }

    private static LocalDate date(String value, String column) {
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw ApiException.invalidField(column, "Use dates like 2026-10-20 or 20/10/2026 (" + column + ": " + value + ")");
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
