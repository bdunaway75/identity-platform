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
            select auth_token.*
            from auth.auth_token auth_token
            join auth.auth_authorization authorization_record on authorization_record.id = auth_token.authorization_id
            join auth.registered_client registered_client on registered_client.registered_client_id = authorization_record.registered_client_id
            where registered_client.registered_client_id in (:registeredClientIds)
            order by auth_token.issued_at desc
            """, nativeQuery = true)
    List<AuthTokenEntity> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds);

    @Modifying
    @Query(value = """
            update auth.auth_token
            set revoked_at = :revokedAt
            where id = :authTokenId
              and revoked_at is null
              and exists (
                  select 1
                  from auth.auth_authorization authorization_record
                  join auth.registered_client registered_client on registered_client.registered_client_id = authorization_record.registered_client_id
                  where authorization_record.id = auth.auth_token.authorization_id
                    and registered_client.registered_client_id in (:registeredClientIds)
              )
            """, nativeQuery = true)
    int invalidateByIdAndRegisteredClientIds(final UUID authTokenId,
                                             final Set<UUID> registeredClientIds,
                                             final Instant revokedAt);

    @Modifying
    @Query(value = """
            update auth.auth_token
            set revoked_at = :revokedAt
            where revoked_at is null
              and exists (
                  select 1
                  from auth.auth_authorization authorization_record
                  where authorization_record.id = auth.auth_token.authorization_id
                    and authorization_record.registered_client_id = :registeredClientId
              )
            """, nativeQuery = true)
    int invalidateAllByRegisteredClientId(final UUID registeredClientId, final Instant revokedAt);
}
