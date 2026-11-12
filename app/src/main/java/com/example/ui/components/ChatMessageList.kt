package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MessageNode
import com.example.ui.theme.*

@Composable
fun ChatMessageList(
    nodes: List<MessageNode>,
    isTyping: Boolean,
    onBranchFrom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(nodes.size, isTyping) {
        val totalCount = nodes.filter { it.role != "system" }.size + if (isTyping) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    val displayNodes = remember(nodes) {
        nodes.filter { it.role != "system" }
    }

    if (displayNodes.isEmpty() && !isTyping) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "برای شروع، پیامی بنویسید.",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorMuted,
                modifier = Modifier.testTag("empty_state_text")
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("messages_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayNodes, key = { it.id }) { node ->
            val isUser = node.role == "user"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // Bubble
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .background(
                            color = if (isUser) ColorUserBubble else ColorAsstBubble,
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isUser) 14.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 14.dp
                            )
                        )
                        .then(
                            if (!isUser) Modifier.border(
                                1.dp,
                                ColorBorder,
                                RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 14.dp)
                            )
                            else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                        .testTag(if (isUser) "user_message_${node.id}" else "asst_message_${node.id}")
                ) {
                    Text(
                        text = node.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorText,
                        lineHeight = 24.sp
                    )
                }

                // User message actions (e.g. Branch from here)
                if (isUser) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onBranchFrom(node.id) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("branch_from_btn_${node.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "شاخه از اینجا",
                                tint = ColorAccent2,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "شاخه از اینجا",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorAccent2
                            )
                        }
                    }
                }
            }
        }

        if (isTyping) {
            item {
                val infiniteTransition = rememberInfiniteTransition(label = "typing_anim")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ColorAsstBubble,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .alpha(alpha)
                            .testTag("typing_indicator")
                    ) {
                        Text(
                            text = "در حال فکر کردن…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorMuted,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
