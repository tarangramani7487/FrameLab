package com.framelab.app.ui.theme

import androidx.compose.ui.graphics.Color

/** FrameLab colour tokens — names and values mirror res/values/colors.xml (HANDOFF §3.1–3.2). */
object Fl {
    // Light side
    val ink = Color(0xFF16130F)
    val paper = Color(0xFFFAF7F2)
    val card = Color(0xFFFFFFFF)
    val muted = Color(0xFF6F685E)
    val line = Color(0xFFECE6DC)
    val lineStrong = Color(0xFFD9D2C7)
    val sunk = Color(0xFFF1ECE4)
    val track = Color(0xFFE4DED4)
    val accent = Color(0xFFFF5A36)
    val accentInk = Color(0xFFB33A17)
    val accentSoft = Color(0xFFFFE6DE)
    val accentSoftText = Color(0xFFA8361A)
    val danger = Color(0xFFC8372D)
    val controlOff = Color(0xFF8F877B)
    val disabledText = Color(0xFFA39C91)

    /** Skeleton shimmer highlight (HANDOFF §5.17). */
    val shimmer = Color(0xFFF8F4EE)
    /** Selected row tint in Select drafts (HANDOFF §7.4). */
    val selectedRow = Color(0xFFFFEDE7)

    // Editor (dark)
    val edBg = Color(0xFF0B0B0C)
    val edSurface = Color(0xFF1C1C1E)
    val edRaised = Color(0xFF2C2C2E)
    val edText = Color(0xFFFFFFFF)
    val edMuted = Color(0xFF8E8E93)
    val edMutedRaised = Color(0xFFA1A1A6)
    val edDisabled = Color(0xFF5A5A5E)
    val edDots = Color(0xFF6E6E73)
    val edHandle = Color(0xFF48484A)
    val edDanger = Color(0xFFFF6961)

    // Translucent
    val edActiveTint = Color(0x1AFF5A36)
    val scrimLight = Color(0x7316130F)
    val scrim = Color(0x8016130F)
    val scrimEditor = Color(0x99000000)
    val peekDim = Color(0x990B0B0C)
    val badgeScrim = Color(0xBD16130F)
    val coachScrim = Color(0xBD000000) // black 74%

    // Template palette
    val tplKraft = Color(0xFFEFE6D6)
    val tplPaper = Color(0xFFFBF6EC)
    val tplLabPrint = Color(0xFFF4ECDC)
    val tplInk = Color(0xFF2B2621)
    val tplInkSoft = Color(0xFF4A4238)
    val tplLabel = Color(0xFF7A6E60)
    val tplDateStamp = Color(0xFFFF8A3D)
    val tplTape = Color(0xE6ECD6A3)
    val tplRule = Color(0x26786046)
    val tplMargin = Color(0x59D65C50)

    // Editor › Color swatches, in order
    val swatchWhite = Color(0xFFFFFFFF)
    val swatchPaper = Color(0xFFFAF7F2)
    val swatchCream = Color(0xFFF3EDE3)
    val swatchBlush = Color(0xFFFFE8EC)
    val swatchMist = Color(0xFFEAF1F4)
    val swatchSage = Color(0xFFE7EEDF)
    val swatchInk = Color(0xFF16130F)
    val swatchNavy = Color(0xFF0F1E2E)
    val swatches = listOf(swatchWhite, swatchPaper, swatchCream, swatchBlush, swatchMist, swatchSage, swatchInk, swatchNavy)
    val swatchNames = listOf("White", "Paper", "Cream", "Blush", "Mist", "Sage", "Ink", "Navy")

    // Pastel placeholders for empty spots in thumbnails
    val pastels = listOf(
        Color(0xFFF3CFC3), Color(0xFFCDE7EA), Color(0xFFE3E0F3),
        Color(0xFFFDE7C4), Color(0xFFD7EBDD), Color(0xFFF6D3DC),
    )
}
