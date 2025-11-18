package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface AuthoritiesJpaRepository extends JpaRepository<AuthoritiesEntity, UUID> {

    Set<AuthoritiesEntity> findAllByNameIn(final Set<String> names);

}
