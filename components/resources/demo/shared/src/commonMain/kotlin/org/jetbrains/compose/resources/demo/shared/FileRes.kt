package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.resources.demo.shared.generated.resources.Res

@Composable
fun FileRes(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.padding(paddingValues)
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "File: 'files/icon.xml'",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            var bytes by remember { mutableStateOf(ByteArray(0)) }
            LaunchedEffect(Unit) {
                bytes = Res.readBytes("files/icon.xml")
            }
            Text(
                modifier = Modifier.padding(8.dp).height(200.dp).verticalScroll(rememberScrollState()),
                text = bytes.decodeToString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }
        Text(
            modifier = Modifier.padding(16.dp),
            text = """
                var bytes by remember { 
                  mutableStateOf(ByteArray(0))
                }
                LaunchedEffect(Unit) {
                  bytes = Res.readFileBytes("files/icon.xml")
                }
                Text(bytes.decodeToString())
            """.trimIndent()
        )
        Text(
            modifier = Modifier.padding(16.dp),
            text = "File: 'files/platform-text.txt'",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            var bytes by remember { mutableStateOf(ByteArray(0)) }
            LaunchedEffect(Unit) {
                bytes = Res.readBytes("files/platform-text.txt")
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = bytes.decodeToString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            modifier = Modifier.padding(16.dp),
            text = """
                var bytes by remember { 
                  mutableStateOf(ByteArray(0))
                }
                LaunchedEffect(Unit) {
                  bytes = Res.readFileBytes("files/platform-text.txt")
                }
                Text(bytes.decodeToString())
            """.trimIndent()
        )
    }
}