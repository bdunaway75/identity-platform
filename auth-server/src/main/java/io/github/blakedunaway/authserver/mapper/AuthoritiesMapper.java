package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import org.mapstruct.Mapper;

@Mapper
public interface AuthoritiesMapper {

    AuthorityEntity authoritiesToAuthoritiesEntity(final Authorities authorities);

    Authorities authoritiesEntityToAuthorities(final AuthorityEntity authorityEntity);

}
