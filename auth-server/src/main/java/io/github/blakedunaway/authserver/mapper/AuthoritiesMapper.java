package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import org.mapstruct.Mapper;

@Mapper
public interface AuthoritiesMapper {

    AuthoritiesEntity authoritiesToAuthoritiesEntity(final Authorities authorities);

    Authorities authoritiesEntityToAuthorities(final AuthoritiesEntity authoritiesEntity);

}
