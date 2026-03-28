/*
 * Adbrowser - A modern cross-platform Android file manager powered by ADB.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/Adbrowser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2025/6/4.
 */
package com.highcapable.adbrowser.frontend.ui.window

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.state.AppPreferences
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme

@Composable
fun PreferencesWindow(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = strings.preferences,
        resizable = false,
        state = rememberWindowState(width = 450.dp, height = 600.dp)
    ) {
        AdbrowserTheme {
            PreferencesScreen()
        }
    }
}

@Composable
private fun FrameWindowScope.PreferencesScreen() {
    val appState = LocalAppState.current
    var currentTab by remember { mutableStateOf(0) }
    var draft by remember(appState.preferences) { mutableStateOf(appState.preferences) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = currentTab) {
            Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text(strings.general) })
            Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text(strings.device) })
        }

        when (currentTab) {
            0 -> GeneralSettingsTab(
                preferences = draft,
                onUpdate = { draft = it }
            )

            1 -> DeviceSettingsTab(
                preferences = draft,
                onUpdate = { draft = it }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                appState.updatePreferences(draft)
                appState.appendAppLog("Preferences saved")
            }) {
                Text(strings.save)
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    preferences: AppPreferences,
    onUpdate: (AppPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = preferences.languageTag,
            onValueChange = { onUpdate(preferences.copy(languageTag = it.trim())) },
            label = { Text(strings.language) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Supported: en, zh-CN",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviceSettingsTab(
    preferences: AppPreferences,
    onUpdate: (AppPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = preferences.adbExecutablePath,
            onValueChange = { onUpdate(preferences.copy(adbExecutablePath = it)) },
            label = { Text(strings.adbPathLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.rememberLastDevice)
            Switch(
                checked = preferences.rememberLastDevice,
                onCheckedChange = { onUpdate(preferences.copy(rememberLastDevice = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.rememberDevicePath)
            Switch(
                checked = preferences.rememberDevicePath,
                onCheckedChange = { onUpdate(preferences.copy(rememberDevicePath = it)) }
            )
        }
    }
}