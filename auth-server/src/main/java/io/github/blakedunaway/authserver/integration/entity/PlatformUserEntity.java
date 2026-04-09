package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "platform_user")
@AssociationOverrides({
        @AssociationOverride(
                name = "authorities",
                joinTable = @JoinTable(
                        name = "platform_user_authorities",
                        joinColumns = @JoinColumn(name = "user_id"),
                        inverseJoinColumns = @JoinColumn(name = "authority_id")
                )
        )
})
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlatformUserEntity extends AbstractUserEntity {

    public PlatformUserEntity(
            final UUID userId,
            final Set<RegisteredClientEntity> registeredClients,
            final String email,
            final String passwordHash,
            final boolean isVerified,
            final LocalDateTime createdAt,
            final LocalDateTime updatedAt,
            final Map<String, Object> userAttributes,
            final Set<AuthorityEntity> authorities,
            final boolean locked,
            final boolean expired,
            final boolean credentialsExpired
    ) {
        super(
                userId,
                email,
                passwordHash,
                isVerified,
                createdAt,
                updatedAt,
                userAttributes,
                authorities,
                locked,
                expired,
                credentialsExpired
        );
        this.registeredClients = registeredClients;
    }

    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_clients",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "registered_client_id")
    )
    private Set<RegisteredClientEntity> registeredClients;

    @Setter
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id")
    private PlatformUserTierEntity tier;

}
