package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AuthTokenJpaRepository extends JpaRepository<AuthTokenEntity, UUID> {

    Set<AuthTokenEntity> findAllByAuthorizationEntity_AuthId(final UUID authorizationEntityAuthId);

    Set<AuthTokenEntity> findByTokenValueHash(final String tokenValueHash);


    @Query("select t.tokenValueHash, t.tokenId from AuthTokenEntity t where t.tokenValueHash in :hashes")
    List<Object[]> findIdsByTokenValueHashes(@Param("hashes") final Set<String> hashes);

    boolean existsByTokenValueHash(final String tokenValueHash);
    
}
