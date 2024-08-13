package com.example.jetsnack.ui.home.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.example.jetsnack.model.OrderLine
import kotlinx.coroutines.flow.StateFlow
import kotlin.native.HiddenFromObjC

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@HiddenFromObjC // Remove after the bug is fixed: https://github.com/JetBrains/compose-multiplatform/issues/4848
actual abstract class JetSnackCartViewModel actual constructor()  {

    @Composable
    actual fun collectOrderLinesAsState(flow: StateFlow<List<OrderLine>>): State<List<OrderLine>> {
        return flow.collectAsState()
    }
}