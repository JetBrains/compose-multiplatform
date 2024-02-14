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
import components.resources.demo.shared.generated.resources.Res
import components.resources.demo.shared.generated.resources.*
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
    }
}