package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "platform_user_tier")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlatformUserTierEntity {

    @Setter
    @Id
    @Column(name = "tier_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tierId;

    @Column(name = "price_id")
    private String stripPriceId;

    @Column(name = "tier_name")
    private String tierName;

    @Column(name = "tier_price")
    private int tierPrice;

    @Column(name = "tier_description")
    private String tierDescription;

    @Column(name = "tier_order")
    private int tierOrder;

    @Column(name = "allowed_number_of_registered_clients")
    private int allowedNumberOfRegisteredClients;

    @Column(name = "allowed_number_of_global_user")
    private int allowedNumberOfGlobalUsers;

    @Column(name = "allowed_number_of_global_scopes")
    private int allowedNumberOfGlobalScopes;

    @Column(name = "allowed_number_of_global_authorities")
    private int allowedNumberOfGlobalAuthorities;
}
