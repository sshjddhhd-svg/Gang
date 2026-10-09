package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.model.Announcement
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.Comment
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.User
import com.example.data.repository.GangRepository
import com.example.ui.theme.GangThemeMode
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GangViewModel(
    private val repository: GangRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authManager.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authManager.currentUser)

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _themeMode = MutableStateFlow(GangThemeMode.MIDNIGHT_BLUE)
    val themeMode: StateFlow<GangThemeMode> = _themeMode.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _currentChat = MutableStateFlow<ChatChannel?>(null)
    val currentChat: StateFlow<ChatChannel?> = _currentChat.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedPostForComments = MutableStateFlow<Post?>(null)
    val selectedPostForComments: StateFlow<Post?> = _selectedPostForComments.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<User>>(emptyList())
    val adminUsers: StateFlow<List<User>> = _adminUsers.asStateFlow()

    private val _adminPosts = MutableStateFlow<List<Post>>(emptyList())
    val adminPosts: StateFlow<List<Post>> = _adminPosts.asStateFlow()

    private val _adminChats = MutableStateFlow<List<ChatChannel>>(emptyList())
    val adminChats: StateFlow<List<ChatChannel>> = _adminChats.asStateFlow()

    private val _adminMetrics = MutableStateFlow<Map<String, Long>>(emptyMap())
    val adminMetrics: StateFlow<Map<String, Long>> = _adminMetrics.asStateFlow()

    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var profileJob: Job? = null
    private var postsJob: Job? = null
    private var channelsJob: Job? = null
    private var announcementsJob: Job? = null
    private var notificationsJob: Job? = null
    private var messagesJob: Job? = null
    private var commentsJob: Job? = null

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    checkUserProfile(user.uid)
                    startListeningData(user.uid)
                } else {
                    stopListeningData()
                    _userProfile.value = null
                    _needsOnboarding.value = false
                }
            }
        }
    }

    fun setTheme(mode: GangThemeMode) {
        _themeMode.value = mode
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun isSuperAdmin(): Boolean {
        return repository.isSuperAdmin()
    }

    private fun checkUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile(uid)
                if (profile == null) {
                    _needsOnboarding.value = true
                } else {
                    _userProfile.value = profile
                    _needsOnboarding.value = false
                    profileJob?.cancel()
                    profileJob = launch {
                        repository.observeUserProfile(uid).collect { updated ->
                            _userProfile.value = updated
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    private fun startListeningData(uid: String) {
        viewModelScope.launch {
            repository.seedDefaultChannelsIfEmpty()
            loadAllRegisteredUsers()
        }

        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            repository.observePosts().collect { list ->
                _posts.value = list
            }
        }

        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            repository.observeChannels().collect { list ->
                _channels.value = list
            }
        }

        announcementsJob?.cancel()
        announcementsJob = viewModelScope.launch {
            repository.observeAnnouncements().collect { list ->
                _announcements.value = list
            }
        }

        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            var isInitial = true
            repository.observeNotifications(uid).collect { list ->
                if (!isInitial) {
                    val existingIds = _notifications.value.map { it.id }.toSet()
                    val newItems = list.filter { it.id !in existingIds }
                    for (item in newItems) {
                        repository.triggerSystemNotification(item.title, item.body)
                    }
                } else {
                    isInitial = false
                }
                _notifications.value = list
            }
        }
    }

    private fun stopListeningData() {
        profileJob?.cancel()
        postsJob?.cancel()
        channelsJob?.cancel()
        announcementsJob?.cancel()
        notificationsJob?.cancel()
        messagesJob?.cancel()
        commentsJob?.cancel()
        _posts.value = emptyList()
        _channels.value = emptyList()
        _announcements.value = emptyList()
        _notifications.value = emptyList()
        _chatMessages.value = emptyList()
    }

    fun loadAllRegisteredUsers() {
        viewModelScope.launch {
            try {
                _allUsers.value = repository.getAllRegisteredUsers()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun completeOnboarding(username: String, displayName: String, bio: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.registerUserProfile(
                    username = username,
                    displayName = displayName,
                    avatarUrl = "https://api.dicebear.com/7.x/bottts/png?seed=$username",
                    bio = bio
                )
                _userProfile.value = profile
                _needsOnboarding.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "فشل إنشاء الملف الشخصي"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateProfile(displayName, bio, avatarUrl)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "فشل تحديث الملف الشخصي"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // POSTS
    fun createPost(text: String, mediaUrls: List<String>) {
        if (text.isBlank() && mediaUrls.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createPost(text.trim(), mediaUrls)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "فشل نشر المنشور"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                repository.toggleLikePost(postId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun deletePost(postId: String, authorId: String) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId, authorId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun selectPostForComments(post: Post?) {
        _selectedPostForComments.value = post
        commentsJob?.cancel()
        if (post != null) {
            commentsJob = viewModelScope.launch {
                repository.observeComments(post.id).collect { list ->
                    _comments.value = list
                }
            }
        } else {
            _comments.value = emptyList()
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addComment(postId, text.trim())
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteComment(postId, commentId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    // CHATS & GROUPS & DMs
    fun createGroupChannel(name: String, description: String, avatarUrl: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val group = repository.createGroupChannel(name.trim(), description.trim(), avatarUrl.trim())
                openChat(group)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "فشل إنشاء المجموعة"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openChat(channel: ChatChannel) {
        _currentChat.value = channel
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(channel.id).collect { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun closeChat() {
        _currentChat.value = null
        messagesJob?.cancel()
        _chatMessages.value = emptyList()
    }

    fun sendMessage(
        chatId: String,
        text: String,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        if (text.isBlank() && imageUrl.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, text.trim(), imageUrl, replyToId, replyToText, replyToSender)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            try {
                repository.editMessage(chatId, messageId, newText.trim())
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(chatId, messageId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun toggleMessageReaction(chatId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                repository.toggleMessageReaction(chatId, messageId, emoji)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun startDirectMessage(targetUser: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dmChannel = repository.createOrGetDirectMessage(targetUser)
                openChat(dmChannel)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ABSOLUTE ADMIN CONTROL (sshjddhhd@gmail.com)
    fun loadAdminData() {
        if (!isSuperAdmin()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val users = repository.getAllUsers()
                val metrics = repository.getAppMetrics()
                val posts = repository.getAllPostsForAdmin()
                val chats = repository.getAllChatsForAdmin()
                _adminUsers.value = users
                _adminMetrics.value = metrics
                _adminPosts.value = posts
                _adminChats.value = chats
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(announcementId)
                loadAdminData()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun setBanStatus(userId: String, isBanned: Boolean) {
        viewModelScope.launch {
            try {
                repository.setBanStatus(userId, isBanned)
                loadAdminData()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun purgeUserPosts(userId: String) {
        viewModelScope.launch {
            try {
                repository.purgeUserPosts(userId)
                loadAdminData()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun deleteUserAccountAndPurge(userId: String, username: String) {
        viewModelScope.launch {
            try {
                repository.deleteUserAccountAndPurge(userId, username)
                loadAdminData()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun adminDeleteChannel(chatId: String) {
        viewModelScope.launch {
            try {
                repository.adminDeleteChannel(chatId)
                loadAdminData()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun broadcastAnnouncement(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createAnnouncement(title.trim(), message.trim())
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }
}
