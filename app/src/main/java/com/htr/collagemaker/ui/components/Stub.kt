package com.htr.collagemaker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType

/** Temporary placeholder for screens not built yet. */
@Composable
fun StubScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name, style = FlType.title, color = Fl.ink) }
}
