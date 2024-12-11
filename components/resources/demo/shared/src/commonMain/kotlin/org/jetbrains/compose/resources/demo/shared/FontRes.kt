package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import components.resources.demo.shared.generated.resources.Res
import components.resources.demo.shared.generated.resources.RobotoFlex_VariableFont
import components.resources.demo.shared.generated.resources.Workbench_Regular
import components.resources.demo.shared.generated.resources.font_awesome
import org.jetbrains.compose.resources.Font
import kotlin.math.roundToInt

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
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily(Font(Res.font.Workbench_Regular)),
                        style = MaterialTheme.typography.headlineLarge,
                        text = "brown fox jumps over the lazy dog"
                    ) 
                """.trimIndent(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }
        Text(
            modifier = Modifier.padding(16.dp),
            fontFamily = FontFamily(Font(Res.font.Workbench_Regular)),
            style = MaterialTheme.typography.headlineLarge,
            text = "brown fox jumps over the lazy dog"
        )

        OutlinedCard(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = """
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily(Font(Res.font.font_awesome)),
                        style = MaterialTheme.typography.headlineLarge,
                        text ="\uf1ba \uf238 \uf21a \uf1bb \uf1b8 \uf09b \uf269 \uf1d0 \uf15a \uf293 \uf1c6"
                    ) 
                """.trimIndent(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }

        Text(
            modifier = Modifier.padding(16.dp),
            fontFamily = FontFamily(Font(Res.font.font_awesome)),
            style = MaterialTheme.typography.headlineLarge,
            text = "\uf1ba \uf238 \uf21a \uf1bb \uf1b8 \uf09b \uf269 \uf1d0 \uf15a \uf293 \uf1c6"
        )

        var variableFontWeight by remember { mutableStateOf(200) }
        val variableFont = Font(
            resource = Res.font.RobotoFlex_VariableFont,
            variationSettings = FontVariation.Settings(FontVariation.weight(variableFontWeight))
        )
        OutlinedCard(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = """
                        Text(
                            modifier = Modifier.padding(16.dp),
                            fontFamily = FontFamily(
                                Font(
                                    Res.font.RobotoFlex_VariableFont,
                                    variationSettings = FontVariation.Settings(
                                        FontVariation.weight($variableFontWeight)
                                    )
                                ),
                            ),
                            text = "The quick brown fox jumps over the lazy dog"
                        )
                """.trimIndent(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }

        Text(
            modifier = Modifier.padding(16.dp),
            fontFamily = FontFamily(variableFont),
            text = "The quick brown fox jumps over the lazy dog"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(text = "Weight:")
            Spacer(modifier = Modifier.size(16.dp))
            Slider(
                value = variableFontWeight.toFloat(),
                onValueChange = { variableFontWeight = it.roundToInt() },
                valueRange = 100f..1000f,
                steps = 9
            )
        }
    }
}