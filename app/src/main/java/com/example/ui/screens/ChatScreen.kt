package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.User
import com.example.ui.viewmodel.GangViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: GangViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showStartDmDialog by remember { mutableStateOf(false) }

    if (currentChat == null) {
        MessengerChannelListView(
            channels = channels,
            onSelectChannel = { viewModel.openChat(it) },
            onCreateGroupClick = { showCreateGroupDialog = true },
            onStartDmClick = {
                viewModel.loadAllRegisteredUsers()
                showStartDmDialog = true
            },
            modifier = modifier
        )
    } else {
        MessengerActiveChatView(
            channel = currentChat!!,
            messages = chatMessages,
            currentUserId = currentUser?.uid ?: "",
            isSuperAdmin = viewModel.isSuperAdmin(),
            onBack = { viewModel.closeChat() },
            onSendMessage = { text, imageUrl, replyId, replyText, replySender ->
                viewModel.sendMessage(currentChat!!.id, text, imageUrl, replyId, replyText, replySender)
            },
            onEditMessage = { msgId, newText ->
                viewModel.editMessage(currentChat!!.id, msgId, newText)
            },
            onDeleteMessage = { msgId ->
                viewModel.deleteMessage(currentChat!!.id, msgId)
            },
            onToggleReaction = { msgId, emoji ->
                viewModel.toggleMessageReaction(currentChat!!.id, msgId, emoji)
            },
            modifier = modifier
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { name, description, avatarUrl ->
                viewModel.createGroupChannel(name, description, avatarUrl)
                showCreateGroupDialog = false
            }
        )
    }

    if (showStartDmDialog) {
        StartDirectMessageDialog(
            users = allUsers.filter { it.userId != currentUser?.uid },
            onDismiss = { showStartDmDialog = false },
            onSelectUser = { targetUser ->
                viewModel.startDirectMessage(targetUser)
                showStartDmDialog = false
            }
        )
    }
}

