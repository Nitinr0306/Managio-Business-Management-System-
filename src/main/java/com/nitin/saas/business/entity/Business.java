package com.nitin.saas.business.entity;

import com.nitin.saas.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "businesses")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(optional = false)
    private User owner;

    protected Business() {}

    public Business(String name, String slug, User owner) {
        this.name = name;
        this.slug = slug;
        this.owner = owner;
    }
}
