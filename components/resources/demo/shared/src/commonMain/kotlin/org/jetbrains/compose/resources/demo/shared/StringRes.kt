package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.jetbrains.compose.resources.readBytes

@OptIn(ExperimentalResourceApi::class)
@Composable
fun StringRes(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "strings.xml",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedCard(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            var bytes by remember { mutableStateOf(ByteArray(0)) }
            LaunchedEffect(Unit) {
                bytes = readBytes("strings.xml")
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = bytes.decodeToString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                softWrap = false
            )
        }
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = getString("app_name"),
            onValueChange = {},
            label = { Text("Text(getString(\"app_name\"))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = getString("hello"),
            onValueChange = {},
            label = { Text("Text(getString(\"hello\"))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = getString("multi_line"),
            onValueChange = {},
            label = { Text("Text(getString(\"multi_line\"))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = getString("str_template", "User_name", 100),
            onValueChange = {},
            label = { Text("Text(getString(\"multi_line\", \"User_name\", 100))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = getStringArray("str_arr").toString(),
            onValueChange = {},
            label = { Text("Text(getStringArray(\"str_arr\").toString())") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
    }
}