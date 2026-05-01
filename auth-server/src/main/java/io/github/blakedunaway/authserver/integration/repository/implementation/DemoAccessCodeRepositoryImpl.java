package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;
import io.github.blakedunaway.authserver.integration.entity.DemoAccessCodeEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.DemoAccessCodeRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.DemoAccessCodeJpaRepository;
import io.github.blakedunaway.authserver.mapper.DemoAccessCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemoAccessCodeRepositoryImpl implements DemoAccessCodeRepository {

    private final DemoAccessCodeJpaRepository demoAccessCodeJpaRepository;

    private final DemoAccessCodeMapper demoAccessCodeMapper;

    @Override
    @Transactional
    public DemoAccessCode save(final DemoAccessCode demoAccessCode) {
        Assert.notNull(demoAccessCode, "DemoAccessCode cannot be null");
        Assert.hasText(demoAccessCode.getAccessCode(), "Access code cannot be blank");
        Assert.notNull(demoAccessCode.getUser(), "Platform user cannot be null");
        Assert.notNull(demoAccessCode.getUser().getId(), "Platform user id cannot be null");

        if (demoAccessCode.getId() != null) {
            final DemoAccessCodeEntity existingEntity = demoAccessCodeJpaRepository.findById(demoAccessCode.getId())
                                                                                   .orElse(null);
            if (existingEntity != null) {
                final DemoAccessCodeEntity updatedEntity = new DemoAccessCodeEntity(
                        existingEntity.getAccessCodeId(),
                        existingEntity.getAccessCode(),
                        existingEntity.getUser(),
                        demoAccessCode.getUseLimit(),
                        demoAccessCode.getUseCount()
                );
                return demoAccessCodeMapper.demoAccessCodeEntityToDemoAccessCode(
                        demoAccessCodeJpaRepository.save(updatedEntity)
                );
            }
        }

        return demoAccessCodeMapper.demoAccessCodeEntityToDemoAccessCode(
                demoAccessCodeJpaRepository.save(demoAccessCodeMapper.demoAccessCodeToDemoAccessCodeEntity(demoAccessCode))
        );
    }

    @Override
    public Optional<DemoAccessCode> findById(final UUID id) {
        return demoAccessCodeJpaRepository.findById(id).map(demoAccessCodeMapper::demoAccessCodeEntityToDemoAccessCode);
    }

    @Override
    public Optional<DemoAccessCode> findByAccessCode(final String accessCode) {
        return demoAccessCodeJpaRepository.findByAccessCode(accessCode).map(demoAccessCodeMapper::demoAccessCodeEntityToDemoAccessCode);
    }

    @Override
    public Optional<DemoAccessCode> findByPlatformUserId(final UUID platformUserId) {
        return demoAccessCodeJpaRepository.findByUser_UserId(platformUserId)
                                          .map(demoAccessCodeMapper::demoAccessCodeEntityToDemoAccessCode);
    }

    @Override
    public List<DemoAccessCode> findAll() {
        return demoAccessCodeJpaRepository.findAll()
                                          .stream()
                                          .map(demoAccessCodeMapper::demoAccessCodeEntityToDemoAccessCode)
                                          .collect(Collectors.toList());
    }

}
