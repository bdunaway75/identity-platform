package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;
import io.github.blakedunaway.authserver.integration.repository.gateway.DemoAccessCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoAccessCodeService {

    private final DemoAccessCodeRepository demoAccessCodeRepository;

    public DemoAccessCode save(final DemoAccessCode demoAccessCode) {
        Assert.notNull(demoAccessCode, "DemoAccessCode cannot be null");
        return demoAccessCodeRepository.save(demoAccessCode);
    }

    public DemoAccessCode findById(final UUID id) {
        return demoAccessCodeRepository.findById(id).orElse(null);
    }

    public DemoAccessCode findByAccessCode(final String accessCode) {
        return demoAccessCodeRepository.findByAccessCode(accessCode).orElse(null);
    }

    public List<DemoAccessCode> findAll() {
        return demoAccessCodeRepository.findAll();
    }

    public Optional<DemoAccessCode> findByPlatformUserId(final UUID platformUserId) {
        return demoAccessCodeRepository.findByPlatformUserId(platformUserId);
    }

}
