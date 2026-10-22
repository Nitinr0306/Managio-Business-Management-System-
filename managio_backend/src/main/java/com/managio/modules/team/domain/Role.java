package com.managio.modules.team.domain;

import com.managio.platform.tenancy.Permission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Set;

/** A named set of permissions within one business (D-007). */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    public static final String OWNER_KEY = "OWNER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private String name;
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] permissions;

    private String systemKey;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Role of(long businessId, String name, String description, Set<Permission> permissions, String systemKey) {
        Role role = new Role();
        role.businessId = businessId;
        role.name = name;
        role.description = description;
        role.permissions = Permission.toKeys(permissions);
        role.systemKey = systemKey;
        return role;
    }

    public boolean isOwnerRole() {
        return OWNER_KEY.equals(systemKey);
    }

    public Set<Permission> permissionSet() {
        return Permission.fromKeys(permissions);
    }

    public void update(String name, String description, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = Permission.toKeys(permissions);
    }
}
