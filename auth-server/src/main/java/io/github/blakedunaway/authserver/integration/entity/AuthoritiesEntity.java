package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(
        name = "authorities",
        uniqueConstraints = @UniqueConstraint(name = "uq_authorities_name", columnNames = "authority_name")
)
@Getter
public class AuthoritiesEntity {

    @Id
    @Column(name = "authority_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID authorityId;

    @Column(name = "authority_name")
    private String name;

    public static AuthoritiesEntity create(final String name) {
        AuthoritiesEntity authorities = new AuthoritiesEntity();
        authorities.name = name;
        return authorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthoritiesEntity other)) {
            return false;
        }
        return (name != null) && (authorityId.equals(other.authorityId) || name.equalsIgnoreCase(other.name));
    }

}
