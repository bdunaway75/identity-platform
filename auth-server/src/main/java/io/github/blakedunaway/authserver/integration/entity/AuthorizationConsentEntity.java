package io.github.blakedunaway.authserver.integration.entity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_authorization_consent", indexes = {
        @Index(name = "ix_auth_rid", columnList = "registered_client_id"),
        @Index(name = "ix_auth_pname", columnList = "principal_name")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"registered_client_id", "principal_name"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorizationConsentEntity implements Persistable<String> {

    @Id
    @Column(name = "authorization_consent_id", updatable = false, nullable = false)
    private UUID consentId;

    @Transient
    private boolean isNew = true;

    @Column(name = "registered_client_id", nullable = false, updatable = false, length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 256)
    private String principalName;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name = "auth_authorization_consent_authorities",
            joinColumns = @JoinColumn(name = "authorization_consent_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private Set<AuthoritiesEntity> authorities;

    @Override
    public String getId() {
        return consentId != null ? consentId.toString() : null;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public static AuthorizationConsentEntity create(UUID consentId, String registeredClientId, String principalName, boolean isNew, Set<AuthoritiesEntity> authorities) {
        Assert.notNull(consentId, "consentId cannot be null");
        Assert.notNull(principalName, "principalName cannot be null");
        Assert.notNull(registeredClientId, "registeredClientId cannot be null");
        Assert.notNull(authorities, "authorities cannot be null");
        AuthorizationConsentEntity entity = new AuthorizationConsentEntity();
        entity.consentId = consentId;
        entity.registeredClientId = registeredClientId;
        entity.principalName = principalName;
        entity.authorities = authorities;
        entity.isNew = isNew;
        return entity;
    }

}
