package com.vidhika.auth_service.service;

import com.vidhika.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        try {
            UUID id = UUID.fromString(identifier);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));
        }
    }

    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }
}