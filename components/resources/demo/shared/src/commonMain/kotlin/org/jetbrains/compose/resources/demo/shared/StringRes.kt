package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.resources.demo.shared.generated.resources.*
import org.jetbrains.compose.resources.*

@Composable
fun StringRes(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = stringResource(Res.string.app_name),
            onValueChange = {},
            label = { Text("Text(stringResource(Res.string.app_name))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = stringResource(Res.string.hello),
            onValueChange = {},
            label = { Text("Text(stringResource(Res.string.hello))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = stringResource(Res.string.multi_line),
            onValueChange = {},
            label = { Text("Text(stringResource(Res.string.multi_line))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = stringResource(Res.string.str_template, "User_name", 100),
            onValueChange = {},
            label = { Text("Text(stringResource(Res.string.str_template, \"User_name\", 100))") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = stringArrayResource(Res.array.str_arr).toString(),
            onValueChange = {},
            label = { Text("Text(stringArrayResource(Res.array.str_arr).toString())") },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        var numMessages by remember { mutableStateOf(0) }
        OutlinedTextField(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            value = pluralStringResource(Res.plurals.new_message, numMessages, numMessages),
            onValueChange = {},
            label = { Text("Text(pluralStringResource(Res.plurals.new_message, $numMessages, $numMessages))") },
            leadingIcon = {
                Row {
                    IconButton({ numMessages += 1 }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Message")
                    }
                }
            },
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            )
        )
    }
}