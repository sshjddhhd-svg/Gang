package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GangViewModel

@Composable
fun OnboardingDialog(
    viewModel: GangViewModel,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()
    val vmError by viewModel.errorMessage.collectAsState()

    Dialog(
        onDismissRequest = { /* Modal: cannot dismiss until registered */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "مرحباً بك في Gang!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "اختر اسم المستخدم الفريد (@username) لتثبيت هويتك في المجتمع.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.filter { char -> char.isLetterOrDigit() || char == '_' }
                    },
                    label = { Text("اسم المستخدم الفريد (@username)") },
                    placeholder = { Text("مثال: alex_rider") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = "اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_username_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم الظاهر للأعضاء") },
                    placeholder = { Text("مثال: أحمد الصياد") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "الاسم الظاهر") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("النبذة التعريفية (اختياري)") },
                    placeholder = { Text("أخبر أعضاء Gang عن اهتماماتك...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_bio_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                val errorText = localError ?: vmError
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val clean = username.trim().lowercase()
                        if (clean.length < 3) {
                            localError = "يجب أن يتكون اسم المستخدم من 3 أحرف إنجليزية على الأقل"
                            return@Button
                        }
                        localError = null
                        viewModel.completeOnboarding(
                            username = clean,
                            displayName = displayName.ifBlank { clean },
                            bio = bio
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && username.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("انضم إلى Gang الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
