package io.github.blakedunaway.authserver.business.service;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;
import io.github.blakedunaway.authserver.integration.repository.gateway.SigningKeyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SigningKeyStore {

    private final SigningKeyRepository signingKeyRepository;

    @PostConstruct
    public void ensureActiveKey() {
        if (signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE).isEmpty()) {
            this.save(createSigningKey());
        }
    }

    public SigningKey createSigningKey() {
        final KeyPair kp = generateRsaKey();
        final String kid = UUID.randomUUID().toString();
        return SigningKey.from(kid)
                         .keys(kp)
                         .algorithm("RS256")
                         .signingKeyStatus(SigningKeyStatus.ACTIVE)
                         .createdAt(LocalDateTime.now())
                         .build();
    }

    public JWKSource<SecurityContext> jwkSource() {
        return (selector, ctx) -> {
            final List<com.nimbusds.jose.jwk.JWK> jwks = signingKeyRepository.findAllByStatusIn(List.of(SigningKeyStatus.ACTIVE,
                                                                                                        SigningKeyStatus.INACTIVE))
                                                                             .stream()
                                                                             .sorted(Comparator.comparing(SigningKey::getCreatedAt).reversed())
                                                                             .map(this::toRsaKey)
                                                                             .collect(Collectors.toUnmodifiableList());
            if (jwks.isEmpty()) {
                return List.of();
            }

            return selector.select(new JWKSet(jwks));
        };
    }

    public SigningKey save(final SigningKey signingKey) {
        return signingKeyRepository.save(signingKey);
    }

    @Scheduled(cron = "0 0 3 */14 * ?")
    public void rotateSigningKeys() {
        SigningKey newKey = createSigningKey();
        signingKeyRepository.save(newKey);
        signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)
                            .stream()
                            .filter(k -> !k.getKid().equals(newKey.getKid()))
                            .forEach(k -> signingKeyRepository.save(k.retire()));
    }

    @Scheduled(cron = "0 0 0 * 1/2 ?")
    public void purgeSigningKeys() {
        signingKeyRepository.purgeInactiveKeys();
    }

    public KeyPair generateRsaKey() {
        try {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RSAKey toRsaKey(final SigningKey signingKey) {
        try {
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            final RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(signingKey.getPublicKey())));
            final RSAPrivateKey pri = signingKey.getStatus() == SigningKeyStatus.INACTIVE
                                ? null
                                : (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingKey.getPrivateKey())));
            final RSAKey.Builder rBuilder = new RSAKey.Builder(pub).keyUse(KeyUse.SIGNATURE)
                                                             .algorithm(new Algorithm(signingKey.getAlgorithm()))
                                                             .keyID(signingKey.getKid());
            if (pri != null) {
                return rBuilder.privateKey(pri).build();
            }
            return rBuilder.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Bad key material for kid :" + signingKey.getKid(), ex);
        }
    }

}
