package com.vitor.listatarefas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF8F9FB),
    surface = Color(0xFFF8F9FB),
    surfaceContainerLowest = Color.White,
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    background = Color(0xFF0F1115),
    surface = Color(0xFF0F1115),
    surfaceContainerLowest = Color(0xFF181B21),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = Color(0xFFF87171)
)

@Composable
fun ListaTarefasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}