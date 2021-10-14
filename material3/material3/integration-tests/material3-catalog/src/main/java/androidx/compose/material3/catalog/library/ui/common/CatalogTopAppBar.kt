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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding

// TODO: Use components/values from Material3 when available
@Composable
fun CatalogTopAppBar(
    title: String,
    showBackNavigationIcon: Boolean = false,
    onBackClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onGuidelinesClick: () -> Unit = {},
    onDocsClick: () -> Unit = {},
    onSourceClick: () -> Unit = {},
    onIssueClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {}
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val backgroundColor = MaterialTheme.colorScheme.background
    // Wrapping in a Surface to handle window insets
    // https://issuetracker.google.com/issues/183161866
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppBarDefaults.TopAppBarElevation,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = {
                Box {
                    Row {
                        IconButton(onClick = onThemeClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette_24dp),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                    }
                    MoreMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                        onGuidelinesClick = {
                            onGuidelinesClick()
                            moreMenuExpanded = false
                        },
                        onDocsClick = {
                            onDocsClick()
                            moreMenuExpanded = false
                        },
                        onSourceClick = {
                            onSourceClick()
                            moreMenuExpanded = false
                        },
                        onIssueClick = {
                            onIssueClick()
                            moreMenuExpanded = false
                        },
                        onTermsClick = {
                            onTermsClick()
                            moreMenuExpanded = false
                        },
                        onPrivacyClick = {
                            onPrivacyClick()
                            moreMenuExpanded = false
                        },
                        onLicensesClick = {
                            onLicensesClick()
                            moreMenuExpanded = false
                        }
                    )
                }
            },
            navigationIcon = if (showBackNavigationIcon) {
                {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            } else {
                null
            },
            backgroundColor = backgroundColor,
            contentColor = contentColorFor(backgroundColor),
            elevation = 0.dp,
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(bottom = false)
        )
    }
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onGuidelinesClick: () -> Unit,
    onDocsClick: () -> Unit,
    onSourceClick: () -> Unit,
    onIssueClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLicensesClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        DropdownMenuItem(onClick = onGuidelinesClick) {
            Text(stringResource(id = R.string.view_design_guidelines))
        }
        DropdownMenuItem(onClick = onDocsClick) {
            Text(stringResource(id = R.string.view_developer_docs))
        }
        DropdownMenuItem(onClick = onSourceClick) {
            Text(stringResource(id = R.string.view_source_code))
        }
        Divider()
        DropdownMenuItem(onClick = onIssueClick) {
            Text(stringResource(id = R.string.report_an_issue))
        }
        Divider()
        DropdownMenuItem(onClick = onTermsClick) {
            Text(stringResource(id = R.string.terms_of_service))
        }
        DropdownMenuItem(onClick = onPrivacyClick) {
            Text(stringResource(id = R.string.privacy_policy))
        }
        DropdownMenuItem(onClick = onLicensesClick) {
            Text(stringResource(id = R.string.open_source_licenses))
        }
    }
}
