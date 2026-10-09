package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.Comment
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.viewmodel.GangViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: GangViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSuperAdmin = viewModel.isSuperAdmin()
    val selectedPostForComments by viewModel.selectedPostForComments.collectAsState()
    val comments by viewModel.comments.collectAsState()

    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var selectedAuthorToDm by remember { mutableStateOf<User?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreatePostSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إنشاء منشور")
                Spacer(modifier = Modifier.width(6.dp))
                Text("منشور جديد", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / App Title & Notifications bell
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "منشورات Gang",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "تفاعل مع أحدث أخبار وتحديثات المجتمع",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notifications button with badge
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("notifications_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge { Text("${notifications.size}") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "الإشعارات",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Global Announcements Banner
            if (announcements.isNotEmpty()) {
                item {
                    AnnouncementsCarousel(announcements)
                }
            }

            // Empty State
            if (posts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "لا توجد منشورات حتى الآن!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "كن أول من يشارك فكرة أو صورة مع مجتمع Gang.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Posts Feed
            items(posts, key = { it.id }) { post ->
                val currentUid = currentUser?.uid
                val isLiked = currentUid != null && post.likedBy.contains(currentUid)
                val canDelete = currentUid == post.authorId || isSuperAdmin

                PostCard(
                    post = post,
                    isLiked = isLiked,
                    canDelete = canDelete,
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onCommentClick = { viewModel.selectPostForComments(post) },
                    onDeleteClick = { viewModel.deletePost(post.id, post.authorId) },
                    onAuthorClick = {
                        if (post.authorId != currentUid) {
                            selectedAuthorToDm = User(
                                userId = post.authorId,
                                username = post.authorUsername,
                                displayName = post.authorName,
                                avatarUrl = post.authorAvatarUrl
                            )
                        }
                    }
                )
            }
        }
    }

    if (showCreatePostSheet) {
        CreatePostDialog(
            onDismiss = { showCreatePostSheet = false },
            onSubmit = { text, mediaUrls ->
                viewModel.createPost(text, mediaUrls)
                showCreatePostSheet = false
            }
        )
    }

    if (selectedPostForComments != null) {
        CommentsDialog(
            post = selectedPostForComments!!,
            comments = comments,
            currentUid = currentUser?.uid,
            isSuperAdmin = isSuperAdmin,
            onDismiss = { viewModel.selectPostForComments(null) },
            onAddComment = { text -> viewModel.addComment(selectedPostForComments!!.id, text) },
            onDeleteComment = { commentId -> viewModel.deleteComment(selectedPostForComments!!.id, commentId) }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    // Direct Message action from author click
    if (selectedAuthorToDm != null) {
        AuthorProfileDialog(
            user = selectedAuthorToDm!!,
            onDismiss = { selectedAuthorToDm = null },
            onStartDm = {
                viewModel.startDirectMessage(selectedAuthorToDm!!)
                selectedAuthorToDm = null
            }
        )
    }
}

@Composable
fun AnnouncementsCarousel(announcements: List<Announcement>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(announcements, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.width(320.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = "إعلان رسمي",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    canDelete: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.authorAvatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${post.authorUsername}" },
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@${post.authorUsername} • ${formatArabicTimeWithEnglishDigits(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف المنشور",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Attached Media images
            if (post.mediaUrls.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(post.mediaUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "صورة المنشور",
                            modifier = Modifier
                                .height(210.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action row: Like, Comment, Share with English numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                TextButton(
                    onClick = onLikeClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "إعجاب",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount} إعجاب",
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Comment
                TextButton(
                    onClick = onCommentClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "تعليق",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.commentsCount} تعليق")
                }

                // Share
                TextButton(
                    onClick = {},
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "مشاركة",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.sharesCount} مشاركة")
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    val imageUrls = remember { mutableStateListOf<String>() }

    // Multi-photo picker from user's local gallery
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            if (!imageUrls.contains(uri.toString())) {
                imageUrls.add(uri.toString())
            }
        }
    }

    val presets = listOf(
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=600",
        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "إنشاء منشور جديد",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("ما الذي يجري في ذهنك؟") },
                    placeholder = { Text("شارك تحديثك، فكرتك أو صورك مع مجتمع Gang...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("create_post_text_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                // Pick from Device Gallery button
                Button(
                    onClick = {
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختيار صور من معرض هاتفك", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("أو رابط صورة من الإنترنت") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            if (imageUrlInput.isNotBlank()) {
                                imageUrls.add(imageUrlInput.trim())
                                imageUrlInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "إضافة صورة", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = "أو اختر صوراً جاهزة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        AsyncImage(
                            model = preset,
                            contentDescription = "صورة مقترحة",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!imageUrls.contains(preset)) imageUrls.add(preset)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (imageUrls.isNotEmpty()) {
                    Text("الصور المرفقة (${imageUrls.size}):", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUrls) { url ->
                            Box {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUrls.remove(url) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف الصورة", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(text, imageUrls.toList()) },
                        enabled = text.isNotBlank() || imageUrls.isNotEmpty(),
                        modifier = Modifier.testTag("publish_post_button")
                    ) {
                        Text("نشر في Gang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsDialog(
    post: Post,
    comments: List<Comment>,
    currentUid: String? = null,
    isSuperAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit = {}
) {
    var commentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التعليقات (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد تعليقات بعد. كن أول من يترك رداً!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(comments, key = { it.id }) { c ->
                        val canDeleteComment = c.authorId == currentUid || isSuperAdmin
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = c.authorAvatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${c.authorUsername}" },
                                contentDescription = c.authorName,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = c.authorName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = c.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (canDeleteComment) {
                                IconButton(
                                    onClick = { onDeleteComment(c.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف التعليق",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("اكتب تعليقاً...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field"),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier.testTag("submit_comment_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "إرسال",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Dialog for In-App Notifications
@Composable
fun NotificationsDialog(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الإشعارات والتنبيهات (${notifications.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (notifications.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد إشعارات جديدة حالياً",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (notif.type) {
                                    "like" -> Icons.Default.Favorite
                                    "comment" -> Icons.Default.Comment
                                    "message" -> Icons.Default.Chat
                                    else -> Icons.Default.Campaign
                                }
                                val iconColor = when (notif.type) {
                                    "like" -> MaterialTheme.colorScheme.error
                                    "comment" -> MaterialTheme.colorScheme.primary
                                    "message" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.tertiary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatArabicTimeWithEnglishDigits(notif.createdAt),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog to view author profile and send DM
@Composable
fun AuthorProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onStartDm: () -> Unit
) {
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = user.avatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${user.username}" },
                    contentDescription = user.displayName,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onStartDm,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مراسلة في الخاص", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatArabicTimeWithEnglishDigits(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "الآن"
        diff < 3600_000 -> "منذ ${diff / 60_000} دقيقة"
        diff < 86400_000 -> "منذ ${diff / 3600_000} ساعة"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))
    }
}
