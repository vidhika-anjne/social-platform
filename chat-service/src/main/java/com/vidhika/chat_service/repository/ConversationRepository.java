package com.vidhika.chat_service.repository;

import com.vidhika.chat_service.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByTeamId(String teamId);

    boolean existsByTeamId(String teamId);

    List<Conversation> findByIdIn(List<String> ids);
}
