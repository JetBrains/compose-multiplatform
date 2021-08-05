package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.data.DemoDataProvider
import org.jetbrains.compose.demo.widgets.platform.Res
import org.jetbrains.compose.demo.widgets.platform.imageResource
import org.jetbrains.compose.demo.widgets.theme.typography

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UICards() {
    val item = remember { DemoDataProvider.item }

    Text(
        text = "Inbuilt box as container for any Clipping/Alignment controls",
        style = typography.subtitle1,
        modifier = Modifier.padding(8.dp)
    )
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.primary,
        shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column {
            Text(
                text = item.title,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colors.onPrimary
            )
            Text(
                text = item.subtitle,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
    Divider()

    Text(text = "Inbuilt Card", style = typography.subtitle1, modifier = Modifier.padding(8.dp))
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp
    ) {
        Row {
            Image(
                imageResource(Res.drawable.p3),
                contentDescription = null,
                modifier = Modifier.requiredSize(60.dp)
            )
            Text(text = item.title, modifier = Modifier.padding(16.dp))
        }
    }
    Divider()

    Text(
        text = "In-built ListItems",
        style = typography.subtitle1,
        modifier = Modifier.padding(8.dp)
    )
    ListItem(text = { Text(item.title) }, secondaryText = { Text(item.subtitle) })
    Divider(modifier = Modifier.padding(4.dp))
    ListItem(
        text = { Text(item.title) },
        secondaryText = { Text(item.subtitle) },
        singleLineSecondaryText = false
    )
    Divider(modifier = Modifier.padding(4.dp))
    ListItem(text = { Text(item.title) }, secondaryText = { Text(item.subtitle) }, icon = {
        Image(
            imageResource(Res.drawable.p3),
            contentDescription = null
        )
    })
    Divider(modifier = Modifier.padding(4.dp))
    //I am not sure why this is not going multiline for secondaryText...
    ListItem(
        text = { Text(item.title) },
        secondaryText = { Text(item.subtitle) },
        icon = { Image(imageResource(Res.drawable.p1), contentDescription = null) },
        overlineText = { Text("Overline text") },
        singleLineSecondaryText = false
    )
    Divider()
    ListItem(
        text = { Text(item.title) },
        secondaryText = { Text(item.subtitle) },
        icon = { Image(imageResource(Res.drawable.p2), contentDescription = null) },
        trailing = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
        singleLineSecondaryText = false
    )
    Divider()

}