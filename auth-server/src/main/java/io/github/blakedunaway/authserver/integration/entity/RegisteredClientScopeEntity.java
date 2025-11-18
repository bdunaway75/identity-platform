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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "registered_client_scope",
        uniqueConstraints = @UniqueConstraint(columnNames = {"registered_client_id", "scope"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientScopeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", nullable = false)
    private RegisteredClientEntity registeredClient;

    @Column(name = "scope", nullable = false)
    private String scope;

    public RegisteredClientScopeEntity(RegisteredClientEntity parent, String scope) {
        this.registeredClient = parent;
        this.scope = scope;
    }

}
