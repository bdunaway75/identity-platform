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
            from platform_user pu
            join user_clients uc on uc.user_id = pu.id
            join registered_client rc on rc.registered_client_id = uc.registered_client_id
            join client_user cu on cu.client_id = rc.client_id
            where lower(pu.email) = lower(:email)
            """, nativeQuery = true)
    int getTotalUserCount(@Param("email") final String email);

    @Query(value = """
            select count(*)
            from platform_user pu
            join user_clients uc on uc.user_id = pu.id
            where lower(pu.email) = lower(:email)
            """, nativeQuery = true)
    int getTotalClientCount(@Param("email") final String email);

}

