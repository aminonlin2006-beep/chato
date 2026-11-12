package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MessageNode
import com.example.model.ThreadItem
import com.example.ui.theme.*

@Composable
fun SidebarContent(
    threads: List<ThreadItem>,
    activeThreadId: String?,
    leaves: List<MessageNode>,
    activeLeafId: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectThread: (String) -> Unit,
    onDeleteThread: (ThreadItem) -> Unit,
    onSelectLeaf: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmedQuery = searchQuery.trim().lowercase()
    val filteredThreads = threads.reversed().filter { t ->
        if (trimmedQuery.isEmpty()) true
        else {
            t.title.lowercase().contains(trimmedQuery) ||
                    t.nodes.any { it.content.lowercase().contains(trimmedQuery) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ColorPanel)
            .padding(12.dp)
            .testTag("sidebar_content")
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("جست‌وجو در گفتگوها...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "جستجو",
                    tint = ColorMuted
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = outlinedFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "گفتگوها (${filteredThreads.size})",
            style = MaterialTheme.typography.titleSmall,
            color = ColorMuted,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Threads list
        LazyColumn(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredThreads, key = { it.id }) { thread ->
                val isActive = thread.id == activeThreadId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ColorPanel2,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (isActive) Modifier.border(1.5.dp, ColorAccent, RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                        )
                        .clickable { onSelectThread(thread.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("thread_item_${thread.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) ColorAccent2 else ColorText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onDeleteThread(thread) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_thread_btn_${thread.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف گفتگو",
                            tint = ColorMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Branches list
        Text(
            text = "شاخه‌های گفتگوی فعلی (${leaves.size})",
            style = MaterialTheme.typography.titleSmall,
            color = ColorMuted,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(leaves, key = { it.id }) { leaf ->
                val isActive = leaf.id == activeLeafId
                val label = leaf.content.take(38).ifBlank { "(بدون پیام)" }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ColorPanel2,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (isActive) Modifier.border(1.5.dp, ColorAccent, RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                        )
                        .clickable { onSelectLeaf(leaf.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("leaf_item_${leaf.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) ColorAccent2 else ColorMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
