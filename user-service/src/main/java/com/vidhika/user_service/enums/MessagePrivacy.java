package com.vidhika.user_service.enums;

/**
 * Defines who can directly message this user.
 * EVERYONE: Any registered user can message.
 * FOLLOWERS_ONLY: Only users that this user follows (or who follow this user) can send direct messages.
 */
public enum MessagePrivacy {
    EVERYONE,
    FOLLOWERS_ONLY
}

