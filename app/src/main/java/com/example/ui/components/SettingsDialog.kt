package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AdvisorConfig
import com.example.model.Specialty
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    initialConfig: AdvisorConfig,
    onSave: (AdvisorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var model by remember { mutableStateOf(initialConfig.model) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var temperature by remember { mutableFloatStateOf(initialConfig.temperature) }
    var autoProfile by remember { mutableStateOf(initialConfig.autoProfile) }
    var autoExportMin by remember { mutableStateOf(initialConfig.autoExportMin.toString()) }
    var dailyBudget by remember { mutableStateOf(initialConfig.dailyBudget.toString()) }

    val specialties = remember {
        mutableStateListOf<Specialty>().apply {
            addAll(initialConfig.specialties.map { Specialty(it.name, it.instruction) })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPanel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تنظیمات مدل و مشاور",
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
                    // API Base URL
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("آدرس پایه API") },
                        placeholder = { Text("https://api.openai.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cfg_base_input"),
                        singleLine = true,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Name
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("نام مدل") },
                        placeholder = { Text("gpt-4o-mini یا gemini-2.5-flash") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cfg_model_input"),
                        singleLine = true,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("کلید API") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cfg_key_input"),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "نمایش کلید",
                                    tint = ColorMuted
                                )
                            }
                        },
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Temperature Slider
                    Text(
                        text = "دما (خلاقیت): ${String.format("%.1f", temperature)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorText
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0f..1f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorAccent,
                            activeTrackColor = ColorAccent,
                            inactiveTrackColor = ColorPanel2
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Auto Profile Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoProfile,
                            onCheckedChange = { autoProfile = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ColorAccent,
                                uncheckedColor = ColorBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "به‌روزرسانی خودکار پروفایل بعد از هر پاسخ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorText
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto Export Minutes
                    OutlinedTextField(
                        value = autoExportMin,
                        onValueChange = { autoExportMin = it.filter { c -> c.isDigit() } },
                        label = { Text("خروجی خودکار پروفایل (دقیقه؛ ۰ = غیرفعال)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily Budget
                    OutlinedTextField(
                        value = dailyBudget,
                        onValueChange = { dailyBudget = it.filter { c -> c.isDigit() } },
                        label = { Text("سقف بودجه‌ی روزانه (توکن؛ ۰ = بدون محدودیت)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Extra Specialties Section
                    Text(
                        text = "تخصص‌های اضافی",
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorAccent2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "هر تخصص یک بلوک جداگانه به پیام سیستم اضافه می‌کند. می‌توانید هر تعداد که بخواهید بسازید؛ مرز و دستورالعمل هر تخصص را بنویسید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    specialties.forEachIndexed { index, specialty ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorPanel2)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = specialty.name,
                                        onValueChange = {
                                            specialties[index] = specialty.copy(name = it)
                                        },
                                        placeholder = { Text("نام تخصص") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = outlinedFieldColors()
                                    )
                                    IconButton(
                                        onClick = { specialties.removeAt(index) },
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف تخصص",
                                            tint = ColorDanger
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = specialty.instruction,
                                    onValueChange = {
                                        specialties[index] = specialty.copy(instruction = it)
                                    },
                                    placeholder = { Text("دستورالعمل و مرز این تخصص...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    colors = outlinedFieldColors()
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { specialties.add(Specialty("", "")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("add_specialty_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorAccent2),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن تخصص")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = initialConfig.copy(
                                baseUrl = baseUrl,
                                model = model,
                                apiKey = apiKey,
                                temperature = temperature,
                                autoProfile = autoProfile,
                                autoExportMin = autoExportMin.toIntOrNull() ?: 0,
                                dailyBudget = dailyBudget.toLongOrNull() ?: 0L,
                                specialties = specialties.filter { it.name.isNotBlank() }.toMutableList()
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
                    ) {
                        Text("ذخیره", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_settings_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                    ) {
                        Text("انصراف")
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ColorPanel2,
    unfocusedContainerColor = ColorPanel2,
    focusedBorderColor = ColorAccent,
    unfocusedBorderColor = ColorBorder,
    focusedTextColor = ColorText,
    unfocusedTextColor = ColorText,
    cursorColor = ColorAccent,
    focusedLabelColor = ColorAccent,
    unfocusedLabelColor = ColorMuted,
    focusedPlaceholderColor = ColorMuted,
    unfocusedPlaceholderColor = ColorMuted
)
