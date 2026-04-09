package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.ClientUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ClientUserJpaRepository extends JpaRepository<ClientUserEntity, UUID> {

    Optional<ClientUserEntity> findByEmailIgnoreCase(final String email);

    Optional<ClientUserEntity> findByEmailAndClientId(final String clientId, final String email);

    int countAllByClientId(final String clientId);

    @Query(value = """
            select client_user.*
            from auth.client_user client_user
            join auth.registered_client registered_client on registered_client.client_id = client_user.client_id
            where registered_client.registered_client_id in (:registeredClientIds)
            """, nativeQuery = true)
    List<ClientUserEntity> findAllByRegisteredClientIds(@Param("registeredClientIds") final Set<UUID> registeredClientIds);

    @Query(value = """
            select client_user.*
            from auth.client_user client_user
            join auth.registered_client registered_client on registered_client.client_id = client_user.client_id
            where client_user.id = :clientUserId
              and registered_client.registered_client_id in (:registeredClientIds)
            """, nativeQuery = true)
    Optional<ClientUserEntity> findByIdAndRegisteredClientIds(@Param("clientUserId") final UUID clientUserId,
                                                              @Param("registeredClientIds") final Set<UUID> registeredClientIds);

}

