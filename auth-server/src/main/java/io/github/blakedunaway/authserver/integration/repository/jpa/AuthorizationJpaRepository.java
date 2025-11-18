package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorizationJpaRepository extends JpaRepository<AuthorizationEntity, UUID> {

    AuthorizationEntity save(final AuthorizationEntity authorizationEntity);

    void deleteById(final UUID id);

    Optional<AuthorizationEntity> findById(final UUID id);

    Optional<AuthorizationEntity> findByTokens_TokenValueHash(final String tokenValueHash);

    List<AuthorizationEntity> findAll();

    @Query(value = " select * from auth_authorization where attributes_json @> cast(:json as jsonb)", nativeQuery = true)
    AuthorizationEntity findByAttribute(@Param("json") final String json);



}
