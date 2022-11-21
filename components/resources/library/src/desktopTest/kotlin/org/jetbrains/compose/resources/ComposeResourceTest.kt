package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalResourceApi::class)
class ComposeResourceTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testMissingResource() {
        var recompositionCount = 0
        runBlocking {
            rule.setContent {
                CountRecompositions(resource("missing.png").rememberImageBitmapAsync()) {
                    recompositionCount++
                }
            }
            rule.awaitIdle()
        }
        assertEquals(1, recompositionCount)
    }

    @Test
    fun testCountRecompositions() {
        val mutableStateFlow = MutableStateFlow(true)
        var recompositionCount = 0
        runBlocking(Dispatchers.Main) {
            rule.setContent {
                val state: Boolean by mutableStateFlow.collectAsState(true)
                val resource = resource(if (state) "1.png" else "2.png")
                CountRecompositions(resource.rememberImageBitmapAsync()) {
                    recompositionCount++
                }
            }
            rule.awaitIdle()
            delay(10)
            mutableStateFlow.value = false
            delay(10)
            rule.awaitIdle()
        }
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
