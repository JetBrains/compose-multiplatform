package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.theme.typography
import org.jetbrains.compose.demo.widgets.ui.WidgetsType
import org.jetbrains.compose.demo.widgets.ui.utils.SubtitleText

@Composable
fun TextViews() {
    Column(Modifier.testTag(WidgetsType.TEXT_VIEWS.testTag)) {
        val textModifier = Modifier.padding(horizontal = 8.dp)

        SubtitleText(subtitle = "Font weights")
        Text(text = "Plain", modifier = textModifier)
        Text(
            text = "Medium Bold",
            style = typography.body1.copy(fontWeight = FontWeight.Medium),
            modifier = textModifier
        )
        Text(
            text = "Bold",
            style = typography.body1.copy(fontWeight = FontWeight.Bold),
            modifier = textModifier
        )
        Text(
            text = "Extra Bold",
            style = typography.body1.copy(fontWeight = FontWeight.Bold),
            modifier = textModifier
        )

        SubtitleText(subtitle = "Text decorations")
        Text(text = "Default", modifier = textModifier)
        Text(
            text = "Underline",
            textDecoration = TextDecoration.Underline,
            modifier = textModifier
        )
        Text(
            text = "LineThrough",
            textDecoration = TextDecoration.LineThrough,
            modifier = textModifier
        )
        Text(
            text = "UnderlineLineThrough",
            textDecoration = TextDecoration.combine(
                listOf(
                    TextDecoration.Underline,
                    TextDecoration.LineThrough
                )
            ),
            modifier = textModifier
        )

        SubtitleText(subtitle = "Overflow")
        Text(
            text = "Ellipsis: This text is supposed to ellipsis with max 1 line allowed for this",
            overflow = TextOverflow.Ellipsis,
            modifier = textModifier,
            maxLines = 1
        )
        Text(
            text = "Clip: This text is supposed to clip with max 1 line allowed for this",
            overflow = TextOverflow.Clip,
            modifier = textModifier,
            maxLines = 1
        )
    }

    /* TODO:  https://github.com/JetBrains/compose-jb/issues/106
    SubtitleText(subtitle = "font family dynamic")
    Row {
        Text(text = "Default", modifier = textModifier)
        Text(
            text = "Cursive",
            style = typography.body1.copy(fontFamily = FontFamily.Cursive),
            modifier = textModifier
        )
        Text(
            text = "SansSerif",
            style = typography.body1.copy(fontFamily = FontFamily.SansSerif),
            modifier = textModifier
        )
        Text(
            text = "Monospace",
            style = typography.body1.copy(fontFamily = FontFamily.Monospace),
            modifier = textModifier
        )
    } */
}