@Composable
fun MessengerChannelListView(
    channels: List<ChatChannel>,
    onSelectChannel: (ChatChannel) -> Unit,
    onCreateGroupClick: () -> Unit,
    onStartDmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val publicChannels = channels.filter { it.type == "channel" }
    val directMessages = channels.filter { it.type == "dm" }

    var selectedFilter by remember { mutableStateOf(0) } // 0: الكل, 1: المجموعات, 2: الرسائل الخاصة

    val filteredList = when (selectedFilter) {
        1 -> publicChannels
        2 -> directMessages
        else -> channels
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الدردشات والمحادثات",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "غرف عامة ومحادثات خاصة 1-on-1 فورية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start DM button
                    FilledTonalButton(
                        onClick = onStartDmClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("start_dm_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("محادثة خاصة", fontWeight = FontWeight.Bold)
                    }

                    // Add Group Button
                    FilledTonalButton(
                        onClick = onCreateGroupClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("create_group_button")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مجموعة جديدة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter Tabs: الكل | المجموعات | الرسائل الخاصة
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("الكل (${channels.size})") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("المجموعات (${publicChannels.size})") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("الرسائل الخاصة (${directMessages.size})") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Active Groups & Rooms Bar (Stories-like Messenger heads)
        if (publicChannels.isNotEmpty() && selectedFilter != 2) {
            item {
                Text(
                    text = "غرف ومجموعات المجتمع",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(publicChannels) { ch ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(70.dp)
                                .clickable { onSelectChannel(ch) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ch.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ch.avatarUrl,
                                        contentDescription = ch.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ch.name.removePrefix("#").trim(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Channels List Header
        if (selectedFilter != 2 && publicChannels.isNotEmpty()) {
            item {
                Text(
                    text = "المجموعات والقنوات العامة",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(publicChannels, key = { it.id }) { channel ->
                MessengerChannelCard(channel = channel, onClick = { onSelectChannel(channel) })
            }
        }

        // Direct Messages Section
        if (selectedFilter != 1) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرسائل الخاصة المباشرة (DMs)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${directMessages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (directMessages.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "لا توجد محادثات خاصة بعد. ابدأ محادثة الآن!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onStartDmClick) {
                                Text("محادثة جديدة")
                            }
                        }
                    }
                }
            } else {
                items(directMessages, key = { it.id }) { dm ->
                    MessengerChannelCard(channel = dm, onClick = { onSelectChannel(dm) })
                }
            }
        }
    }
}

@Composable
fun MessengerChannelCard(
    channel: ChatChannel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("messenger_channel_${channel.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.avatarUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (channel.type == "channel") Icons.Default.Groups else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimeEnglish(channel.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.lastMessage.ifBlank { channel.description },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerActiveChatView(
    channel: ChatChannel,
    messages: List<ChatMessage>,
    currentUserId: String,
    isSuperAdmin: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (String, String?, String?, String?, String?) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = channel.avatarUrl,
                                contentDescription = channel.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "متصل الآن",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Call, contentDescription = "اتصال صوتي", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId
                MessengerMessageBubble(
                    message = msg,
                    isMe = isMe,
                    canDelete = isMe || isSuperAdmin,
                    onReply = { replyingToMessage = msg },
                    onEdit = { editingMessage = msg },
                    onDelete = { onDeleteMessage(msg.id) },
                    onReaction = { emoji -> onToggleReaction(msg.id, emoji) }
                )
            }
        }

        // Replying to preview
        if (replyingToMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = "رد",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الرد على ${replyingToMessage!!.senderName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = replyingToMessage!!.text.ifBlank { "[صورة]" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { replyingToMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء الرد", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Attached image preview before sending
        if (attachedImageUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "صورة مرفقة",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تم اختيار صورة للإرسال", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { attachedImageUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.Red)
                    }
                }
            }
        }

        // Messenger-style Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button to open Gallery photo picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("chat_gallery_button")
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "إرسال صورة من المعرض",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("اكتب رسالة...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(26.dp),
                    maxLines = 4
                )

                if (inputText.isBlank() && attachedImageUri == null) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "رسالة صوتية",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            val imgString = attachedImageUri?.toString()
                            onSendMessage(
                                inputText,
                                imgString,
                                replyingToMessage?.id,
                                replyingToMessage?.text,
                                replyingToMessage?.senderName
                            )
                            inputText = ""
                            attachedImageUri = null
                            replyingToMessage = null
                        },
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Edit message dialog
    if (editingMessage != null) {
        var editInput by remember { mutableStateOf(editingMessage!!.text) }
        Dialog(onDismissRequest = { editingMessage = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تعديل الرسالة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = editInput,
                        onValueChange = { editInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingMessage = null }) { Text("إلغاء") }
                        Button(onClick = {
                            onEditMessage(editingMessage!!.id, editInput)
                            editingMessage = null
                        }) { Text("حفظ التعديل") }
                    }
                }
            }
        }
    }
}

@Composable
fun MessengerMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    canDelete: Boolean = false,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    onReaction: (String) -> Unit
) {
    var showActionMenu by remember { mutableStateOf(false) }
    val emojiOptions = listOf("😆", "❤️", "😢", "👍", "🔥")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            if (!isMe) {
                AsyncImage(
                    model = message.senderAvatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${message.senderName}" },
                    contentDescription = message.senderName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .padding(bottom = 2.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                // Messenger-like curved bubble
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isMe) 18.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 18.dp
                            )
                        )
                        .background(
                            if (isMe) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        )
                        .clickable { showActionMenu = !showActionMenu }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("chat_bubble_${message.id}")
                ) {
                    Column {
                        // Quoted reply banner
                        if (message.replyToText != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMe) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = message.replyToSender ?: "اقتباس",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = message.replyToText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Image attachment in message
                        if (!message.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = "صورة في الرسالة",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 6.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Text content
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Meta row: edited status and time in English digits
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (message.edited) {
                                Text(
                                    text = "معدلة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = formatTimeEnglish(message.createdAt),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Emoji reaction pills
                if (message.reactions.isNotEmpty()) {
                    val grouped = message.reactions.values.groupingBy { it }.eachCount()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(grouped.entries.toList()) { (emoji, count) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp,
                                modifier = Modifier.clickable { onReaction(emoji) }
                            ) {
                                Text(
                                    text = "$emoji $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Popup menu
                if (showActionMenu) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        emojiOptions.forEach { emoji ->
                            Text(
                                text = emoji,
                                modifier = Modifier
                                    .clickable {
                                        onReaction(emoji)
                                        showActionMenu = false
                                    }
                                    .padding(4.dp),
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                onReply()
                                showActionMenu = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = "رد", modifier = Modifier.size(16.dp))
                        }
                        if (isMe) {
                            IconButton(
                                onClick = {
                                    onEdit()
                                    showActionMenu = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (canDelete) {
                            IconButton(
                                onClick = {
                                    onDelete()
                                    showActionMenu = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف الرسالة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog to select a member to start a 1-on-1 private DM
@Composable
fun StartDirectMessageDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بدء محادثة خاصة (DM)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن عضو بالاسم أو @username...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "لا يوجد أعضاء مطابقون للبحث",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(filtered, key = { it.userId }) { user ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.avatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${user.username}" },
                                    contentDescription = user.displayName,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "مراسلة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog to create custom group with custom photo & gallery picker
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (String, String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupAvatarUrl by remember { mutableStateOf("") }

    val groupPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            groupAvatarUrl = uri.toString()
        }
    }

    val presetGroupAvatars = listOf(
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=200",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=200",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
        "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=200"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "إنشاء مجموعة جديدة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Current selected avatar preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = groupAvatarUrl,
                                contentDescription = "صورة المجموعة",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "صورة المجموعة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = {
                                groupPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار من المعرض", fontSize = 12.sp)
                        }
                    }
                }

                // Preset avatars list
                Text("أو اختر من الصور المقترحة:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetGroupAvatars) { preset ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (groupAvatarUrl == preset) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { groupAvatarUrl = preset }
                        ) {
                            AsyncImage(
                                model = preset,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("اسم المجموعة") },
                    placeholder = { Text("مثال: رواد التقنية، محترفو الألعاب...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = groupDesc,
                    onValueChange = { groupDesc = it },
                    label = { Text("وصف المجموعة") },
                    placeholder = { Text("نبذة عن موضوع ونشاط المجموعة...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreateGroup(groupName, groupDesc, groupAvatarUrl) },
                        enabled = groupName.isNotBlank()
                    ) {
                        Text("إنشاء المجموعة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTimeEnglish(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
}
