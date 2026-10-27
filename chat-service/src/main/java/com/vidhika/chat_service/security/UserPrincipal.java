package com.vidhika.chat_service.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {

    private final String userId;
    private final String email;
    private final String role;

    @Override
    public String getName() {
        return userId;
    }
}
