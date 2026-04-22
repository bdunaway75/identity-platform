package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;
import io.github.blakedunaway.authserver.integration.entity.DemoAccessCodeEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper
public abstract class DemoAccessCodeMapper {

    @Autowired
    private UserMapper userMapper;

    public DemoAccessCode demoAccessCodeEntityToDemoAccessCode(final DemoAccessCodeEntity entity) {
        if (entity == null) {
            return null;
        }

        return DemoAccessCode.builder()
                             .id(entity.getAccessCodeId())
                             .accessCode(entity.getAccessCode())
                             .user(entity.getUser() == null ? null : userMapper.platformUserEntityToPlatformUser(entity.getUser()))
                             .dispensed(entity.isDispensed())
                             .build();
    }

    public DemoAccessCodeEntity demoAccessCodeToDemoAccessCodeEntity(final DemoAccessCode demoAccessCode) {
        if (demoAccessCode == null) {
            return null;
        }

        final PlatformUserEntity platformUserEntity =
                demoAccessCode.getUser() == null ? null : userMapper.platformUserToPlatformUserEntity(demoAccessCode.getUser());

        return new DemoAccessCodeEntity(
                demoAccessCode.getId(),
                demoAccessCode.getAccessCode(),
                platformUserEntity,
                demoAccessCode.isDispensed()
        );
    }

}
