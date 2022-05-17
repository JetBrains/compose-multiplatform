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

package androidx.compose.ui.text.googlefonts

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.AssetManager
import android.graphics.Typeface
import android.os.Handler
import androidx.annotation.WorkerThread
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.test.R
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class GoogleFontTest {

    val context = InstrumentationRegistry.getInstrumentation().context

    private val TestProvider = GoogleFont.Provider(
        "providerAuthority",
        "providerPackage",
        listOf(listOf(ByteArray(100) { it.toByte() }))
    )

    @Test
    fun GoogleFont_create_ComposeFont() {
        val font = Font(GoogleFont("Test font"), TestProvider)
        assertThat(font).isInstanceOf(Font::class.java)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun GoogleFont_is_AsyncFont() {
        val font = Font(GoogleFont("Test font"), TestProvider)
        assertThat(font.loadingStrategy).isEqualTo(FontLoadingStrategy.Async)
    }

    @Test
    fun GoogleFont_default_W400() {
        val font = Font(GoogleFont("Test"), TestProvider)
        assertThat(font.weight).isEqualTo(FontWeight.W400)
    }

    @Test
    fun GoogleFont_default_isNormal() {
        val font = Font(GoogleFont("Test"), TestProvider)
        assertThat(font.style).isEqualTo(FontStyle.Normal)
    }

    @Test
    fun GoogleFont_default_bestEffort_true() {
        val font = Font(GoogleFont("best effort"), TestProvider) as GoogleFontImpl
        assertThat(font.bestEffort).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun GoogleFont_throwsOn_emptyName() {
       Font(GoogleFont(""), TestProvider)
    }

    @Test
    fun GoogleFont_keepsUrlEncodingRequiredNames() {
        val expected = "!@#$%^&*(){}'<>PYFGCRL?+|AOEUIDHTNS_:QJKXBMWVZ~~`1234567890[]/=\\-;:,."
        val font = Font(GoogleFont(expected), TestProvider) as GoogleFontImpl
        assertThat(font.name).isEqualTo(expected)
    }

    @Test
    fun GoogleFontImpl_fontRequest_containsName() {
        val font = Font(GoogleFont("Test Name"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("name=Test Name")
    }

    @Test
    fun GoogleFontImpl_fontRequest_containsWeight() {
        val font = Font(GoogleFont("a"), TestProvider, weight = FontWeight.W800) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("weight=800")
    }

    @Test
    fun GoogleFontImpl_fontRequest_containsStyle_normal() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("italic=0")
    }

    @Test
    fun GoogleFontImpl_fontRequest_containsStyle_italic() {
        val font = Font(GoogleFont("a"), TestProvider, style = FontStyle.Italic) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("italic=1")
    }

    @Test
    fun GoogleFontImpl_fontRequest_bestEffort() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("besteffort=true")
    }

    @Test
    fun GoogleFontImpl_fontRequest_bestEffort_false() {
        val font = Font(GoogleFont("a", bestEffort = false), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().query).contains("besteffort=false")
    }

    @Test
    fun GoogleFontImpl_providerAuthority_passedDown() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().providerAuthority).isEqualTo(TestProvider.providerAuthority)
    }

    @Test
    fun GoogleFontImpl_providerPackage_passedDown() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().providerPackage).isEqualTo(TestProvider.providerPackage)
    }

    @Test
    fun GoogleFontImpl_providerCerts_passedDown() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toFontRequest().certificates).isEqualTo(TestProvider.certificates)
    }

    @Test
    fun GoogleFontImpl_providercerts_fromRes_passedDown() {
        val provider = GoogleFont.Provider("a", "b", 17)
        val font = Font(GoogleFont("a"), provider) as GoogleFontImpl
        assertThat(font.toFontRequest().certificatesArrayResId).isEqualTo(17)
    }

    @Test
    fun GoogleFontImpl_TypefaceStyle_Normal() {
        val font = Font(GoogleFont("a"), TestProvider) as GoogleFontImpl
        assertThat(font.toTypefaceStyle()).isEqualTo(Typeface.NORMAL)
    }

    @Test
    fun GoogleFontImpl_TypefaceStyle_Italic() {
        val font = Font(GoogleFont("a"), TestProvider, style = FontStyle.Italic) as GoogleFontImpl
        assertThat(font.toTypefaceStyle()).isEqualTo(Typeface.ITALIC)
    }

    @Test
    fun GoogleFontImpl_TypefaceStyle_Bold() {
        val font = Font(GoogleFont("a"), TestProvider, weight = FontWeight.Bold) as GoogleFontImpl
        assertThat(font.toTypefaceStyle()).isEqualTo(Typeface.BOLD)
    }

    @Test
    fun GoogleFontImpl_TypefaceStyle_BoldItalic() {
        val font = Font(
            GoogleFont("a"),
            TestProvider,
            weight = FontWeight.Bold,
            style = FontStyle.Italic
        ) as GoogleFontImpl
        assertThat(font.toTypefaceStyle()).isEqualTo(Typeface.BOLD_ITALIC)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GoogleFont_TypefaceLoader_resumesOnCompletion() {
        val compatLoader = CapturingFontsContractCompatLoader()
        runTest(UnconfinedTestDispatcher()) {
            val deferred = async {
                GoogleFontTypefaceLoader.awaitLoad(
                    context,
                   Font(GoogleFont("Foo"), TestProvider) as AndroidFont,
                    compatLoader
                )
            }
            compatLoader.callback?.onTypefaceRetrieved(Typeface.MONOSPACE)
            assertThat(deferred.await()).isEqualTo(Typeface.MONOSPACE)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GoogleFont_TypefaceLoader_throwsOnError() {
        val compatLoader = CapturingFontsContractCompatLoader()
        runTest(UnconfinedTestDispatcher()) {
            val deferred = async(Job()) {
                GoogleFontTypefaceLoader.awaitLoad(
                    context,
                   Font(GoogleFont("Foo"), TestProvider) as AndroidFont,
                    compatLoader
                )
            }
            compatLoader.callback?.onTypefaceRequestFailed(42)
            var exception: IllegalStateException? = null
            try {
                assertThat(deferred.await())
            } catch (ex: IllegalStateException) {
                exception = ex
            }
            assertThat(exception?.message).contains("reason=42")
            assertThat(exception?.message).contains("GoogleFont(\"Foo\"")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GoogleFont_TypefaceLoader_throwsOnError_withFullListUrl() {
        val compatLoader = CapturingFontsContractCompatLoader()
        runTest(UnconfinedTestDispatcher()) {
            val deferred = async(Job()) {
                GoogleFontTypefaceLoader.awaitLoad(
                    context,
                    Font(GoogleFont("Foo"), TestProvider) as AndroidFont,
                    compatLoader
                )
            }
            compatLoader.callback?.onTypefaceRequestFailed(FAIL_REASON_FONT_NOT_FOUND)
            var exception: IllegalStateException? = null
            try {
                assertThat(deferred.await())
            } catch (ex: IllegalStateException) {
                exception = ex
            }
            assertThat(exception?.message).contains("Font not found, please check availability " +
                "on GoogleFont.Provider.AllFontsList: https://fonts.gstatic.com/s/a/directory.xml")
        }
    }

    @Test
    fun GoogleFont_toString() {
        val font = Font(GoogleFont("Font Family"), TestProvider)
        assertThat(font.toString()).isEqualTo(
            "Font(GoogleFont(\"Font Family\", bestEffort=true), weight=FontWeight(weight=400), " +
                "style=Normal)"
        )
    }

    @Test
    fun GoogleFont_Provider_withResId() {
        val provider = GoogleFont.Provider("provider", "package", 17)
        assertThat(provider.certificatesRes).isEqualTo(17)
        assertThat(provider.certificates).isNull()
    }

    @Test
    fun GoogleFont_Provider_withCertsArray() {
        val provider = GoogleFont.Provider(
            "provider",
            "package",
            listOf(listOf(ByteArray(100) { it.toByte() }))
        )
        assertThat(provider.certificatesRes).isEqualTo(0)
        assertThat(provider.certificates).isNotNull()
    }

    // disabled for CI, enable locally to test gms certs integrations
    private val RunRealGmsIntegrationTests = false

    @Test(expected = IllegalStateException::class)
    fun GoogleFontProvider_isAvailableOnDevice_throwsCorrectly() {
        assumeTrue(RunRealGmsIntegrationTests)
        // only run this where gms is present
        val packageInfo = getComAndroidGmsOrNull()
        assumeTrue(packageInfo != null)

        val provider = GoogleFont.Provider(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            listOf() /* this is never a valid cert */
        )
        provider.isAvailableOnDevice(context)
    }

    @Test
    fun GoogleFontProvider_isAvailableOnDevice_realCerts_isTrue() {
        assumeTrue(RunRealGmsIntegrationTests)

        // only run this where gms is present
        val packageInfo = getComAndroidGmsOrNull()
        assumeTrue(packageInfo != null)

        val provider = GoogleFont.Provider(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            listOf(
                listOf(loadComGoogleAndroidGmsProdCertificateByteArray(context.assets)),
                listOf(loadComGoogleAndroidGmsDevCertificateByteArray(context.assets))
            )
        )

        assertThat(provider.isAvailableOnDevice(context)).isTrue()
    }

    @Test
    fun GoogleFontProvider_isAvailableOnDevice_realCerts_xml_isTrue() {
        assumeTrue(RunRealGmsIntegrationTests)

        // only run this where gms is present
        val packageInfo = getComAndroidGmsOrNull()
        assumeTrue(packageInfo != null)

        val provider = GoogleFont.Provider(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            R.array.com_google_android_gms_fonts_certs
        )
        assertThat(provider.isAvailableOnDevice(context)).isTrue()
    }

    @Test
    fun GoogleFontProvider_isNotAvailableOnDevice_isFalse() {
        // only run this where gms not present, can run on CI
        val packageInfo = getComAndroidGmsOrNull()
        assumeTrue(packageInfo == null)

        val provider = GoogleFont.Provider(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            listOf() /* this is never a valid cert */
        )
        assertThat(provider.isAvailableOnDevice(context)).isFalse()
    }

    @Suppress("DEPRECATION")
    private fun getComAndroidGmsOrNull(): PackageInfo? {
        return try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
        } catch (ex: NameNotFoundException) {
            null
        }
    }

    private class CapturingFontsContractCompatLoader : FontsContractCompatLoader {
        var callback: FontsContractCompat.FontRequestCallback? = null

        override fun requestFont(
            context: Context,
            fontRequest: FontRequest,
            typefaceStyle: Int,
            handler: Handler,
            callback: FontsContractCompat.FontRequestCallback
        ) {
            this.callback = callback
        }
    }
}

@WorkerThread
private fun loadComGoogleAndroidGmsProdCertificateByteArray(assetManager: AssetManager): ByteArray {
    return assetManager.open("ComGoogleAndroidGmsCertificate.prod").use {
        it.readBytes()
    }
}

@WorkerThread
private fun loadComGoogleAndroidGmsDevCertificateByteArray(assetManager: AssetManager): ByteArray {
    return assetManager.open("ComGoogleAndroidGmsCertificate.dev").use {
        it.readBytes()
    }
}