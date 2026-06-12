package com.adrianwitaszak.ballast.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.adrianwitaszak.ballast.web.components.widgets.Card
import com.adrianwitaszak.ballast.web.components.widgets.EditableTextView
import com.ballast.sharedui.root.HomeContract
import com.ballast.sharedui.root.HomeViewModel
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.px

@Composable
fun Home(
    goToCounter: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val vm = remember(coroutineScope) {
        HomeViewModel(viewModelCoroutineScope = coroutineScope)
    }
    val state by vm.observeStates().collectAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Card {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.gap(24.px)
            ) {
                SpanText("Welcome to Kobweb!")
                SpanText("What is your name?")
                EditableTextView(
                    value = state.name,
                    onValueChange = { vm.trySend(HomeContract.Inputs.OnNameChanged(it)) }
                )
                if (state.name.isNotBlank()) {
                    SpanText("Hello, ${state.name}!")
                } else {
                    SpanText("")
                }

                Button(
                    onClick = { goToCounter() },
                ) {
                    SpanText(text = "Go to Counter")
                }
            }
        }
    }
}
