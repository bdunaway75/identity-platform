package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AuthTokenJpaRepository extends JpaRepository<AuthTokenEntity, UUID> {

    @Query(value = """
            select at.*
            from auth_token at
            join auth_authorization aa on aa.id = at.authorization_id
            join registered_client rc on rc.registered_client_id = aa.registered_client_id
            where rc.registered_client_id in (:registeredClientIds)
            order by at.issued_at desc
            """, nativeQuery = true)
    List<AuthTokenEntity> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds);

    @Modifying
    @Query(value = """
            update auth_token
            set revoked_at = :revokedAt
            where id = :authTokenId
              and revoked_at is null
              and exists (
                  select 1
                  from auth_authorization aa
                  join registered_client rc on rc.registered_client_id = aa.registered_client_id
                  where aa.id = auth_token.authorization_id
                    and rc.registered_client_id in (:registeredClientIds)
              )
            """, nativeQuery = true)
    int invalidateByIdAndRegisteredClientIds(final UUID authTokenId,
                                             final Set<UUID> registeredClientIds,
                                             final Instant revokedAt);

    @Modifying
    @Query(value = """
            update auth_token at
            set revoked_at = :revokedAt
            from auth_authorization aa
            where aa.id = at.authorization_id
              and aa.registered_client_id = :registeredClientId
              and at.revoked_at is null
            """, nativeQuery = true)
    int invalidateAllByRegisteredClientId(final UUID registeredClientId, final Instant revokedAt);
}
