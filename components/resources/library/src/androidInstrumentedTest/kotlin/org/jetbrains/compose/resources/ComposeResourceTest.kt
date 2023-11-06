package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalResourceApi::class, ExperimentalCoroutinesApi::class)
class ComposeResourceTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testMissingResource() = runTest (UnconfinedTestDispatcher()) {
        var recompositionCount = 0
        rule.setContent {
            CountRecompositions(resource("missing.png").rememberImageBitmap().orEmpty()) {
                recompositionCount++
            }
        }
        rule.awaitIdle()
        assertEquals(2, recompositionCount)
    }

    @Test
    fun testCountRecompositions() = runTest (UnconfinedTestDispatcher()) {
        val mutableStateFlow = MutableStateFlow(true)
        var recompositionCount = 0
        rule.setContent {
            val state: Boolean by mutableStateFlow.collectAsState(true)
            val resource = resource(if (state) "1.png" else "2.png")
            CountRecompositions(resource.rememberImageBitmap().orEmpty()) {
                recompositionCount++
            }
        }
        rule.awaitIdle()
        mutableStateFlow.value = false
        rule.awaitIdle()
        assertEquals(4, recompositionCount)
    }

}

@Composable
private fun CountRecompositions(imageBitmap: ImageBitmap?, onRecomposition: () -> Unit) {
    onRecomposition()
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = null)
    }
}
