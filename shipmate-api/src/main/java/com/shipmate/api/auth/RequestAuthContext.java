package com.shipmate.api.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RequestAuthContext {
    private static final ThreadLocal<MerchantUser> CURRENT_USER = new ThreadLocal<>();

    public void set(MerchantUser user) {
        CURRENT_USER.set(user);
    }

    public Optional<MerchantUser> currentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public String currentMerchantIdOr(String fallbackMerchantId) {
        return currentUser()
                .map(MerchantUser::merchantId)
                .orElse(fallbackMerchantId);
    }

    public String currentActorIdOr(String fallbackActorId) {
        return currentUser()
                .map(MerchantUser::merchantUserId)
                .orElse(fallbackActorId);
    }

    public boolean hasRole(String role) {
        return currentUser()
                .map(user -> user.role().equals(role))
                .orElse(false);
    }

    public void clear() {
        CURRENT_USER.remove();
    }
}
