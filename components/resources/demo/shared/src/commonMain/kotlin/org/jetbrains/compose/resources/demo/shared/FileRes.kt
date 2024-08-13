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
import components.resources.demo.shared.generated.resources.droid_icon
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment

@OptIn(ExperimentalResourceApi::class)
@Composable
fun FileRes(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState())
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
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        OutlinedCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            val composeEnv = rememberResourceEnvironment()
            var bytes by remember { mutableStateOf(ByteArray(0)) }
            LaunchedEffect(Unit) {
                bytes = getDrawableResourceBytes(composeEnv, Res.drawable.droid_icon)
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = "droid_icon byte size = " + bytes.size,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            modifier = Modifier.padding(16.dp),
            text = """
                val composeEnv = rememberResourceEnvironment()
                var bytes by remember { mutableStateOf(ByteArray(0)) }
                LaunchedEffect(Unit) {
                    bytes = getDrawableResourceBytes(composeEnv, Res.drawable.droid_icon)
                }
                Text("droid_icon byte size = " + bytes.size)
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
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        OutlinedCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = "File URI: " + Res.getUri("files/platform-text.txt"),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            modifier = Modifier.padding(16.dp),
            text = """
                Text("File URI: " + Res.getUri("files/platform-text.txt"))
            """.trimIndent()
        )
    }
}