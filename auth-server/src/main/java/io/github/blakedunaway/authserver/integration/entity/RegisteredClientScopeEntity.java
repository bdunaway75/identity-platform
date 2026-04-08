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
@Table(name = "scopes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientScopeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope", nullable = false)
    private String scope;

    public RegisteredClientScopeEntity(final UUID id, final String scope) {
        this.id = id;
        this.scope = scope;
    }

}
