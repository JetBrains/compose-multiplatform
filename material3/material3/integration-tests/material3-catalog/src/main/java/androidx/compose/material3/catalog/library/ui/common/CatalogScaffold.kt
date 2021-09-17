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

package androidx.compose.material3.catalog.library.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.theme.ThemePicker
import androidx.compose.material3.catalog.library.util.GuidelinesUrl
import androidx.compose.material3.catalog.library.util.IssueUrl
import androidx.compose.material3.catalog.library.util.LicensesUrl
import androidx.compose.material3.catalog.library.util.PrivacyUrl
import androidx.compose.material3.catalog.library.util.ReleasesUrl
import androidx.compose.material3.catalog.library.util.SourceUrl
import androidx.compose.material3.catalog.library.util.TermsUrl
import androidx.compose.material3.catalog.library.util.openUrl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

// TODO: Use components/values from Material3 when available
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CatalogScaffold(
    topBarTitle: String,
    showBackNavigationIcon: Boolean = false,
    theme: Theme,
    guidelinesUrl: String = GuidelinesUrl,
    docsUrl: String = ReleasesUrl,
    sourceUrl: String = SourceUrl,
    issueUrl: String = IssueUrl,
    termsUrl: String = TermsUrl,
    privacyUrl: String = PrivacyUrl,
    licensesUrl: String = LicensesUrl,
    onThemeChange: (theme: Theme) -> Unit,
    onBackClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ThemePicker(
                theme = theme,
                onThemeChange = { theme ->
                    coroutineScope.launch {
                        sheetState.hide()
                        onThemeChange(theme)
                    }
                }
            )
        },
        // Default scrim color is onSurface which is incorrect in dark theme
        // https://issuetracker.google.com/issues/183697056
        scrimColor = SheetScrimColor
    ) {
        val context = LocalContext.current
        Scaffold(
            topBar = {
                CatalogTopAppBar(
                    title = topBarTitle,
                    showBackNavigationIcon = showBackNavigationIcon,
                    onBackClick = onBackClick,
                    onThemeClick = { coroutineScope.launch { sheetState.show() } },
                    onGuidelinesClick = { context.openUrl(guidelinesUrl) },
                    onDocsClick = { context.openUrl(docsUrl) },
                    onSourceClick = { context.openUrl(sourceUrl) },
                    onIssueClick = { context.openUrl(issueUrl) },
                    onTermsClick = { context.openUrl(termsUrl) },
                    onPrivacyClick = { context.openUrl(privacyUrl) },
                    onLicensesClick = { context.openUrl(licensesUrl) }
                )
            },
            content = content
        )
    }
}

private val SheetScrimColor = Color.Black.copy(alpha = 0.32f)
