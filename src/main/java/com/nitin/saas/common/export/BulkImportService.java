package com.nitin.saas.common.export;

import com.nitin.saas.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkImportService {

    public BulkImportResult importMembers(Long businessId, MultipartFile file) throws Exception {
        List<Member> members = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                try {
                    Member member = parseMemberFromCSV(line, businessId, rowNumber);
                    members.add(member);
                    successCount++;
                } catch (Exception e) {
                    errors.add(String.format("Row %d: %s", rowNumber, e.getMessage()));
                    errorCount++;
                }
            }
        }

        return BulkImportResult.builder()
                .totalRows(successCount + errorCount)
                .successCount(successCount)
                .errorCount(errorCount)
                .members(members)
                .errors(errors)
                .build();
    }

    private Member parseMemberFromCSV(String line, Long businessId, int rowNumber) {
        String[] fields = parseCSVLine(line);

        if (fields.length < 7) {
            throw new IllegalArgumentException("Insufficient fields. Expected at least 7 fields.");
        }

        try {
            return Member.builder()
                    .businessId(businessId)
                    .firstName(fields[0].trim())
                    .lastName(fields[1].trim())
                    .phone(fields[2].trim())
                    .email(fields[3].trim().isEmpty() ? null : fields[3].trim())
                    .dateOfBirth(fields[4].trim().isEmpty() ? null : LocalDate.parse(fields[4].trim()))
                    .gender(fields[5].trim().isEmpty() ? null : fields[5].trim())
                    .address(fields[6].trim().isEmpty() ? null : fields[6].trim())
                    .notes(fields.length > 7 ? fields[7].trim() : null)
                    .status("ACTIVE")
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse member data: " + e.getMessage());
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quotes
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Add last field
        result.add(currentField.toString());

        return result.toArray(new String[0]);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkImportResult {
        private Integer totalRows;
        private Integer successCount;
        private Integer errorCount;
        private List<Member> members;
        private List<String> errors;
    }
}