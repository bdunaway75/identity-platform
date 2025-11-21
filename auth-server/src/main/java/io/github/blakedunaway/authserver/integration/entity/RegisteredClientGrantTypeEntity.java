package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "registered_client_grant_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"registered_client_id", "authorization_grant_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientGrantTypeEntity {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", nullable = false)
    private RegisteredClientEntity registeredClient;

    @Column(name = "authorization_grant_type", nullable = false)
    private String authorizationGrantType;

    public RegisteredClientGrantTypeEntity(final RegisteredClientEntity parent, final String grantType) {
        this.registeredClient = parent;
        this.authorizationGrantType = grantType;
    }
}
