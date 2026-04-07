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
 * This file is created by fankes on 2026/4/7.
 */
package com.highcapable.adbrowser.frontend.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.ui.stage.InitialSetupStage
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.InitialSetupStageModel

@Composable
fun InitialSetupWindow(onCloseRequest: () -> Unit) {
    val appState = LocalAppState.current
    val viewModel = remember { InitialSetupStageModel(appState) }
    val exitApp = appState.application::exitApplication

    Window(
        onCloseRequest = {
            if (viewModel.isBusy) return@Window
            exitApp()
        },
        title = strings.setupTitle,
        resizable = false,
        state = rememberWindowState(
            width = 680.dp,
            height = 290.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        DisposableEffect(viewModel) {
            onDispose { viewModel.dispose() }
        }

        AdbrowserTheme(darkTheme = appState.isDarkTheme) {
            InitialSetupStage(
                viewModel = viewModel,
                onContinue = onCloseRequest,
                onCancel = exitApp
            )
        }
    }
}