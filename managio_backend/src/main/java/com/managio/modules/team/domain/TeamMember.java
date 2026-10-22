package com.managio.modules.team.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A user's membership in a business: their role, status and employment details. */
@Entity
@Table(name = "team_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    public enum Status { ACTIVE, SUSPENDED, LEFT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private long userId;
    private long roleId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private String title;
    private String employeeCode;
    private String phone;
    private LocalDate joinedOn;
    private LocalDate leftOn;
    private BigDecimal monthlySalary;
    private String notes;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static TeamMember join(long businessId, long userId, long roleId, String title, LocalDate joinedOn) {
        TeamMember member = new TeamMember();
        member.businessId = businessId;
        member.userId = userId;
        member.roleId = roleId;
        member.title = title;
        member.joinedOn = joinedOn;
        return member;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    /** Someone who left or was suspended accepts a new invitation. */
    public void rejoin(long roleId, String title, LocalDate joinedOn) {
        this.roleId = roleId;
        if (title != null) {
            this.title = title;
        }
        if (status == Status.LEFT) {
            this.joinedOn = joinedOn;
            this.leftOn = null;
        }
        this.status = Status.ACTIVE;
    }

    public void changeRole(long roleId) {
        this.roleId = roleId;
    }

    public void updateDetails(String title, String employeeCode, String phone, LocalDate joinedOn, String notes) {
        this.title = title;
        this.employeeCode = employeeCode;
        this.phone = phone;
        if (joinedOn != null) {
            this.joinedOn = joinedOn;
        }
        this.notes = notes;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public void suspend() {
        this.status = Status.SUSPENDED;
    }

    public void reactivate() {
        this.status = Status.ACTIVE;
    }

    public void leave(LocalDate leftOn) {
        this.status = Status.LEFT;
        this.leftOn = leftOn;
    }
}
