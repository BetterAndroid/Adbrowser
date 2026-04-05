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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.ui.stage.PreferencesStage
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.PreferencesStageModel
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener

@Composable
fun PreferencesWindow(onCloseRequest: () -> Unit) {
    var appHasActiveWindow by remember { 
        mutableStateOf(KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null)
    }

    DisposableEffect(Unit) {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val listener = PropertyChangeListener {
            appHasActiveWindow = focusManager.activeWindow != null
        }
        focusManager.addPropertyChangeListener("activeWindow", listener)

        onDispose {
            focusManager.removePropertyChangeListener("activeWindow", listener)
        }
    }

    Window(
        onCloseRequest = onCloseRequest,
        title = strings.menuPreferences,
        resizable = false,
        alwaysOnTop = appHasActiveWindow,
        state = rememberWindowState(width = 400.dp, height = 550.dp)
    ) {
        val appState = LocalAppState.current
        val viewModel = remember { PreferencesStageModel(appState) }

        AdbrowserTheme(darkTheme = appState.isDarkTheme) {
            PreferencesStage(
                viewModel = viewModel,
                onCloseRequest = onCloseRequest
            )
        }
    }
}