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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

val marginSmall: Dp = 10.dp
val marginNormal: Dp = 20.dp
val spacingNormal: Dp = 10.dp

class IoSettingsActivity : ComponentActivity() {
    // This is here to trick the compiler to recompose right after we change the model (otherwise
    // it would skip recomposing).
    val isLoading = mutableStateOf(true)
    val switchesOn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    SettingsScreen(isLoading.value, switchesOn.value)
                }
            }

            if (isLoading.value) {
                // Forces another recomposition right after this one. Simulates a delayed ViewModel
                // that kicks-in later on.
                SideEffect {
                    isLoading.value = false
                    switchesOn.value = true
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isLoading: Boolean,
    switchesOn: Boolean
) {
    Box(Modifier.fillMaxSize()) {
        SettingsScreenContent(
            isLoading = isLoading,
            switchesOn = switchesOn,
            openWebsiteLink = { url -> Log.d("IO", "Open $url") }
        )
    }
}

@Composable
private fun SettingsScreenContent(
    isLoading: Boolean,
    switchesOn: Boolean,
    openWebsiteLink: (String) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxWidth()) {}
    } else {
        Column(Modifier.padding(vertical = marginNormal).verticalScroll(rememberScrollState())) {
            SettingsSection(switchesOn)
            SettingsSection(switchesOn)
            SettingsSection(switchesOn)
            SettingsSection(switchesOn)

            Divider(Modifier.fillMaxWidth())
            Spacer(Modifier.height(spacingNormal))
            AboutSection(openWebsiteLink)
        }
    }
}

@Composable
private fun ColumnScope.SettingsSection(
    switchesOn: Boolean
) {
    val switchModifier = Modifier
        .padding(marginNormal)
        .fillMaxWidth()
    SwitchSetting(
        text = stringResource(R.string.io_settings_time_zone_label),
        checked = switchesOn,
        onCheck = { Log.d("IO", "timezoneCheck") },
        modifier = switchModifier
    )
    SwitchSetting(
        text = stringResource(R.string.io_settings_enable_notifications),
        checked = switchesOn,
        onCheck = { Log.d("IO", "notifCheck") },
        modifier = switchModifier
    )
    SwitchSetting(
        text = stringResource(R.string.io_settings_send_anonymous_usage_statistics),
        checked = switchesOn,
        onCheck = { Log.d("IO", "usageCheck") },
        modifier = switchModifier
    )
}

@Composable
private fun AboutSection(
    openWebsiteLink: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.io_about_title).uppercase(
            LocaleListCompat.getDefault().get(0)!!
        ),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(marginNormal)
    )

    val tosUrl = stringResource(R.string.io_tos_url)
    TextButton(
        modifier = Modifier.padding(marginSmall),
        onClick = {
            openWebsiteLink(tosUrl)
        }
    ) {
        LinkText(stringResource(R.string.io_settings_tos))
    }

    val privacyPolicyUrl = stringResource(R.string.io_privacy_policy_url)
    TextButton(
        modifier = Modifier.padding(marginSmall),
        onClick = {
            openWebsiteLink(privacyPolicyUrl)
        }
    ) {
        LinkText(stringResource(R.string.io_settings_privacy_policy))
    }

    TextButton(
        modifier = Modifier.padding(marginSmall),
        onClick = { Log.d("IO", "Click") }
    ) {
        LinkText(stringResource(R.string.io_settings_oss_licenses))
    }

    Text(
        text = stringResource(R.string.io_version_name, "test"),
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .padding(marginNormal),
        style = MaterialTheme.typography.body2,
    )
}

@Composable
private fun LinkText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.body1
    )
}

@Composable
private fun SwitchSetting(
    text: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            style = MaterialTheme.typography.body2
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary
            )
        )
    }
}
