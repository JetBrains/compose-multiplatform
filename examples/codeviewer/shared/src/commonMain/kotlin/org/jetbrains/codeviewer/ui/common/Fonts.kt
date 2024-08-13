package org.jetbrains.codeviewer.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import codeviewer.shared.generated.resources.Res
import codeviewer.shared.generated.resources.*
import org.jetbrains.compose.resources.Font

object Fonts {
    @Composable
    fun jetbrainsMono() = FontFamily(
        Font(
            Res.font.jetbrainsmono_regular,
            FontWeight.Normal,
            FontStyle.Normal
        ),
        Font(
            Res.font.jetbrainsmono_italic,
            FontWeight.Normal,
            FontStyle.Italic
        ),

        Font(
            Res.font.jetbrainsmono_bold,
            FontWeight.Bold,
            FontStyle.Normal
        ),
        Font(
            Res.font.jetbrainsmono_bold_italic,
            FontWeight.Bold,
            FontStyle.Italic
        ),

        Font(
            Res.font.jetbrainsmono_extrabold,
            FontWeight.ExtraBold,
            FontStyle.Normal
        ),
        Font(
            Res.font.jetbrainsmono_extrabold_italic,
            FontWeight.ExtraBold,
            FontStyle.Italic
        ),

        Font(
            Res.font.jetbrainsmono_medium,
            FontWeight.Medium,
            FontStyle.Normal
        ),
        Font(
            Res.font.jetbrainsmono_medium_italic,
            FontWeight.Medium,
            FontStyle.Italic
        )
    )
}