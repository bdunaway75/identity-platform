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
public class AuthorityEntity {

    @Id
    @Column(name = "authority_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID authorityId;

    @Column(name = "authority_name")
    private String name;

    public static AuthorityEntity create(final String name) {
        AuthorityEntity authorities = new AuthorityEntity();
        authorities.name = name.toUpperCase();
        return authorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorityEntity other)) {
            return false;
        }
        return (name != null) && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.toUpperCase().hashCode() : 0;
    }

}
