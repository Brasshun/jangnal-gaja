package com.jangnal.gaja.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = JangnalOrange,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = JangnalYellowLight,
    onPrimaryContainer = JangnalBrown,
    
    secondary = JangnalBrown,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = JangnalYellow,
    onSecondaryContainer = JangnalBrown,

    background = BackgroundCream,
    onBackground = TextBrown,
    
    surface = SurfaceWhite,
    onSurface = TextBrown,
    surfaceVariant = JangnalYellowLight,
    onSurfaceVariant = TextBrown
)

@Composable
fun JangnalGajaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // 무시됨
    dynamicColor: Boolean = false, // 기본값 false로 변경하여 고유 테마 유지
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    // 무조건 Light Theme 사용
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // 상태바를 배경색과 동일하게
            // 상태바 아이콘을 어둡게 (배경이 밝으니까)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true 
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getResponsiveTypography(textScale),
        content = content
    )
}
