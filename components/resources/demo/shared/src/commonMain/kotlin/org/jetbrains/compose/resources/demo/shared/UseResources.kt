package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    Column {
        Text("Hello, resources")
        Image(
            bitmap = resource("dir/img.png").rememberImageBitmap().orEmpty(),
            contentDescription = null,
        )
        Image(
            bitmap = resource("img.webp").rememberImageBitmap().orEmpty(),
            contentDescription = null,
        )
        Icon(
            imageVector = resource("vector.xml").rememberImageVector(LocalDensity.current).orEmpty(),
            modifier = Modifier.size(150.dp),
            contentDescription = null
        )
        Icon(
            painter = painterResource("dir/vector.xml"),
            modifier = Modifier.size(150.dp),
            contentDescription = null
        )
    }
}
