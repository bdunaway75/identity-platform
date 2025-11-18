package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "registered_client_redirect_uri",
        uniqueConstraints = @UniqueConstraint(columnNames = {"registered_client_id", "redirect_uri"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientRedirectUriEntity {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_client_id", nullable = false)
    private RegisteredClientEntity registeredClient;

    @Column(name = "redirect_uri", nullable = false)
    private String redirectUri;

    public RegisteredClientRedirectUriEntity(RegisteredClientEntity parent, String uri) {
        this.registeredClient = parent;
        this.redirectUri = uri;
    }
}
