package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.ui.WidgetsType
import org.jetbrains.compose.demo.widgets.ui.utils.SubtitleText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import widgets_gallery.shared.generated.resources.Res
import widgets_gallery.shared.generated.resources.p2
import widgets_gallery.shared.generated.resources.p6

@OptIn(ExperimentalResourceApi::class)
@Composable
fun Chips() {
    Column(Modifier.testTag(WidgetsType.CHIPS.testTag)) {
        // There is no in-built chips but you can make yours like below
        SubtitleText(subtitle = "Custom chips with surface")
        Column(modifier = Modifier.padding(8.dp)) {
            YoutubeChip(
                selected = true,
                text = "Chip",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            YoutubeChip(
                selected = false,
                text = "Inactive",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CustomImageChip(
                text = "custom",
                imageId = Res.drawable.p2,
                selected = true
            )
            Spacer(modifier = Modifier.padding(8.dp))
            CustomImageChip(
                text = "custom2",
                imageId = Res.drawable.p6,
                selected = false
            )
        }
        SubtitleText(subtitle = "Buttons with circle clipping.")
        Column(modifier = Modifier.padding(8.dp)) {
            Button(
                onClick = {},
                modifier = Modifier.padding(8.dp).clip(CircleShape)
            ) {
                Text(text = "Chip button")
            }
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.padding(8.dp).clip(CircleShape)
            ) {
                Text(text = "Disabled chip")
            }
        }
    }
}


//Inspired from jetcaster sample. I hope compose can add simple Chip UI element that can
// support images or icons with multiple states.
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun CustomImageChip(
    text: String,
    imageId: DrawableResource,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.primary
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> MaterialTheme.colors.onPrimary
            else -> Color.LightGray
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> MaterialTheme.colors.primary
                else -> Color.LightGray
            }
        ),
        modifier = modifier
    ) {
        Row(modifier = Modifier) {
            Image(
                painterResource(imageId),
                contentDescription = null,
                modifier = Modifier.padding(8.dp).requiredSize(20.dp).clip(CircleShape)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun YoutubeChip(selected: Boolean, text: String, modifier: Modifier = Modifier) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.onSurface
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> MaterialTheme.colors.onPrimary
            else -> Color.LightGray
        },
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> MaterialTheme.colors.primary
                else -> Color.LightGray
            }
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(8.dp)
        )

    }
}