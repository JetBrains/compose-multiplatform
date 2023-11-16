package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.resources.demo.generated.resources.Res
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ImagesRes(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(contentPadding).verticalScroll(rememberScrollState()),
    ) {
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(Res.images.compose),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = painterResource(Res.images.compose)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(Res.images.insta_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = painterResource(Res.images.insta_icon)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(140.dp),
                    bitmap = imageResource(Res.images.land),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          bitmap = imageResource(Res.images.land)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    imageVector = vectorResource(Res.images.droid_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          imageVector = vectorResource(Res.images.droid_icon)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(Res.images.compose),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = painterResource(Res.images.compose)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(Res.images.insta_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = painterResource(Res.images.insta_icon)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(140.dp),
                    bitmap = imageResource(Res.images.land),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          bitmap = imageResource(Res.images.land)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    imageVector = vectorResource(Res.images.droid_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          imageVector = vectorResource(Res.images.droid_icon)
                        )
                    """.trimIndent()
                )
            }
        }
    }
}