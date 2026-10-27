package com.vidhika.chat_service.repository;

import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByConversationId(String conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    List<Message> findByConversationIdAndStatusNotAndSenderIdNot(
            String conversationId,
            MessageStatus status,
            String senderId
    );

    void deleteByConversationId(String conversationId);
}
