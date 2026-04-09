package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "demo_access_code")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DemoAccessCodeEntity {

    @Setter
    @Id
    @Column(name = "access_code_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID accessCodeId;

    @Column(name = "access_code")
    private String accessCode;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "platform_user_id", nullable = false)
    private PlatformUserEntity user;

    @Column(name = "code_dispensed")
    private boolean dispensed;
}
