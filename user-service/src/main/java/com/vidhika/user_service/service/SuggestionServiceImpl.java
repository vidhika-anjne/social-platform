package com.vidhika.user_service.service;

import com.vidhika.user_service.model.Follow;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.FollowRepository;
import com.vidhika.user_service.repository.SuggestionService;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService
{
    private final FollowRepository followRepository;
    private final UserProfileRepository userProfileRepository;
    public List<UserProfile> getSuggestions(UUID userId)
    {
        // fetch all the users which this user follows;
        List<Follow> followList = followRepository.findByFollowerId(userId);
        Set<UUID> FOLLOW_LIST = new HashSet<>(followList.stream()
                .map(Follow::getFollowingId)
                .toList());
        /*
            Now this Set contains all the unique users which
            are followed by our current user.
            A->B
            (FOLLOW_LIST contains all B)
         */
        FOLLOW_LIST.remove(userId);
        /*
            A->B->C
            (SUGGESTION_LIST contains all C)
         */
        Set<UUID> SUGGESTION_LIST = new HashSet<>();
        for(UUID followerId : FOLLOW_LIST)
        {
            List<Follow> suggestionList = followRepository.findByFollowerId(followerId);
            SUGGESTION_LIST.addAll(suggestionList.stream()
                    .map(Follow::getFollowingId)
                    .toList()
            );
        }
        SUGGESTION_LIST.removeAll(FOLLOW_LIST);
        return userProfileRepository.findAllById(SUGGESTION_LIST);
    }
}
