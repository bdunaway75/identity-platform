package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_user")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClientUserEntity extends AbstractUserEntity {

    public ClientUserEntity(
            final UUID userId,
            final String clientId,
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
        this.clientId = clientId;
    }

    @Column(name = "client_id", nullable = false, updatable = false)
    private String clientId;

}
