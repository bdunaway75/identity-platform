package io.github.blakedunaway.authserver.integration.entity;

import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "signing_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id", "kid", "encoded_public_key", "encoded_private_key"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SigningKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "kid", nullable = false, unique = true)
    private String kid;

    @Column(name = "encoded_public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "encoded_private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "algo")
    private String algorithm;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SigningKeyStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany
    @JoinColumn(name = "kid", referencedColumnName = "kid", insertable = false, updatable = false)
    private Set<AuthTokenEntity> tokens;

}
