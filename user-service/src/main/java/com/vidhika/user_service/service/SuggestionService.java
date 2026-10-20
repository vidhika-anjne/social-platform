package com.vidhika.user_service.service;

import com.vidhika.user_service.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface SuggestionService
{
    List<UserProfile> getSuggestions(UUID userId);
}
