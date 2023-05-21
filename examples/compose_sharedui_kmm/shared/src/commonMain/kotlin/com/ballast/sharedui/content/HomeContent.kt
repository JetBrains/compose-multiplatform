package com.ballast.sharedui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ballast.sharedui.root.HomeContract
import com.ballast.sharedui.root.HomeViewModel

@Composable
internal fun HomeContent(
    onGoToCounter: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val vm = remember(coroutineScope) { HomeViewModel(coroutineScope) }
    val state by vm.observeStates().collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to Ballast",
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "What's you name?",
                    style = MaterialTheme.typography.h4,
                    modifier = Modifier.padding(16.dp)
                )
                TextField(
                    value = state.name,
                    onValueChange = { vm.trySend(HomeContract.Inputs.OnNameChanged(it)) },
                    label = { Text("Name") },
                    modifier = Modifier.padding(16.dp)
                )
                if (state.name.isNotEmpty()) {
                    Text(
                        text = "Hello ${state.name}!",
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "Please enter your name",
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Button(
                    onClick = onGoToCounter
                ) {
                    Text("Go to Counter")
                }
            }
        }
    }
}
