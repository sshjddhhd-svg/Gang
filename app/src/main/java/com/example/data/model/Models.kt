package com.example.data.model

data class User(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val postsCount: Long = 0,
    val isBanned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PrivateProfile(
    val email: String = "",
    val role: String = "member"
)

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val sharesCount: Long = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatChannel(
    val id: String = "",
    val type: String = "channel", // "channel" or "dm"
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val edited: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, String> = emptyMap() // userId to emoji
)

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String = "",
    val type: String = "general", // "like", "comment", "message", "announcement"
    val title: String = "",
    val body: String = "",
    val avatarUrl: String = "",
    val targetId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
