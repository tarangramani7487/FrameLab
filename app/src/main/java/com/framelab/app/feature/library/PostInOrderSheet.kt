package com.framelab.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.model.Design
import com.framelab.app.render.SlideView
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlSheet
import com.framelab.app.ui.components.FlTextButton
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType

/**
 * N9 · "Post in this order" sheet. Opened by Share to Instagram on a saved post (N11) and the success screen (D3).
 * [images] are the saved content:// URIs in order; [design] renders the numbered strip.
 */
@Composable
fun PostInOrderSheet(visible: Boolean, design: Design?, images: List<String>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val n = design?.slides ?: images.size
    val noImgLead = stringResource(R.string.lib_toast_no_images)
    val noImgBody = stringResource(R.string.lib_toast_no_images_body)
    FlSheet(visible, onDismiss, paneTitle = stringResource(R.string.cd_post_in_this_order), horizontalPadding = 24.dp) {
        Spacer(Modifier.height(3.dp))
        Text(stringResource(R.string.s_post_in_this_order), style = FlType.sheetTitle, color = Fl.ink)
        Spacer(Modifier.height(16.dp))
        // Numbered strip 01–N.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(n) { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(50.dp, 63.dp).clip(RoundedCornerShape(4.dp)).background(Fl.sunk)) {
                        if (design != null) SlideView(design, i, Modifier.size(50.dp, 63.dp), keepRatio = false)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("%02d".format(i + 1), style = FlType.monoLabel.copy(letterSpacing = 0.sp, lineHeight = 14.sp), color = Fl.muted, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Step(1, stringResource(R.string.lib_inorder_step1, n))
            Step(2, stringResource(R.string.s_choose_a_feed_post_and_keep_the_order_as))
            Step(3, stringResource(R.string.s_add_a_caption_and_share_the_photos_line))
        }
        Spacer(Modifier.height(20.dp))
        FlButton(
            stringResource(R.string.s_open_instagram),
            {
                if (shareImages(ctx, images, instagram = true)) onDismiss() else toaster.show(noImgLead, noImgBody, R.drawable.ic_alert)
            },
            Modifier.fillMaxWidth(), large = true, leadingIcon = R.drawable.ic_send,
        )
        Spacer(Modifier.height(6.dp))
        FlTextButton(stringResource(R.string.s_not_now), onDismiss, Modifier.fillMaxWidth().height(44.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) {
            Text("$n", style = FlType.monoLabel.copy(letterSpacing = 0.sp, lineHeight = 24.sp), color = Fl.card)
        }
        Text(text, style = FlType.bodyS, color = Fl.ink, modifier = Modifier.weight(1f))
    }
}
