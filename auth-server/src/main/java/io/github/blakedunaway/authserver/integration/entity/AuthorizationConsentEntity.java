package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_authorization_consent")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorizationConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "authorization_consent_id", updatable = false, nullable = false)
    private UUID consentId;

    @Column(name = "registered_client_id", nullable = false, updatable = false)
    private UUID registeredClientId;

    @Column(name = "principal_name", nullable = false)
    private String principalName;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "auth_authorization_consent_authorities",
            joinColumns = @JoinColumn(name = "authorization_consent_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private Set<AuthorityEntity> authorities;

    public static AuthorizationConsentEntity create(final UUID consentId,
                                                    final UUID registeredClientId,
                                                    final String principalName,
                                                    final Set<AuthorityEntity> authorities) {
        Assert.notNull(principalName, "principalName cannot be null");
        Assert.notNull(registeredClientId, "registeredClientId cannot be null");
        Assert.notNull(authorities, "authorities cannot be null");
        final AuthorizationConsentEntity entity = new AuthorizationConsentEntity();
        entity.consentId = consentId;
        entity.registeredClientId = registeredClientId;
        entity.principalName = principalName;
        entity.authorities = authorities;
        return entity;
    }

}
