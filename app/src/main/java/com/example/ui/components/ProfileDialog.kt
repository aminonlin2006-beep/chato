package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AdvisorMemory
import com.example.model.ExportVersion
import com.example.ui.theme.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileDialog(
    memory: AdvisorMemory,
    onSaveProfile: (golden: List<String>, recent: List<String>) -> Unit,
    onManualExport: () -> Unit,
    onDismiss: () -> Unit,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    var goldenText by remember {
        mutableStateOf(memory.golden.joinToString("\n") { it.text })
    }
    var recentText by remember {
        mutableStateOf(memory.recent.joinToString("\n") { it.text })
    }

    fun buildCurrentExport(): String {
        val lines = mutableListOf<String>()
        lines.add("شناخت کاربر (خروجی قابل حمل)")
        lines.add("====================")
        val g = goldenText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (g.isNotEmpty()) {
            lines.add("واقعیتهای پایدار:")
            g.forEach { lines.add("- $it") }
        }
        val r = recentText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (r.isNotEmpty()) {
            lines.add("مشاهدات اخیر:")
            r.forEach { lines.add("- $it") }
        }
        if (memory.summary.isNotEmpty()) {
            lines.add("خلاصهی گفتگوهای قبلی:")
            memory.summary.forEach { lines.add("- $it") }
        }
        return lines.joinToString("\n")
    }

    var previewText by remember { mutableStateOf(buildCurrentExport()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("profile_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPanel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "پروفایل و شناخت کاربر",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Golden facts
                    Text(
                        text = "واقعیت‌های پایدار (یکی در هر خط)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorMuted
                    )
                    OutlinedTextField(
                        value = goldenText,
                        onValueChange = {
                            goldenText = it
                            previewText = buildCurrentExport()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("prof_golden_input"),
                        maxLines = 6,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recent observations
                    Text(
                        text = "مشاهدات اخیر (یکی در هر خط)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorMuted
                    )
                    OutlinedTextField(
                        value = recentText,
                        onValueChange = {
                            recentText = it
                            previewText = buildCurrentExport()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("prof_recent_input"),
                        maxLines = 7,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Portable preview
                    Text(
                        text = "پیش‌نمایش خروجی قابل حمل",
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorAccent2,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = previewText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("prof_preview_text"),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timed export versions
                    Text(
                        text = "نسخه‌های زمان‌دار (${memory.exports.size} نسخه)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorAccent2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نسخه‌های ذخیره‌شده‌ی پروفایل. روی هرکدام بزنید تا در پیش‌نمایش بالا نمایش داده شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    if (memory.exports.isEmpty()) {
                        Text(
                            text = "هنوز نسخه‌ی زمان‌داری ذخیره نشده است. می‌توانید با دکمه‌ی زیر نسخه ذخیره کنید یا خروجی خودکار را در تنظیمات فعال کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorMuted,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        memory.exports.reversed().forEachIndexed { index, ex ->
                            val num = memory.exports.size - index
                            val dateStr = dateFormat.format(Date(ex.ts))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        previewText = ex.text
                                        showToast("نسخه #$num در پیش‌نمایش نمایش داده شد")
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = ColorPanel2)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#$num — $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorText
                                    )
                                    Text(
                                        text = "${ex.text.length} کاراکتر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorAccent2
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onManualExport()
                            previewText = buildCurrentExport()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorAccent2),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                    ) {
                        Text("+ ثبت نسخه‌ی زمان‌دار جدید")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val g = goldenText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                val r = recentText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                onSaveProfile(g, r)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_profile_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                        ) {
                            Text("ذخیره ویرایش‌ها", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("پروفایل کاربر", previewText)
                                clipboard.setPrimaryClip(clip)
                                showToast("متن پروفایل کپی شد.")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_profile_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPanel2)
                        ) {
                            Text("کپی متن", color = ColorText)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, previewText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری پروفایل"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                        ) {
                            Text("اشتراک‌گذاری متن")
                        }

                        OutlinedButton(
                            onClick = {
                                val jsonStr = Json { prettyPrint = true }.encodeToString(
                                    mapOf(
                                        "golden" to memory.golden.map { it.text },
                                        "recent" to memory.recent.map { it.text },
                                        "summary" to memory.summary
                                    )
                                )
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, jsonStr)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری JSON"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                        ) {
                            Text("اشتراک JSON")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorMuted),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                    ) {
                        Text("بستن")
                    }
                }
            }
        }
    }
}
