package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.Comment
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.PrivateProfile
import com.example.data.model.User
import com.example.util.GangNotificationHelper
import com.example.util.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GangRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private var appContext: Context? = null

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    ) {
        this.appContext = context.applicationContext
        GangNotificationHelper.initChannel(context.applicationContext)
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    fun triggerSystemNotification(title: String, body: String) {
        appContext?.let { ctx ->
            GangNotificationHelper.showNotification(ctx, title, body)
        }
    }

    fun isSuperAdmin(): Boolean {
        val email = auth.currentUser?.email ?: return false
        return email.equals("sshjddhhd@gmail.com", ignoreCase = true)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    // -------------------------------------------------------------
    // USER PROFILE & ONBOARDING
    // -------------------------------------------------------------

    suspend fun getUserProfile(userId: String): User? {
        val snapshot = db.collection("users").document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(User::class.java) else null
    }

    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun registerUserProfile(
        username: String,
        displayName: String,
        avatarUrl: String,
        bio: String
    ): User {
        val uid = requireUserId()
        val email = auth.currentUser?.email ?: ""
        val cleanUsername = username.trim().lowercase().removePrefix("@")

        val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()
        if (usernameDoc.exists() && usernameDoc.getString("userId") != uid) {
            throw IllegalArgumentException("Username @$cleanUsername is already taken")
        }

        val user = User(
            userId = uid,
            username = cleanUsername,
            displayName = displayName.ifBlank { "عضو Gang" },
            avatarUrl = avatarUrl.ifBlank {
                "https://api.dicebear.com/7.x/bottts/png?seed=$cleanUsername"
            },
            bio = bio,
            followersCount = 0,
            followingCount = 0,
            postsCount = 0,
            isBanned = false,
            createdAt = System.currentTimeMillis()
        )

        val role = if (isSuperAdmin()) "admin" else "member"
        val privateProfile = PrivateProfile(email = email, role = role)

        val batch = db.batch()
        batch.set(db.collection("usernames").document(cleanUsername), mapOf("userId" to uid))
        batch.set(db.collection("users").document(uid), user)
        batch.set(db.collection("users").document(uid).collection("private").document("profile"), privateProfile)
        batch.commit().await()

        return user
    }

    suspend fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        val uid = requireUserId()
        val finalAvatar = if (appContext != null && (avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://"))) {
            ImageCompressor.uriToCompressedBase64(appContext!!, avatarUrl, 400, 80)
        } else avatarUrl

        db.collection("users").document(uid).update(
            mapOf(
                "displayName" to displayName,
                "bio" to bio,
                "avatarUrl" to finalAvatar
            )
        ).await()
    }

    suspend fun getAllRegisteredUsers(): List<User> {
        val snapshot = db.collection("users").limit(100).get().await()
        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
    }

    // -------------------------------------------------------------
    // NOTIFICATIONS
    // -------------------------------------------------------------

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(NotificationItem::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendNotificationToUser(
        targetUserId: String,
        type: String,
        title: String,
        body: String,
        avatarUrl: String = "",
        targetId: String = ""
    ) {
        if (targetUserId == currentUserId) return
        val doc = db.collection("users").document(targetUserId).collection("notifications").document()
        val notif = NotificationItem(
            id = doc.id,
            type = type,
            title = title,
            body = body,
            avatarUrl = avatarUrl,
            targetId = targetId,
            createdAt = System.currentTimeMillis(),
            read = false
        )
        doc.set(notif).await()

        // Also trigger system notification if running on device
        appContext?.let { ctx ->
            GangNotificationHelper.showNotification(ctx, title, body)
        }
    }

    // -------------------------------------------------------------
    // POSTS & FEED
    // -------------------------------------------------------------

    fun observePosts(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(text: String, mediaUrls: List<String>): Post {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val postId = db.collection("posts").document().id

        val processedMediaUrls = mediaUrls.map { url ->
            if (appContext != null && (url.startsWith("content://") || url.startsWith("file://"))) {
                ImageCompressor.uriToCompressedBase64(appContext!!, url, 800, 75)
            } else url
        }

        val post = Post(
            id = postId,
            authorId = uid,
            authorName = user?.displayName ?: "عضو Gang",
            authorUsername = user?.username ?: "member",
            authorAvatarUrl = user?.avatarUrl ?: "",
            text = text,
            mediaUrls = processedMediaUrls,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            likedBy = emptyList(),
            createdAt = System.currentTimeMillis()
        )

        val batch = db.batch()
        batch.set(db.collection("posts").document(postId), post)
        batch.update(db.collection("users").document(uid), "postsCount", FieldValue.increment(1))
        batch.commit().await()

        return post
    }

    suspend fun toggleLikePost(postId: String) {
        val uid = requireUserId()
        val myUser = getUserProfile(uid)
        val postRef = db.collection("posts").document(postId)
        var postAuthorId = ""
        var isNowLiked = false

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(Post::class.java) ?: return@runTransaction
            postAuthorId = post.authorId
            val liked = post.likedBy.contains(uid)
            isNowLiked = !liked
            val newLikedBy = if (liked) post.likedBy - uid else post.likedBy + uid
            val newLikesCount = (post.likesCount + if (liked) -1 else 1).coerceAtLeast(0)
            transaction.update(postRef, mapOf(
                "likedBy" to newLikedBy,
                "likesCount" to newLikesCount
            ))
        }.await()

        if (isNowLiked && postAuthorId.isNotBlank() && postAuthorId != uid) {
            sendNotificationToUser(
                targetUserId = postAuthorId,
                type = "like",
                title = "إعجاب جديد على منشورك ❤️",
                body = "قام ${myUser?.displayName ?: "عضو"} بالإعجاب بمنشورك",
                avatarUrl = myUser?.avatarUrl ?: "",
                targetId = postId
            )
        }
    }

    suspend fun deletePost(postId: String, authorId: String) {
        val uid = requireUserId()
        if (uid != authorId && !isSuperAdmin()) {
            throw IllegalAccessException("غير مصرح لك بحذف هذا المنشور")
        }
        val batch = db.batch()
        batch.delete(db.collection("posts").document(postId))
        batch.update(db.collection("users").document(authorId), "postsCount", FieldValue.increment(-1))
        batch.commit().await()
    }

    // Comments
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toObject(Comment::class.java) } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, text: String): Comment {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val commentRef = db.collection("posts").document(postId).collection("comments").document()

        val comment = Comment(
            id = commentRef.id,
            postId = postId,
            authorId = uid,
            authorName = user?.displayName ?: "عضو Gang",
            authorUsername = user?.username ?: "member",
            authorAvatarUrl = user?.avatarUrl ?: "",
            text = text,
            createdAt = System.currentTimeMillis()
        )

        val batch = db.batch()
        batch.set(commentRef, comment)
        batch.update(db.collection("posts").document(postId), "commentsCount", FieldValue.increment(1))
        batch.commit().await()

        // Fetch post author to notify
        val postSnap = db.collection("posts").document(postId).get().await()
        val authorId = postSnap.getString("authorId")
        if (!authorId.isNullOrBlank() && authorId != uid) {
            sendNotificationToUser(
                targetUserId = authorId,
                type = "comment",
                title = "تعليق جديد على منشورك 💬",
                body = "علق ${user?.displayName ?: "عضو"}: '$text'",
                avatarUrl = user?.avatarUrl ?: "",
                targetId = postId
            )
        }

        return comment
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        val uid = requireUserId()
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        val snap = commentRef.get().await()
        val authorId = snap.getString("authorId")
        if (authorId != uid && !isSuperAdmin()) {
            throw IllegalAccessException("غير مصرح لك بحذف هذا التعليق")
        }
        val batch = db.batch()
        batch.delete(commentRef)
        batch.update(db.collection("posts").document(postId), "commentsCount", FieldValue.increment(-1))
        batch.commit().await()
    }

    // -------------------------------------------------------------
    // CHATS & CHANNELS & DMs
    // -------------------------------------------------------------

    suspend fun seedDefaultChannelsIfEmpty() {
        val channels = db.collection("chats").limit(1).get().await()
        if (channels.isEmpty) {
            val defaults = listOf(
                ChatChannel("general", "channel", "# العام - General Gang", "المكان الرئيسي للدردشة وتجمع أعضاء Gang", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150", emptyList(), "أهلاً بك في مجتمع Gang!", System.currentTimeMillis()),
                ChatChannel("tech_gaming", "channel", "# الألعاب والتقنية", "نقاشات حول أحدث الألعاب، البرمجة وتجميعات الكمبيوتر", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=150", emptyList(), "انضم إلى الروم الآن!", System.currentTimeMillis()),
                ChatChannel("lounge", "channel", "# مقهى Gang Lounge", "مساحة للاسترخاء، الموسيقى، الميمز والحديث اليومي", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=150", emptyList(), "أجواء حماسية فقط", System.currentTimeMillis()),
                ChatChannel("announcements", "channel", "# الإعلانات الرسمية", "البيانات الرسمية وتحديثات التطبيق من الإدارة", "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=150", emptyList(), "مرحباً بكم في شبكة Gang!", System.currentTimeMillis())
            )
            val batch = db.batch()
            for (c in defaults) {
                batch.set(db.collection("chats").document(c.id), c)
            }
            batch.commit().await()
        }
    }

    fun observeChannels(): Flow<List<ChatChannel>> = callbackFlow {
        val listener = db.collection("chats")
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChatChannel::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createGroupChannel(name: String, description: String, avatarUrl: String): ChatChannel {
        requireUserId()
        val docRef = db.collection("chats").document()
        val finalAvatar = if (appContext != null && (avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://"))) {
            ImageCompressor.uriToCompressedBase64(appContext!!, avatarUrl, 400, 80)
        } else avatarUrl.ifBlank {
            "https://api.dicebear.com/7.x/identicon/png?seed=${docRef.id}"
        }

        val channel = ChatChannel(
            id = docRef.id,
            type = "channel",
            name = if (name.startsWith("#")) name else "# $name",
            description = description.ifBlank { "مجموعة مجتمع Gang" },
            avatarUrl = finalAvatar,
            participantIds = emptyList(),
            lastMessage = "تم إنشاء المجموعة بنجاح",
            lastMessageAt = System.currentTimeMillis()
        )
        docRef.set(channel).await()
        return channel
    }

    suspend fun createOrGetDirectMessage(targetUser: User): ChatChannel {
        val uid = requireUserId()
        val dmId = if (uid < targetUser.userId) "dm_${uid}_${targetUser.userId}" else "dm_${targetUser.userId}_${uid}"
        val existing = db.collection("chats").document(dmId).get().await()
        if (existing.exists()) {
            return existing.toObject(ChatChannel::class.java)!!
        }

        val channel = ChatChannel(
            id = dmId,
            type = "dm",
            name = "@${targetUser.username} (${targetUser.displayName})",
            description = "محادثة خاصة 1-on-1",
            avatarUrl = targetUser.avatarUrl,
            participantIds = listOf(uid, targetUser.userId),
            lastMessage = "بدأت محادثة خاصة جديدة",
            lastMessageAt = System.currentTimeMillis()
        )
        db.collection("chats").document(dmId).set(channel).await()
        return channel
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ): ChatMessage {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val msgRef = db.collection("chats").document(chatId).collection("messages").document()

        val finalImageUrl = if (appContext != null && imageUrl != null && (imageUrl.startsWith("content://") || imageUrl.startsWith("file://"))) {
            ImageCompressor.uriToCompressedBase64(appContext!!, imageUrl, 700, 75)
        } else imageUrl

        val message = ChatMessage(
            id = msgRef.id,
            senderId = uid,
            senderName = user?.displayName ?: "عضو Gang",
            senderAvatarUrl = user?.avatarUrl ?: "",
            text = text,
            imageUrl = finalImageUrl,
            createdAt = System.currentTimeMillis(),
            edited = false,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            reactions = emptyMap()
        )

        val lastSnippet = when {
            text.isNotBlank() -> text
            finalImageUrl != null -> "[صورة]"
            else -> "رسالة"
        }

        val batch = db.batch()
        batch.set(msgRef, message)
        batch.update(db.collection("chats").document(chatId), mapOf(
            "lastMessage" to "${user?.displayName ?: "عضو"}: $lastSnippet",
            "lastMessageAt" to System.currentTimeMillis()
        ))
        batch.commit().await()

        // If DM, notify the other participant
        val chatDoc = db.collection("chats").document(chatId).get().await()
        val participants = chatDoc.get("participantIds") as? List<*>
        if (participants != null) {
            val recipientId = participants.filterIsInstance<String>().firstOrNull { it != uid }
            if (recipientId != null) {
                sendNotificationToUser(
                    targetUserId = recipientId,
                    type = "message",
                    title = "رسالة خاصة من ${user?.displayName} 💬",
                    body = lastSnippet,
                    avatarUrl = user?.avatarUrl ?: "",
                    targetId = chatId
                )
            }
        }

        // If replying to someone else's message, notify that person
        if (!replyToId.isNullOrBlank()) {
            val origSnap = db.collection("chats").document(chatId).collection("messages").document(replyToId).get().await()
            val originalSenderId = origSnap.getString("senderId")
            if (!originalSenderId.isNullOrBlank() && originalSenderId != uid) {
                sendNotificationToUser(
                    targetUserId = originalSenderId,
                    type = "reply",
                    title = "رد جديد على رسالتك ↩️",
                    body = "قام ${user?.displayName ?: "عضو"} بالرد عليك: '$lastSnippet'",
                    avatarUrl = user?.avatarUrl ?: "",
                    targetId = chatId
                )
            }
        }

        return message
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        val snap = msgRef.get().await()
        val senderId = snap.getString("senderId")
        if (senderId != uid && !isSuperAdmin()) {
            throw IllegalAccessException("غير مصرح لك بحذف هذه الرسالة")
        }
        msgRef.delete().await()
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        val snapshot = msgRef.get().await()
        val senderId = snapshot.getString("senderId")
        if (senderId != uid && !isSuperAdmin()) {
            throw IllegalAccessException("لا يمكنك تعديل رسالة عضو آخر")
        }
        msgRef.update(mapOf(
            "text" to newText,
            "edited" to true
        )).await()
    }

    suspend fun toggleMessageReaction(chatId: String, messageId: String, emoji: String) {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(msgRef)
            val message = snapshot.toObject(ChatMessage::class.java) ?: return@runTransaction
            val currentReactions = message.reactions.toMutableMap()
            if (currentReactions[uid] == emoji) {
                currentReactions.remove(uid)
            } else {
                currentReactions[uid] = emoji
            }
            transaction.update(msgRef, "reactions", currentReactions)
        }.await()
    }

    // -------------------------------------------------------------
    // ANNOUNCEMENTS
    // -------------------------------------------------------------

    fun observeAnnouncements(): Flow<List<Announcement>> = callbackFlow {
        val listener = db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Announcement::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createAnnouncement(title: String, message: String): Announcement {
        if (!isSuperAdmin()) {
            throw IllegalAccessException("الإعلانات الرسمية محصورة بالمسؤول الأعلى sshjddhhd@gmail.com")
        }
        val doc = db.collection("announcements").document()
        val announcement = Announcement(
            id = doc.id,
            title = title,
            message = message,
            authorEmail = currentUserEmail ?: "sshjddhhd@gmail.com",
            createdAt = System.currentTimeMillis()
        )
        doc.set(announcement).await()

        // Trigger local notification
        appContext?.let { ctx ->
            GangNotificationHelper.showNotification(ctx, "📢 $title", message)
        }

        return announcement
    }

    // -------------------------------------------------------------
    // ABSOLUTE SUPER-ADMIN PRIVILEGES (sshjddhhd@gmail.com)
    // -------------------------------------------------------------

    suspend fun getAllUsers(): List<User> {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val snapshot = db.collection("users").get().await()
        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
    }

    suspend fun setBanStatus(userId: String, isBanned: Boolean) {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        db.collection("users").document(userId).update("isBanned", isBanned).await()
    }

    suspend fun purgeUserPosts(userId: String) {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val posts = db.collection("posts").whereEqualTo("authorId", userId).get().await()
        val batch = db.batch()
        for (doc in posts.documents) {
            batch.delete(doc.reference)
        }
        batch.update(db.collection("users").document(userId), "postsCount", 0)
        batch.commit().await()
    }

    suspend fun deleteUserAccountAndPurge(userId: String, username: String) {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val posts = db.collection("posts").whereEqualTo("authorId", userId).get().await()
        val batch = db.batch()
        for (doc in posts.documents) {
            batch.delete(doc.reference)
        }
        if (username.isNotBlank()) {
            batch.delete(db.collection("usernames").document(username.lowercase()))
        }
        batch.delete(db.collection("users").document(userId))
        batch.delete(db.collection("users").document(userId).collection("private").document("profile"))
        batch.commit().await()
    }

    suspend fun adminDeleteChannel(chatId: String) {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        // Delete messages in chat
        val msgs = db.collection("chats").document(chatId).collection("messages").get().await()
        val batch = db.batch()
        for (m in msgs.documents) {
            batch.delete(m.reference)
        }
        batch.delete(db.collection("chats").document(chatId))
        batch.commit().await()
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        db.collection("announcements").document(announcementId).delete().await()
    }

    suspend fun getAllPostsForAdmin(): List<Post> {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val snap = db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().await()
        return snap.documents.mapNotNull { it.toObject(Post::class.java) }
    }

    suspend fun getAllChatsForAdmin(): List<ChatChannel> {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val snap = db.collection("chats").orderBy("lastMessageAt", Query.Direction.DESCENDING).limit(100).get().await()
        return snap.documents.mapNotNull { it.toObject(ChatChannel::class.java) }
    }

    suspend fun getAppMetrics(): Map<String, Long> {
        if (!isSuperAdmin()) throw IllegalAccessException("مطلوب صلاحيات الإدارة العليا")
        val users = db.collection("users").get().await().size().toLong()
        val posts = db.collection("posts").get().await().size().toLong()
        val chats = db.collection("chats").get().await().size().toLong()
        val announcements = db.collection("announcements").get().await().size().toLong()
        return mapOf(
            "totalUsers" to users,
            "totalPosts" to posts,
            "totalChats" to chats,
            "totalAnnouncements" to announcements
        )
    }
}
