package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.theme.purple
import org.jetbrains.compose.demo.widgets.theme.purple200
import org.jetbrains.compose.demo.widgets.theme.typography
import org.jetbrains.compose.demo.widgets.ui.WidgetsType

@Composable
fun Buttons() {
    Column(Modifier.testTag(WidgetsType.BUTTONS.testTag)) {
        Button(onClick = {}, modifier = Modifier.padding(8.dp)) {
            Text(text = "Main Button")
        }
        TextButton(onClick = {}, modifier = Modifier.padding(8.dp)) {
            Text(text = "Text Button")
        }
        TextButton(onClick = {}, modifier = Modifier.padding(8.dp), enabled = false) {
            Text(text = "Text Disabled")
        }
        Button(onClick = {}, modifier = Modifier.padding(8.dp), enabled = false) {
            Text(text = "Disabled")
        }
        Button(
            onClick = {},
            modifier = Modifier.padding(8.dp),
            elevation = ButtonDefaults.elevation()
        ) {
            Text(text = "Flat")
        }
        Button(
            onClick = {},
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Rounded")
        }
        OutlinedButton(onClick = {}, modifier = Modifier.padding(8.dp)) {
            Text(text = "Outline")
        }
        Button(onClick = {}, modifier = Modifier.padding(8.dp)) {
            Row {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(text = "Icon Button")
            }
        }
        Button(onClick = {}, modifier = Modifier.padding(8.dp)) {
            Text(text = "Icon Button")
            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
        }
        //custom background buttons
        val outlineButtonColor = ButtonDefaults.outlinedButtonColors(
            contentColor = purple200,
        )
        val mainButtonColor = ButtonDefaults.buttonColors(
            backgroundColor = purple,
            contentColor = MaterialTheme.colors.surface
        )
        OutlinedButton(
            colors = outlineButtonColor,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = "Outline colors")
        }
        Button(colors = mainButtonColor, onClick = {}, modifier = Modifier.padding(8.dp)) {
            Text(text = "Custom colors")
        }

        val horizontalGradient = Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colors.primary, MaterialTheme.colors.primaryVariant),
            0f,
            250f
        )
        val verticalGradient = Brush.verticalGradient(
            colors = listOf(MaterialTheme.colors.primary, MaterialTheme.colors.primaryVariant),
            startY = 0f,
            endY = 100f
        )
        Text(
            text = "Horizontal gradient",
            style = typography.body2.copy(color = Color.White),
            modifier = Modifier.padding(12.dp).clickable(onClick = {})
                .clip(RoundedCornerShape(4.dp))
                .background(brush = horizontalGradient).padding(12.dp)
        )
        Text(
            text = "Vertical gradient",
            style = typography.body1.copy(color = Color.White),
            modifier = Modifier.padding(12.dp).clickable(onClick = {})
                .clip(RoundedCornerShape(4.dp))
                .background(brush = verticalGradient).padding(12.dp)
        )
    }
}
