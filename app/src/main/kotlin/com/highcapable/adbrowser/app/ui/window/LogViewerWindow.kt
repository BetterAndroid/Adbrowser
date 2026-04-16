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
package com.highcapable.adbrowser.app.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.cl.LocalAppState
import com.highcapable.adbrowser.app.ui.input.WindowFocusListener
import com.highcapable.adbrowser.app.ui.input.rememberAppHasActiveWindow
import com.highcapable.adbrowser.app.ui.menu.LogViewerMenuBar
import com.highcapable.adbrowser.app.ui.stage.LogViewerStage
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.LogViewerStageModel
import java.awt.Dimension

@Composable
fun LogViewerWindow(onCloseRequest: () -> Unit) {
    val appState = LocalAppState.current
    val viewModel = remember { LogViewerStageModel(appState) }
    val appHasActiveWindow = rememberAppHasActiveWindow()
    var hasWindowFocus by remember { mutableStateOf(true) }

    Window(
        onCloseRequest = onCloseRequest,
        title = strings.menuAppLogs,
        resizable = true,
        alwaysOnTop = appHasActiveWindow,
        state = rememberWindowState(width = 940.dp, height = 620.dp)
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = MinWindowSize
        }
        DisposableEffect(window) {
            hasWindowFocus = window.isFocused

            val listener = WindowFocusListener(
                windowGainedFocus = { hasWindowFocus = true },
                windowLostFocus = { hasWindowFocus = false }
            )
            window.addWindowFocusListener(listener)

            onDispose { window.removeWindowFocusListener(listener) }
        }
        DisposableEffect(viewModel) {
            onDispose { viewModel.dispose() }
        }

        AdbrowserTheme(darkTheme = appState.isDarkTheme) {
            LogViewerMenuBar(
                viewModel = viewModel,
                onCloseRequest = onCloseRequest
            )
            LogViewerStage(
                viewModel = viewModel,
                hasWindowFocus = hasWindowFocus,
                onCloseRequest = onCloseRequest
            )
        }
    }
}

private val MinWindowSize = Dimension(700, 400)