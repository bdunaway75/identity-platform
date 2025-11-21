package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "registered_client_auth_method",
        uniqueConstraints = @UniqueConstraint(columnNames = {"registered_client_id", "client_auth_method"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientAuthMethodEntity {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", nullable = false)
    private RegisteredClientEntity registeredClient;

    @Column(name = "client_auth_method", nullable = false)
    private String clientAuthMethod;

    public RegisteredClientAuthMethodEntity(final RegisteredClientEntity parent, final String method) {
        this.registeredClient = parent;
        this.clientAuthMethod = method;
    }
}
