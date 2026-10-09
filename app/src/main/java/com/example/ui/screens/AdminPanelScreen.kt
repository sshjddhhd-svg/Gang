package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.ChatChannel
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.viewmodel.GangViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPanelScreen(
    viewModel: GangViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.adminUsers.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val posts by viewModel.adminPosts.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val metrics by viewModel.adminMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedAdminTab by remember { mutableStateOf(0) } // 0: المستخدمون, 1: المنشورات, 2: المحادثات, 3: الإعلانات

    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var channelToDelete by remember { mutableStateOf<ChatChannel?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAdminData()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Super Admin Header
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "الإدارة",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "غرفة تحكم الإدارة العليا (Super-Admin)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "صلاحيات ومراقبة مطلقة: sshjddhhd@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // App-wide metrics card (English digits)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إحصائيات المنصة اللحظية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.loadAdminData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("المستخدمون", metrics["totalUsers"] ?: users.size.toLong())
                        MetricItem("المنشورات", metrics["totalPosts"] ?: posts.size.toLong())
                        MetricItem("المجموعات", metrics["totalChats"] ?: channels.size.toLong())
                        MetricItem("الإعلانات", metrics["totalAnnouncements"] ?: announcements.size.toLong())
                    }
                }
            }
        }

        // Navigation Tabs for Admin Sections
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedAdminTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedAdminTab == 0,
                    onClick = { selectedAdminTab = 0 },
                    text = { Text("المستخدمون (${users.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == 1,
                    onClick = { selectedAdminTab = 1 },
                    text = { Text("المنشورات (${posts.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == 2,
                    onClick = { selectedAdminTab = 2 },
                    text = { Text("المجموعات (${channels.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedAdminTab == 3,
                    onClick = { selectedAdminTab = 3 },
                    text = { Text("الإعلانات (${announcements.size})", fontWeight = FontWeight.Bold) }
                )
            }
        }

        // TAB 0: USERS MANAGEMENT
        if (selectedAdminTab == 0) {
            if (users.isEmpty()) {
                item {
                    Text(
                        text = "لا يوجد مستخدمون مسجلون حالياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(users, key = { it.userId }) { user ->
                AdminUserCard(
                    user = user,
                    onToggleBan = { isBanned -> viewModel.setBanStatus(user.userId, isBanned) },
                    onPurgePosts = { viewModel.purgeUserPosts(user.userId) },
                    onDeleteUser = { userToDelete = user }
                )
            }
        }

        // TAB 1: POSTS MANAGEMENT (Absolute control to view & delete any post)
        if (selectedAdminTab == 1) {
            if (posts.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد منشورات في المنصة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(posts, key = { it.id }) { post ->
                AdminPostCard(
                    post = post,
                    onDelete = { postToDelete = post }
                )
            }
        }

        // TAB 2: GROUPS & CHANNELS MANAGEMENT
        if (selectedAdminTab == 2) {
            if (channels.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد مجموعات أو غرف",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(channels, key = { it.id }) { ch ->
                AdminChannelCard(
                    channel = ch,
                    onDelete = { channelToDelete = ch }
                )
            }
        }

        // TAB 3: ANNOUNCEMENTS MANAGEMENT
        if (selectedAdminTab == 3) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "نشر إعلان رسمي لجميع الأعضاء",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedTextField(
                            value = announcementTitle,
                            onValueChange = { announcementTitle = it },
                            label = { Text("عنوان الإعلان الرسمي") },
                            placeholder = { Text("مثال: تحديث أمني جديد / فعاليات المجتمع") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = announcementMessage,
                            onValueChange = { announcementMessage = it },
                            label = { Text("نص الإعلان الرسمي") },
                            placeholder = { Text("اكتب تفاصيل الإعلان...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.broadcastAnnouncement(announcementTitle, announcementMessage)
                                announcementTitle = ""
                                announcementMessage = ""
                            },
                            enabled = announcementTitle.isNotBlank() && announcementMessage.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("broadcast_announcement_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نشر الإعلان", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(announcements, key = { it.id }) { ann ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ann.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ann.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { announcementToDelete = ann }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الإعلان", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog: Delete User
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("تأكيد حذف الحساب نهائياً") },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف حساب @${userToDelete!!.username} ومسح جميع منشوراته وبياناته نهائياً من قاعدة بيانات Firestore؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAccountAndPurge(userToDelete!!.userId, userToDelete!!.username)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف الحساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation dialog: Delete Channel
    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("تأكيد حذف المجموعة") },
            text = {
                Text("هل أنت متأكد من حذف المجموعة '${channelToDelete!!.name}' وكافة رسائلها نهائياً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteChannel(channelToDelete!!.id)
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف المجموعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation dialog: Delete Post
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("تأكيد حذف المنشور") },
            text = {
                Text("بصفتك المسؤول الأعلى، هل تريد حذف هذا المنشور التابع للمستخدم @${postToDelete!!.authorUsername} نهائياً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(postToDelete!!.id, postToDelete!!.authorId)
                        postToDelete = null
                        viewModel.loadAdminData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف المنشور")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation dialog: Delete Announcement
    if (announcementToDelete != null) {
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            title = { Text("تأكيد حذف الإعلان") },
            text = {
                Text("هل تريد حذف الإعلان '${announcementToDelete!!.title}'؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAnnouncement(announcementToDelete!!.id)
                        announcementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun MetricItem(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdminChannelCard(
    channel: ChatChannel,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                        if (channel.type == "dm") Icons.Default.Person else Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = channel.lastMessage.ifBlank { channel.description },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف المحادثة",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AdminPostCard(
    post: Post,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.authorAvatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${post.authorUsername}" },
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "@${post.authorUsername} • ${formatTimeEnglishAdmin(post.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف المنشور",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (post.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(post.mediaUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "❤️ ${post.likesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "💬 ${post.commentsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onToggleBan: (Boolean) -> Unit,
    onPurgePosts: () -> Unit,
    onDeleteUser: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = user.avatarUrl.ifBlank { "https://api.dicebear.com/7.x/bottts/png?seed=${user.username}" },
                    contentDescription = user.displayName,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (user.isBanned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "محظور",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${user.username} • ${user.postsCount} منشورات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onToggleBan(!user.isBanned) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (user.isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (user.isBanned) "إلغاء الحظر" else "حظر المستخدم")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onPurgePosts,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("مسح المنشورات")
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDeleteUser) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "حذف الحساب",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatTimeEnglishAdmin(timestamp: Long): String {
    return SimpleDateFormat("yyyy/MM/dd h:mm a", Locale.US).format(Date(timestamp))
}
