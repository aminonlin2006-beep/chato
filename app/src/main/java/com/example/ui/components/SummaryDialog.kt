package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun SummaryDialog(
    summary: String,
    onDismiss: () -> Unit,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("summary_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPanel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "خلاصه‌ی گفتگو",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorAccent2,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "این خلاصه به حافظه‌ی سیستم اضافه شد و در تحلیل‌های بعدی استفاده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorText,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("خلاصه گفتگو", summary)
                            clipboard.setPrimaryClip(clip)
                            showToast("خلاصه در کلیپ‌بورد کپی شد")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_summary_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPanel2)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = ColorText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کپی", color = ColorText)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summary)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری خلاصه"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_summary_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPanel2)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = ColorText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اشتراک", color = ColorText)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_summary_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                    ) {
                        Text("بستن", color = Color.White)
                    }
                }
            }
        }
    }
}
