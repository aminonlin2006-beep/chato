package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

fun formatPersianNumber(number: Long): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val formatted = String.format("%,d", number)
    val sb = StringBuilder()
    for (ch in formatted) {
        if (ch in '0'..'9') {
            sb.append(persianDigits[ch - '0'])
        } else if (ch == ',') {
            sb.append('،')
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

@Composable
fun AdvisorTopBar(
    dayTokens: Long,
    dailyBudget: Long,
    onNewChat: () -> Unit,
    onSummarize: () -> Unit,
    onContinueInNewChat: () -> Unit,
    onToggleSidebar: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    isBusy: Boolean
) {
    val pct = if (dailyBudget > 0) dayTokens.toDouble() / dailyBudget else 0.0
    val tokenColor = when {
        dailyBudget > 0 && pct >= 1.0 -> ColorCrit
        dailyBudget > 0 && pct >= 0.8 -> ColorWarn
        else -> ColorMuted
    }

    Surface(
        color = ColorPanel,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advisor_top_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand name & Sidebar button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_sidebar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "شاخهها و گفتگوها",
                            tint = ColorAccent2
                        )
                    }

                    Text(
                        text = "مشاور",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorAccent2,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Token counter badge
                Row(
                    modifier = Modifier
                        .background(ColorPanel2, RoundedCornerShape(20.dp))
                        .border(1.dp, tokenColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("token_counter"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔢 ${formatPersianNumber(dayTokens)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokenColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Top action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNewChat,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_new_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "گفتگوی جدید",
                            tint = ColorText
                        )
                    }

                    IconButton(
                        onClick = onSummarize,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_summarize")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "خلاصه‌سازی دستی گفتگو",
                            tint = ColorAccent2
                        )
                    }

                    IconButton(
                        onClick = onContinueInNewChat,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_continue")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "ادامه در گفتگوی جدید",
                            tint = ColorText
                        )
                    }

                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "خروجی پروفایل",
                            tint = ColorText
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            tint = ColorText
                        )
                    }
                }
            }

            HorizontalDivider(color = ColorBorder, thickness = 1.dp)
        }
    }
}
