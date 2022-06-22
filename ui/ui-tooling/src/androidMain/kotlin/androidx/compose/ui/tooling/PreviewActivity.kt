/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.tooling

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Activity used to run `@Composable` previews from Android Studio.
 *
 * The supported `@Composable` functions either have no parameters, or have only parameters with
 * default values and/or *one* parameter annotated with `@PreviewParameter`.
 *
 * The `@Composable` fully qualified name must be passed to this Activity through intent parameters,
 * using `composable` as the key. When deploying Compose Previews with `@PreviewParameter`
 * annotated parameters, the provider should be specified as an intent parameter as well, using
 * the key `parameterProviderClassName`. Optionally, `parameterProviderIndex` can also be set to
 * display a specific provider value instead of all of them.
 *
 * @suppress
 */
class PreviewActivity : ComponentActivity() {

    private val TAG = "PreviewActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            Log.d(TAG, "Application is not debuggable. Compose Preview not allowed.")
            finish()
            return
        }

        intent?.getStringExtra("composable")?.let { setComposableContent(it) }
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalComposeUiApi::class)
    private fun setComposableContent(composableFqn: String) {
        Log.d(TAG, "PreviewActivity has composable $composableFqn")
        val className = composableFqn.substringBeforeLast('.')
        val methodName = composableFqn.substringAfterLast('.')

        intent.getStringExtra("parameterProviderClassName")?.let { parameterProvider ->
            setParameterizedContent(className, methodName, parameterProvider)
            return@setComposableContent
        }
        Log.d(TAG, "Previewing '$methodName' without a parameter provider.")
        setContent {
            ComposableInvoker.invokeComposable(
                className,
                methodName,
                currentComposer
            )
        }
    }

    /**
     * Sets the activity content according to a given `@PreviewParameter` provider. If
     * `parameterProviderIndex` is also set, the content will be a single `@Composable` that uses
     * the `parameterProviderIndex`-th value in the provider's sequence as the argument value.
     * Otherwise, the content will display a FAB that changes the argument value on click, cycling
     * through all the values in the provider's sequence.
     */
    @Suppress("DEPRECATION")
    @OptIn(ExperimentalComposeUiApi::class)
    private fun setParameterizedContent(
        className: String,
        methodName: String,
        parameterProvider: String
    ) {
        Log.d(TAG, "Previewing '$methodName' with parameter provider: '$parameterProvider'")
        val previewParameters = getPreviewProviderParameters(
            parameterProvider.asPreviewProviderClass(),
            intent.getIntExtra("parameterProviderIndex", -1)
        )

        // Handle the case where parameterProviderIndex is not provided. In this case, instead of
        // showing an arbitrary value (e.g. the first one), we display a FAB that can be used to
        // cycle through all the values.
        if (previewParameters.size > 1) {
            setContent {
                val index = remember { mutableStateOf(0) }

                Scaffold(
                    content = {
                        ComposableInvoker.invokeComposable(
                            className,
                            methodName,
                            currentComposer,
                            previewParameters[index.value]
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text("Next") },
                            onClick = { index.value = (index.value + 1) % previewParameters.size }
                        )
                    }
                )
            }
        } else {
            setContent {
                ComposableInvoker.invokeComposable(
                    className,
                    methodName,
                    currentComposer,
                    *previewParameters
                )
            }
        }
    }
}
