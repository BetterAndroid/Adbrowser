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
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.ui.stage.MainMenuBar
import com.highcapable.adbrowser.frontend.ui.stage.MainStage
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel

@Composable
fun MainWindow(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = strings.mainTitle,
        state = rememberWindowState(width = 1300.dp, height = 820.dp)
    ) {
        val appState = LocalAppState.current
        val viewModel = remember { MainStageModel(appState) }

        AdbrowserTheme {
            MainMenuBar(
                viewModel = viewModel,
                onCloseRequest = onCloseRequest
            )
            MainStage(
                viewModel = viewModel,
                onCloseRequest = onCloseRequest
            )
        }
    }
}