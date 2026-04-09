package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformUserJpaRepository extends JpaRepository<PlatformUserEntity, UUID> {

    Optional<PlatformUserEntity> findByEmailIgnoreCase(final String email);

    @Query(value = """
            select count(*)
            from auth.platform_user platform_user
            join auth.user_clients platform_user_client on platform_user_client.user_id = platform_user.id
            join auth.registered_client registered_client on registered_client.registered_client_id = platform_user_client.registered_client_id
            join auth.client_user client_user on client_user.client_id = registered_client.client_id
            where lower(platform_user.email) = lower(:email)
            """, nativeQuery = true)
    int getTotalUserCount(@Param("email") final String email);

    @Query(value = """
            select count(*)
            from auth.platform_user platform_user
            join auth.user_clients platform_user_client on platform_user_client.user_id = platform_user.id
            where lower(platform_user.email) = lower(:email)
            """, nativeQuery = true)
    int getTotalClientCount(@Param("email") final String email);


}

