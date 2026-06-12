package com.adrianwitaszak.ballast.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.adrianwitaszak.ballast.web.components.widgets.Card
import com.ballast.sharedui.root.CounterContract
import com.ballast.sharedui.root.CounterViewModel
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.px

@Composable
fun Counter(
    goBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val vm = remember(coroutineScope) {
        CounterViewModel(viewModelCoroutineScope = coroutineScope)
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
                SpanText("Counter")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.gap(24.px)
                ) {
                    Button(
                        onClick = { vm.trySend(CounterContract.Inputs.Decrement()) },
                    ) {
                        SpanText(text = "-")
                    }
                    SpanText("Current count: ${state.count}")
                    Button(
                        onClick = { vm.trySend(CounterContract.Inputs.Increment()) },
                    ) {
                        SpanText(text = "+")
                    }
                }

                Button(
                    onClick = { goBack() },
                ) {
                    SpanText(text = "Go back")
                }
            }
        }
    }
}
