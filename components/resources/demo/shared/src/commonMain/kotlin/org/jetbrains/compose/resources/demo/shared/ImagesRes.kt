package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.resources.demo.shared.generated.resources.Res
import components.resources.demo.shared.generated.resources.compose
import components.resources.demo.shared.generated.resources.droid_icon
import components.resources.demo.shared.generated.resources.insta_icon
import components.resources.demo.shared.generated.resources.land
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.DefaultComposeEnvironment
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ThemeQualifier
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalResourceApi::class)
private val InvertedThemeComposeEnvironment = object : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val defaultEnvironment = DefaultComposeEnvironment.rememberEnvironment()
        return remember(defaultEnvironment) {
            val invertedTheme = when (defaultEnvironment.theme) {
                ThemeQualifier.LIGHT -> ThemeQualifier.DARK
                ThemeQualifier.DARK -> ThemeQualifier.LIGHT
            }

            defaultEnvironment.copy(theme = invertedTheme)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImagesRes(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(contentPadding).verticalScroll(rememberScrollState()),
    ) {
        SvgShowcase()
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(Res.drawable.compose),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = painterResource(Res.drawable.compose)
                        )
                    """.trimIndent()
                )
            }
        }
        OutlinedCard(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Inverted ThemeQualifier")

                CompositionLocalProvider(
                    value = LocalComposeEnvironment provides InvertedThemeComposeEnvironment
                ) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(Res.drawable.compose),
                        contentDescription = null
                    )
                }

                Text(
                    """
                        Image(
                          painter = painterResource(Res.drawable.compose)
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
                    painter = painterResource(Res.drawable.insta_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          painter = painterResource(Res.drawable.insta_icon)
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
                    bitmap = imageResource(Res.drawable.land),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          bitmap = imageResource(Res.drawable.land)
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
                    imageVector = vectorResource(Res.drawable.droid_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Image(
                          imageVector = vectorResource(Res.drawable.droid_icon)
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
                    painter = painterResource(Res.drawable.compose),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = painterResource(Res.drawable.compose)
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
                    painter = painterResource(Res.drawable.insta_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          painter = painterResource(Res.drawable.insta_icon)
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
                    bitmap = imageResource(Res.drawable.land),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          bitmap = imageResource(Res.drawable.land)
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
                    imageVector = vectorResource(Res.drawable.droid_icon),
                    contentDescription = null
                )
                Text(
                    """
                        Icon(
                          imageVector = vectorResource(Res.drawable.droid_icon)
                        )
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
expect fun SvgShowcase()