package com.framelab.app.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = Fl.accent, onPrimary = Fl.ink,
    secondary = Fl.ink, onSecondary = Fl.card,
    background = Fl.paper, onBackground = Fl.ink,
    surface = Fl.card, onSurface = Fl.ink,
    surfaceVariant = Fl.sunk, onSurfaceVariant = Fl.muted,
    outline = Fl.line, outlineVariant = Fl.lineStrong,
    error = Fl.danger, onError = Fl.card,
    scrim = Fl.scrim,
)

private val EditorScheme = darkColorScheme(
    primary = Fl.accent, onPrimary = Fl.ink,
    background = Fl.edBg, onBackground = Fl.edText,
    surface = Fl.edSurface, onSurface = Fl.edText,
    surfaceVariant = Fl.edRaised, onSurfaceVariant = Fl.edMuted,
    outline = Fl.edHandle, error = Fl.edDanger, scrim = Fl.scrimEditor,
)

private val FlTypography = Typography(
    displayLarge = FlType.display, headlineLarge = FlType.screenTitle, headlineMedium = FlType.sheetTitle,
    titleLarge = FlType.title, titleMedium = FlType.navTitle, bodyLarge = FlType.body,
    bodyMedium = FlType.bodyS, bodySmall = FlType.caption, labelLarge = FlType.button, labelMedium = FlType.chip,
    labelSmall = FlType.label,
)

/** Light (paper) theme used by Home, Explore, My Designs, Profile, onboarding. */
@Composable
fun FrameLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, typography = FlTypography) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(Fl.accent, Fl.accent.copy(alpha = 0.3f)),
            content = content,
        )
    }
}

/** The editor is always dark (HANDOFF §2). */
@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EditorScheme, typography = FlTypography, content = content)
}

/** Radii, sizes and spacing (HANDOFF §3.4). */
object FlDimens {
    val radiusThumbXs = 4.dp
    val radiusTag = 6.dp
    val radiusThumbS = 10.dp
    val radiusCard = 14.dp
    val radiusListCard = 16.dp
    val radiusGroup = 18.dp
    val radiusHero = 22.dp
    val radiusDialog = 24.dp
    val radiusSheetLight = 28.dp
    val screenMargin = 20.dp
    val formMargin = 24.dp
    val touch = 48.dp
    val buttonL = 52.dp
    val button = 48.dp
    val chip = 40.dp
    val chipSmall = 36.dp
    val tabBar = 64.dp
    val dock = 64.dp
    val editorTopBar = 56.dp
}

object FlMotion {
    const val EDITOR_PUSH = 260
    const val EDITOR_POP = 220
    const val SHEET = 280
    const val TOAST_MS = 4000L
    const val PRESS_SCALE = 0.97f
    const val PRESS_ALPHA = 0.82f
    const val PRESS_MS = 120
}
