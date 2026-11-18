package com.framelab.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.framelab.app.R

val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)
/** Template text — system serif (Noto Serif on Android), usually italic. */
val TemplateSerif = FontFamily.Serif

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(
    family: FontFamily, weight: Int, size: Int, line: Int? = null, tracking: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line?.sp ?: TextUnit.Unspecified,
    letterSpacing = tracking.sp,
    lineHeightStyle = trim,
)

/** FrameLab type styles (HANDOFF §3.3). */
object FlType {
    val wordmark = style(Bricolage, 800, 28, 30, -0.8f)
    val wordmarkSplash = style(Bricolage, 800, 46, 50, -1.6f)
    val display = style(Bricolage, 800, 36, 40, -1.1f)
    val screenTitle = style(Bricolage, 800, 30, 34, -0.8f)
    val sheetTitle = style(Bricolage, 800, 24, 28, -0.5f)
    val sheetTitleLarge = style(Bricolage, 800, 26, 30, -0.5f)
    val title = style(Bricolage, 700, 22, 28, -0.3f)
    val dialogTitle = style(Bricolage, 700, 20, 26)
    val section = style(Bricolage, 700, 19, 24, -0.3f)
    val navTitle = style(Inter, 700, 17, 22)
    val bodyL = style(Inter, 400, 16, 24)
    val body = style(Inter, 400, 15, 22)
    val bodyS = style(Inter, 400, 14, 20)
    val secondary = style(Inter, 400, 13, 18)
    val caption = style(Inter, 400, 12, 16)
    val label = style(Inter, 600, 11, 14)
    val buttonL = style(Inter, 700, 16, 20)
    val button = style(Inter, 600, 15, 20)
    val buttonAccent = style(Inter, 700, 15, 20)
    val chip = style(Inter, 600, 13, 18)
    val chipHome = style(Inter, 600, 14, 18)
    val tag = style(Inter, 700, 10, 18, 0.6f)
    val monoLabel = style(DmMono, 500, 11, 16, 1.0f)
    val monoSmall = style(DmMono, 500, 10, 14, 1.0f)
    val cardTitle = style(Inter, 600, 14, 18)
}

fun TextStyle.italic() = copy(fontStyle = FontStyle.Italic)
