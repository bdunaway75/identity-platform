package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.UUID;

@Entity
@Table(
        name = "authorities",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_authorities_name_registered_client",
                columnNames = {"registered_client_id", "authority_name"}
        )
)
@Getter
@NoArgsConstructor
public class AuthorityEntity {

    @Id
    @Column(name = "authority_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID authorityId;

    @Column(name = "authority_name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", nullable = false)
    private RegisteredClientEntity registeredClient;

    public AuthorityEntity(final String name, final RegisteredClientEntity parent) {
        Assert.hasText(name, "authority name must not be empty");
        Assert.notNull(parent, "parent must not be null");
        this.name = name.toUpperCase();
        this.registeredClient = parent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorityEntity other)) {
            return false;
        }
        if (name == null || other.name == null) {
            return false;
        }
        if (!name.equalsIgnoreCase(other.name)) {
            return false;
        }
        if (registeredClient == null && other.registeredClient == null) {
            return true;
        }
        return registeredClient != null && registeredClient.equals(other.registeredClient);
    }

    @Override
    public int hashCode() {
        if (name == null) {
            return 0;
        }
        final int nameHash = this.getName() == null ? 0 : name.toUpperCase().hashCode();
        return 31 * nameHash + (registeredClient == null ? 0 : registeredClient.getRegisteredClientId() != null ? registeredClient.getRegisteredClientId().hashCode() : 0);
    }

}
