// Entity là lớp đại diện cho dữ liệu trong database, User Entity có chức năng ánh xạ bảng "User" trong PostgreSQL thành một đối tượng Java

package com.worldexplorer.world_explorer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import java.time.OffsetDateTime;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"User\"")
public class User {

    @Id
    @UuidGenerator
    @Column(name = "\"id\"", nullable = false)
    private UUID id;

    @Column(name = "\"email\"", nullable = false, unique = true)
    private String email;

    @Column(name = "\"fullName\"", nullable = false)
    private String fullName;

    @Column(name = "\"avatarUrl\"")
    private String avatarUrl;

    @Column(name = "\"roleId\"", nullable = false)
    private UUID roleId;

    @Column(name = "\"isActive\"", nullable = false)
    private boolean isActive = true;

    @Column(name = "\"createdAt\"", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "\"updatedAt\"", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "\"deletedAt\"")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void deactivateAndDelete() {
        this.isActive = false;
        this.deletedAt = OffsetDateTime.now();
    }
}
