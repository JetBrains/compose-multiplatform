package org.jetbrains.compose.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TestWebResourceCache {

    //https://youtrack.jetbrains.com/issue/CMP-10137
    @Test
    fun testPoisonedCache() = clearResourceCachesAndRunUiTest {
        val res = TestStringResource("app_name")

        var showBranchB by mutableStateOf(false)
        var branchAText = ""
        var branchBText = ""
        var branchACancelled = false
        var branchBStartedBeforeBranchACancelled = false
        var resourceLoadingWasCancelled = false

        val testResourceReader = object : ResourceReader {
            lateinit var loadContinuation: CancellableContinuation<ByteArray>
            override suspend fun readPart(
                path: String,
                offset: Long,
                size: Long
            ): ByteArray = suspendCancellableCoroutine {
                loadContinuation = it
                it.invokeOnCancellation { resourceLoadingWasCancelled = true }
            }

            suspend fun provideData(str: String) {
                yield()
                loadContinuation.resume(Base64.encodeToByteArray(str.encodeToByteArray()))
                yield()
            }

            override suspend fun read(path: String) = TODO()
            override fun getUri(path: String) = TODO()
        }

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                if (!showBranchB) {
                    branchAText = stringResource(res)
                    Text(branchAText)
                    LaunchedEffect(Unit) { showBranchB = true }
                    DisposableEffect(Unit) {
                        onDispose {
                            branchACancelled = true
                        }
                    }
                } else {
                    branchBStartedBeforeBranchACancelled = branchACancelled == false
                    branchBText = stringResource(res)
                    Text(branchBText)
                }
            }
        }

        waitForIdle()
        assertTrue(branchBStartedBeforeBranchACancelled, "Branch B should start before Branch A is cancelled")
        assertEquals("", branchAText, "Branch A text should be empty when Branch A is cancelled")

        testResourceReader.provideData("Compose Resources App")

        waitForIdle()
        assertFalse(resourceLoadingWasCancelled, "Resource loading should not be cancelled")
        assertEquals("Compose Resources App", branchBText)
    }
}