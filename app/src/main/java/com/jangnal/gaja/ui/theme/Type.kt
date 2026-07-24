package com.jangnal.gaja.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
// function to generate typography based on scale
fun getResponsiveTypography(scale: Float = 1.0f): Typography {
    return androidx.compose.material3.Typography(
        headlineMedium = TextStyle(
             fontFamily = FontFamily.Default,
             fontWeight = FontWeight.Bold,
             fontSize = 28.sp * scale,
             lineHeight = 36.sp * scale
        ),
        titleMedium = TextStyle(
             fontFamily = FontFamily.Default,
             fontWeight = FontWeight.Bold,
             fontSize = 18.sp * scale,
             lineHeight = 24.sp * scale,
             letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp * scale,
            lineHeight = 24.sp * scale,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
             fontFamily = FontFamily.Default,
             fontWeight = FontWeight.Normal,
             fontSize = 14.sp * scale,
             lineHeight = 20.sp * scale,
             letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
             fontFamily = FontFamily.Default,
             fontWeight = FontWeight.Normal,
             fontSize = 12.sp * scale,
             lineHeight = 16.sp * scale,
             letterSpacing = 0.4.sp
        ),
        labelMedium = TextStyle(
             fontFamily = FontFamily.Default,
             fontWeight = FontWeight.Medium,
             fontSize = 12.sp * scale,
             lineHeight = 16.sp * scale,
             letterSpacing = 0.5.sp
        )
    )
}

// Default Typography
val Typography = getResponsiveTypography(1.0f)
