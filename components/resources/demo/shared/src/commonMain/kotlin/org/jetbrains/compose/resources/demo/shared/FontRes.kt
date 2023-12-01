package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import components.resources.demo.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun FontRes(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.padding(paddingValues)
    ) {
        OutlinedCard(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = """
                    val fontAwesome = FontFamily(Font(Res.fonts.font_awesome))
                    val symbols = arrayOf(0xf1ba, 0xf238, 0xf21a, 0xf1bb, 0xf1b8, 0xf09b, 0xf269, 0xf1d0, 0xf15a, 0xf293, 0xf1c6)
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontFamily = fontAwesome,
                        style = MaterialTheme.typography.headlineLarge,
                        text = symbols.joinToString(" ") { it.toChar().toString() }
                    ) 
                """.trimIndent(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }

        val fontAwesome = FontFamily(Font(Res.fonts.font_awesome))
        val symbols = arrayOf(0xf1ba, 0xf238, 0xf21a, 0xf1bb, 0xf1b8, 0xf09b, 0xf269, 0xf1d0, 0xf15a, 0xf293, 0xf1c6)
        Text(
            modifier = Modifier.padding(16.dp),
            fontFamily = fontAwesome,
            style = MaterialTheme.typography.headlineLarge,
            text = symbols.joinToString(" ") { it.toChar().toString() }
        )
    }
}