package me.sample.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import me.sample.library.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyLibraryText(modifier: Modifier = Modifier, txt: String) {
    Column {
        Text(text = txt + stringResource(Res.string.str_1), modifier)
        Text(
            text = "\uF055\uF056\uF057\uF058\uF059\uF05A",
            fontFamily = FontFamily(Font(Res.font.font_awesome))
        )
    }
}

@Composable
fun MyLibraryIcon(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(Res.drawable.terminal),
        contentDescription = null
    )
}