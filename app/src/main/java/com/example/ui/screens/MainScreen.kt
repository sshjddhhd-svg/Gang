package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.GangViewModel

enum class MainTab {
    FEED,
    CHATS,
    VOICE_AI,
    PROFILE,
    ADMIN
}

@Composable
fun MainScreen(
    viewModel: GangViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.FEED) }
    val isSuperAdmin = viewModel.isSuperAdmin()
    val needsOnboarding by viewModel.needsOnboarding.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()

    // Back navigation when inside an active chat
    BackHandler(enabled = currentChat != null) {
        viewModel.closeChat()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // 1. الرئيسية / Feed
                NavigationBarItem(
                    selected = selectedTab == MainTab.FEED,
                    onClick = { selectedTab = MainTab.FEED },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.FEED) Icons.Default.DynamicFeed else Icons.Outlined.DynamicFeed,
                            contentDescription = "الرئيسية"
                        )
                    },
                    label = { Text("الرئيسية") },
                    modifier = Modifier.testTag("nav_tab_feed")
                )

                // 2. المحادثات / Chats
                NavigationBarItem(
                    selected = selectedTab == MainTab.CHATS,
                    onClick = { selectedTab = MainTab.CHATS },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.CHATS) Icons.Default.Forum else Icons.Outlined.Forum,
                            contentDescription = "المحادثات"
                        )
                    },
                    label = { Text("المحادثات") },
                    modifier = Modifier.testTag("nav_tab_chats")
                )

                // 3. الصوت والذكاء / Voice AI
                NavigationBarItem(
                    selected = selectedTab == MainTab.VOICE_AI,
                    onClick = { selectedTab = MainTab.VOICE_AI },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.VOICE_AI) Icons.Default.GraphicEq else Icons.Outlined.GraphicEq,
                            contentDescription = "المساعد الصوتي"
                        )
                    },
                    label = { Text("صوت و AI") },
                    modifier = Modifier.testTag("nav_tab_voice")
                )

                // 4. الملف الشخصي / Profile
                NavigationBarItem(
                    selected = selectedTab == MainTab.PROFILE,
                    onClick = { selectedTab = MainTab.PROFILE },
                    icon = {
                        Icon(
                            if (selectedTab == MainTab.PROFILE) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "حسابي"
                        )
                    },
                    label = { Text("حسابي") },
                    modifier = Modifier.testTag("nav_tab_profile")
                )

                // 5. لوحة الإدارة / Admin (Only for sshjddhhd@gmail.com)
                if (isSuperAdmin) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.ADMIN,
                        onClick = { selectedTab = MainTab.ADMIN },
                        icon = {
                            Icon(
                                if (selectedTab == MainTab.ADMIN) Icons.Default.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                contentDescription = "الإدارة"
                            )
                        },
                        label = { Text("الإدارة") },
                        modifier = Modifier.testTag("nav_tab_admin")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.FEED -> FeedScreen(viewModel = viewModel)
                MainTab.CHATS -> ChatScreen(viewModel = viewModel)
                MainTab.VOICE_AI -> VoiceAssistantScreen()
                MainTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                MainTab.ADMIN -> {
                    if (isSuperAdmin) {
                        AdminPanelScreen(viewModel = viewModel)
                    } else {
                        selectedTab = MainTab.FEED
                    }
                }
            }

            // Onboarding Dialog overlay if newly registered user has no profile
            if (needsOnboarding) {
                OnboardingDialog(viewModel = viewModel)
            }
        }
    }
}
