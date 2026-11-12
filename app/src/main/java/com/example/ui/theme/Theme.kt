package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal)
)

val ColorBg = Color(0xFF0F1115)
val ColorPanel = Color(0xFF171A21)
val ColorPanel2 = Color(0xFF1F232D)
val ColorBorder = Color(0xFF2A2F3A)
val ColorText = Color(0xFFE8EAF0)
val ColorMuted = Color(0xFF9AA3B2)
val ColorAccent = Color(0xFF6C8CFF)
val ColorAccent2 = Color(0xFF8BA4FF)
val ColorUserBubble = Color(0xFF2B3A5E)
val ColorAsstBubble = Color(0xFF1F232D)
val ColorDanger = Color(0xFFFF6B6B)
val ColorWarn = Color(0xFFB8860B)
val ColorCrit = Color(0xFFC0392B)

val MoshaverColorScheme = darkColorScheme(
    primary = ColorAccent,
    onPrimary = Color.White,
    primaryContainer = ColorUserBubble,
    onPrimaryContainer = ColorText,
    secondary = ColorAccent2,
    onSecondary = Color.Black,
    background = ColorBg,
    onBackground = ColorText,
    surface = ColorPanel,
    onSurface = ColorText,
    surfaceVariant = ColorPanel2,
    onSurfaceVariant = ColorMuted,
    outline = ColorBorder,
    error = ColorDanger,
    onError = Color.White
)

val MoshaverTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = ColorText
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = ColorText
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = ColorText
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = ColorText
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 22.sp,
        color = ColorText
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = ColorMuted
    ),
    labelLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ColorText
    )
)

@Composable
fun MoshaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoshaverColorScheme,
        typography = MoshaverTypography,
        content = content
    )
}
