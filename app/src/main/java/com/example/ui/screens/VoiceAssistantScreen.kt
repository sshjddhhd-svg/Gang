package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AiConversationMessage(
    val id: String = System.currentTimeMillis().toString(),
    val isUser: Boolean,
    val text: String,
    val isVoice: Boolean = false
)

@Composable
fun VoiceAssistantScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLiveActive by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AiConversationMessage(
                isUser = false,
                text = "مرحباً بك! أنا مساعد Gang الذكي المدعوم بنموذج Gemini 3.8. يمكنك التحدث معي بالصوت المباشر أو كتابة أي سؤال لمساعدتك في أفكار المنشورات وإدارة المجموعات."
            )
        )
    }

    val suggestedTopics = listOf(
        "🔥 فكرة منشور رائج لمجتمع Gang",
        "💡 تنشيط التفاعل في المجموعات",
        "⚡ صياغة إعلان سريع للأعضاء",
        "🎮 أفضل ألعاب جماعية لهذا الأسبوع"
    )

    // Pulsing animation for Live voice mode
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLiveActive) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مساعد Gang الصوتي الذكي",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "محادثات تفاعلية حية عبر Gemini 3.8 Live API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isLiveActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLiveActive) MaterialTheme.colorScheme.error else Color.Gray)
                    )
                    Text(
                        text = if (isLiveActive) "بث صوتي مباشر" else "وضع الاستعداد",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isLiveActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Voice Call Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isLiveActive) listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                ) else listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .clickable {
                            isLiveActive = !isLiveActive
                            if (isLiveActive) {
                                coroutineScope.launch {
                                    messages.add(
                                        AiConversationMessage(
                                            isUser = false,
                                            text = "🎙️ تم بدء الجلسة الصوتية المباشرة بنجاح! أنا أستمع إليك الآن، تفضل بالكلام...",
                                            isVoice = true
                                        )
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLiveActive) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "الميكروفون الصوتي",
                        tint = if (isLiveActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = if (isLiveActive) "جارٍ الاستماع في الوقت الفعلي... تحدث الآن" else "انقر على الميكروفون للتحدث الصوتي الحي مع المساعد",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isLiveActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Suggested Topics
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedTopics) { topic ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val userMsg = topic
                            messages.add(AiConversationMessage(isUser = true, text = userMsg))
                            isThinking = true
                            delay(1000)
                            val aiReply = when {
                                topic.contains("رائج") -> "🔥 فكرة منشور رائج في Gang: 'ما هو أقوى جهاز أو قطعة طورتها في سيت أب الألعاب الخاص بك هذا العام؟ شاركونا صور غرفكم وأجهزتكم 💻🎧'"
                                topic.contains("تنشيط") -> "💡 فكرة تنشيط التفاعل: نظم مسابقة أسبوعية كل يوم جمعة في الروم الصوتي للألعاب أو حدد موضوع نقاش يومي لأفضل فيلم أو كود تم إنجازه!"
                                topic.contains("إعلان") -> "⚡ مسودة إعلان مقترحة: 'تحية لأعضاء Gang الأعزاء! تم إطلاق قنوات دردشة ومجموعات جديدة كلياً. انضموا وشاركوا بصماتكم الآن!'"
                                else -> "🎮 أفضل الألعاب الجماعية المقترحة للعب مع طاقم Gang هذا الأسبوع: Helldivers 2, Valorant, و EA Sports FC 25!"
                            }
                            messages.add(AiConversationMessage(isUser = false, text = aiReply))
                            isThinking = false
                        }
                    }
                ) {
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Message stream
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isMe) "أنت" else "مساعد Gang AI",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Text(
                        text = "مساعد Gang يفكر في الإجابة...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Text prompt input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("اسأل مساعد Gang بالصوت أو النص...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_input_field"),
                shape = RoundedCornerShape(26.dp),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        coroutineScope.launch {
                            messages.add(AiConversationMessage(isUser = true, text = text))
                            isThinking = true
                            delay(1200)
                            val answer = "بناءً على طلبك: '$text' - تعتبر هذه فكرة ممتازة لمجتمع Gang! هل ترغب في أن أصيغ لك منشوراً كاملاً بها أو أرسل رسالة ترحيبية في الروم العام؟"
                            messages.add(AiConversationMessage(isUser = false, text = answer))
                            isThinking = false
                        }
                    }
                },
                modifier = Modifier.testTag("send_ai_button")
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
