/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import androidx.compose.runtime.State
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.LOAD_STRATEGY_MANUAL
import androidx.emoji2.text.EmojiCompat.MetadataRepoLoader
import androidx.emoji2.text.MetadataRepo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EmojiCompatStatusTest {

    @Before
    fun reset() {
        EmojiCompat.reset(null)
        EmojiCompatStatus.setDelegateForTesting(null)
    }

    @After
    fun clean() {
        EmojiCompat.reset(null)
        EmojiCompatStatus.setDelegateForTesting(null)
    }

    @Test
    fun nonConfiguredEc_isNotLoaded() {
        EmojiCompat.reset(null)
        assertThat(EmojiCompatStatus.fontLoaded.value).isFalse()
    }

    @Test
    fun default_isNotLoaded() {
        val (config, deferred) = makeEmojiConfig()
        EmojiCompat.init(config)
        assertThat(EmojiCompatStatus.fontLoaded.value).isFalse()
        deferred.complete(null)
    }

    @Test
    fun loading_isNotLoaded() {
        val (config, deferred) = makeEmojiConfig()
        val ec = EmojiCompat.init(config)
        ec.load()
        assertThat(EmojiCompatStatus.fontLoaded.value).isFalse()
        deferred.complete(null)
    }

    @Test
    fun error_isNotLoaded() {
        val (config, deferred) = makeEmojiConfig()
        val ec = EmojiCompat.init(config)
        ec.load()
        deferred.complete(null)
        assertThat(EmojiCompatStatus.fontLoaded.value).isFalse()
    }

    @Test
    fun loaded_isLoaded() {
        val (config, deferred) = makeEmojiConfig()
        val ec = EmojiCompat.init(config)
        deferred.complete(MetadataRepo.create(Typeface.DEFAULT))
        ec.load()
        // query now, after init EC
        EmojiCompatStatus.setDelegateForTesting(null)
        EmojiCompatStatus.fontLoaded.assertTrue()
    }

    @Test
    fun nonLoaded_toLoaded_updatesReturnState() {
        val (config, deferred) = makeEmojiConfig()
        val ec = EmojiCompat.init(config)
        val state = EmojiCompatStatus.fontLoaded
        assertThat(state.value).isFalse()

        deferred.complete(MetadataRepo.create(Typeface.DEFAULT))
        ec.load()

        state.assertTrue()
    }

    @Test
    fun nonConfigured_canLoadLater() {
        EmojiCompat.reset(null)
        val initialFontLoad = EmojiCompatStatus.fontLoaded
        assertThat(initialFontLoad.value).isFalse()

        val (config, deferred) = makeEmojiConfig()
        val ec = EmojiCompat.init(config)
        deferred.complete(MetadataRepo.create(Typeface.DEFAULT))
        ec.load()

        EmojiCompatStatus.fontLoaded.assertTrue()
    }

    private fun State<Boolean>.assertTrue() {
        // there's too many async actors to do anything reasonable and non-flaky here. tie up the
        // test thread until main posts the value
        runBlocking {
            withTimeout(1000) {
                while (!value)
                    delay(0)
            }
            assertThat(value).isTrue()
        }
    }

    private fun makeEmojiConfig(): Pair<EmojiCompat.Config, CompletableDeferred<MetadataRepo?>> {
        val deferred = CompletableDeferred<MetadataRepo?>(null)
        val loader = MetadataRepoLoader { cb ->
            CoroutineScope(Dispatchers.Default).launch {
                val result = deferred.await()
                if (result != null) {
                    cb.onLoaded(result)
                } else {
                    cb.onFailed(IllegalStateException("No"))
                }
            }
        }
        val config = object : EmojiCompat.Config(loader) {}
        config.setMetadataLoadStrategy(LOAD_STRATEGY_MANUAL)
        return config to deferred
    }
}