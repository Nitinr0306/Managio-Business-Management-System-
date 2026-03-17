package com.nitin.saas.common.export;

import com.nitin.saas.member.entity.Member;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.subscription.entity.MemberSubscription;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CSVExportService {

    public byte[] exportMembers(List<Member> members) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("ID,First Name,Last Name,Phone,Email,Date of Birth,Gender,Address,Status,Created At");

        // CSV Data
        for (Member member : members) {
            writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    member.getId(),
                    escapeCSV(member.getFirstName()),
                    escapeCSV(member.getLastName()),
                    escapeCSV(member.getPhone()),
                    escapeCSV(member.getEmail()),
                    member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : "",
                    escapeCSV(member.getGender()),
                    escapeCSV(member.getAddress()),
                    escapeCSV(member.getStatus()),
                    member.getCreatedAt() != null ? member.getCreatedAt().toString() : ""
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    public byte[] exportPayments(List<Payment> payments, List<Member> members) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("Payment ID,Member Name,Member Phone,Amount,Payment Method,Notes,Payment Date");

        // CSV Data
        for (Payment payment : payments) {
            Member member = members.stream()
                    .filter(m -> m.getId().equals(payment.getMemberId()))
                    .findFirst()
                    .orElse(null);

            String memberName = member != null ? member.getFullName() : "Unknown";
            String memberPhone = member != null ? member.getPhone() : "";

            writer.printf("%d,%s,%s,%.2f,%s,%s,%s%n",
                    payment.getId(),
                    escapeCSV(memberName),
                    escapeCSV(memberPhone),
                    payment.getAmount(),
                    payment.getPaymentMethod().getDisplayName(),
                    escapeCSV(payment.getNotes()),
                    payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : ""
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    public byte[] exportSubscriptions(List<MemberSubscription> subscriptions, List<Member> members) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("Subscription ID,Member Name,Member Phone,Plan ID,Start Date,End Date,Status,Amount");

        // CSV Data
        for (MemberSubscription subscription : subscriptions) {
            Member member = members.stream()
                    .filter(m -> m.getId().equals(subscription.getMemberId()))
                    .findFirst()
                    .orElse(null);

            String memberName = member != null ? member.getFullName() : "Unknown";
            String memberPhone = member != null ? member.getPhone() : "";

            writer.printf("%d,%s,%s,%d,%s,%s,%s,%.2f%n",
                    subscription.getId(),
                    escapeCSV(memberName),
                    escapeCSV(memberPhone),
                    subscription.getPlanId(),
                    subscription.getStartDate() != null ? subscription.getStartDate().toString() : "",
                    subscription.getEndDate() != null ? subscription.getEndDate().toString() : "",
                    escapeCSV(subscription.getStatus()),
                    subscription.getAmount()
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    public byte[] getMemberTemplate() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("First Name,Last Name,Phone,Email,Date of Birth (YYYY-MM-DD),Gender (MALE/FEMALE/OTHER),Address,Notes");

        // Sample row
        writer.println("John,Doe,9876543210,john@example.com,1990-01-15,MALE,123 Main St,Premium member");

        writer.flush();
        return outputStream.toByteArray();
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}