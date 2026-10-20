package com.vidhika.user_service.service;

import com.vidhika.user_service.model.Follow;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.FollowRepository;
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
        List<UUID> followList = followRepository.findByFollowerId(userId).stream()
                .map((Follow::getFollowingId))
                .toList();
        if(followList.isEmpty()) return List.of();
        /*
            Now this Set contains all the unique users which
            are followed by our current user.
            A->B
            (FOLLOW_LIST contains all B)
         */
        List<UUID> suggestionList = followRepository.findByFollowerIdInOrderByFollowingIdAsc(followList).stream()
                .map(Follow::getFollowingId)
                .toList();
        List<UUID> uniqueSuggestionList = removeDuplicateSuggestions(suggestionList);
         /*
            A->B->C
            (SUGGESTION_LIST contains all C)
         */
        uniqueSuggestionList.remove(userId);
        uniqueSuggestionList.removeAll(followList);
        return userProfileRepository.findAllById(uniqueSuggestionList);
    }
    private List<UUID> removeDuplicateSuggestions(List<UUID> suggestionList)
    {
        if(suggestionList.isEmpty()) return new ArrayList<>();
        int i = 0;
        int n = suggestionList.size();
        for(int j=0;j<n;j++)
        {
            if(!suggestionList.get(i).equals(suggestionList.get(j)))
            {
                suggestionList.set(i+1,suggestionList.get(j));
                i++;
            }
        }
        return suggestionList.stream()
                .limit(i+1)
                .toList();
    }
}
