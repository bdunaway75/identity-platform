package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SigningKey {

    private final UUID id;

    private final String kid;

    private final String publicKey;

    private final String privateKey;

    private final String algorithm;

    private final SigningKeyStatus status;

    private final LocalDateTime createdAt;

    private final Set<AuthToken> tokens;

    private final boolean encoded;

    public static Builder from(final String kid) {
        return new Builder(kid);
    }

    public static Builder from(final UUID id) {
        return new Builder(id);
    }

    public SigningKey retire() {
        return SigningKey.from(this.getId())
                         .encoded(this.isEncoded())
                         .keys(this.getPrivateKey(), this.getPublicKey())
                         .signingKeyStatus(SigningKeyStatus.INACTIVE)
                         .authTokens(this.getTokens())
                         .algorithm(this.getAlgorithm())
                         .createdAt(this.getCreatedAt())
                         .kid(this.getKid())
                         .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private UUID id;

        private String kid;

        private String publicKey;

        private String privateKey;

        private String algorithm;

        private SigningKeyStatus status;

        private Set<AuthToken> authTokens;

        private LocalDateTime createdAt;

        private boolean encoded = false;

        protected Builder(final UUID id) {
            this.id = id;
        }

        protected Builder(final String kid) {
            this.kid = kid;
        }

        public Builder id(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder encoded(final boolean encoded) {
            this.encoded = encoded;
            return this;
        }

        public Builder kid(final String kid) {
            this.kid = kid;
            return this;
        }

        //Used for new keys
        public Builder keys(final KeyPair keyPair) {
            this.publicKey = Base64.getEncoder()
                                   .encodeToString(keyPair.getPublic()
                                                          .getEncoded());
            this.privateKey = Base64.getEncoder()
                                    .encodeToString(keyPair.getPrivate()
                                                           .getEncoded());
            this.encoded = true;
            return this;
        }

        //Used for already persisted keys (already encoded)
        public Builder keys(final String privateKey, final String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.encoded = true;
            return this;
        }


        public Builder signingKeyStatus(final SigningKeyStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder algorithm(final String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder authTokens(final Set<AuthToken> authTokens) {
            this.authTokens = authTokens;
            return this;
        }

        public SigningKey build() {
            Assert.hasText(this.getKid(), "Key ID must not be empty");
            Assert.hasText(this.getPublicKey(), "Public Key must not be empty");
            Assert.hasText(this.getPrivateKey(), "Private Key must not be empty");
            Assert.hasText(this.getAlgorithm(), "Algorithm must not be empty");
            Assert.isTrue(this.isEncoded(), "Keys must be encoded");
            return new SigningKey(this.getId(),
                                  this.getKid(),
                                  this.getPublicKey(),
                                  this.getPrivateKey(),
                                  this.getAlgorithm(),
                                  this.getStatus(),
                                  this.getCreatedAt(),
                                  this.getAuthTokens(),
                                  this.isEncoded());
        }

    }

}
