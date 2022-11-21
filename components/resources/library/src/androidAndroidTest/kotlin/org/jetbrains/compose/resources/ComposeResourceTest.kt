package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(InternalTestApi::class, ExperimentalResourceApi::class)
class ComposeResourceTest {
    @get:Rule
    val rule = createComposeRule()

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
            delay(100)
            mutableStateFlow.value = false
            delay(100)
            Assert.assertEquals(2, recompositionCount)
        }
    }

}

@Composable
fun CountRecompositions(imageBitmap: ImageBitmap?, onRecomposition: () -> Unit) {
    onRecomposition()
    println("imageBitmap: $imageBitmap")
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = null)
    }
}
