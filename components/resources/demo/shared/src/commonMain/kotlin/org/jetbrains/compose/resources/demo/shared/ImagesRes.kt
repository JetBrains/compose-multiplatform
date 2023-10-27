package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.rememberImageVector
import org.jetbrains.compose.resources.rememberPainter

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImagesRes(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    painter = rememberPainter("images/compose.png"),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = rememberPainter(
                            "images/compose.png"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    painter = rememberPainter("images/insta_icon.xml"),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = rememberPainter(
                            "images/insta_icon.xml"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(140.dp),
                    bitmap = rememberImageBitmap("images/land.webp"),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          bitmap = rememberImageBitmap(
                            "images/land.webp"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    imageVector = rememberImageVector("images/droid_icon.xml"),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          imageVector = rememberImageVector(
                            "images/droid_icon.xml"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = rememberPainter("images/compose.png"),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = rememberPainter(
                            "images/compose.png"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = rememberPainter("images/insta_icon.xml"),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = rememberPainter(
                            "images/insta_icon.xml"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(140.dp),
                    bitmap = rememberImageBitmap("images/land.webp"),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          bitmap = rememberImageBitmap(
                            "images/land.webp"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    imageVector = rememberImageVector("images/droid_icon.xml"),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          imageVector = rememberImageVector(
                            "images/droid_icon.xml"
                          )
                        )
                    """.trimIndent()
                )
            }
        }
    }
}