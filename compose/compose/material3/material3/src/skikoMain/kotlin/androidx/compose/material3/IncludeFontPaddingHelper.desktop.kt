package androidx.compose.material3

import androidx.compose.ui.text.TextStyle

// TODO(b/237588251) remove this once the default includeFontPadding is false
/* NOOP includeFontPadding doesn't exist on desktop */
internal actual fun copyAndSetFontPadding(
    style: TextStyle,
    includeFontPadding: Boolean
): TextStyle = style
