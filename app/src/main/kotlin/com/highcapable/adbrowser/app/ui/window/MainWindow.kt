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
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.cl.LocalAppState
import com.highcapable.adbrowser.app.ui.input.ComponentAdapter
import com.highcapable.adbrowser.app.ui.input.WindowBounds
import com.highcapable.adbrowser.app.ui.input.currentBounds
import com.highcapable.adbrowser.app.ui.menu.MainMenuBar
import com.highcapable.adbrowser.app.ui.stage.MainStage
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.MainStageModel
import com.highcapable.adbrowser.core.domain.setting.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import java.awt.Dimension

@Composable
fun MainWindow(onCloseRequest: () -> Unit) {
    val appState = LocalAppState.current
    val settingsService = appState.appServices.settingsService
    val settings = settingsService.current
    val initialPosition = remember(settings.mainWindowPosX, settings.mainWindowPosY) {
        settings.savedMainWindowPosition()
    }
    val windowState = rememberWindowState(
        width = settings.mainWindowWidth.dp,
        height = settings.mainWindowHeight.dp,
        position = initialPosition
    )

    Window(
        onCloseRequest = onCloseRequest,
        title = strings.mainTitle,
        state = windowState
    ) {
        val viewModel = remember { MainStageModel(appState) }
        var handledFileListRefreshVersion by remember { mutableStateOf(appState.fileListRefreshVersion) }
        var liveWindowBounds by remember { mutableStateOf<WindowBounds?>(null) }

        LaunchedEffect(Unit) {
            window.minimumSize = MinWindowSize
        }
        DisposableEffect(window) {
            val adapter = ComponentAdapter(
                componentMoved = {
                    liveWindowBounds = window.currentBounds()
                },
                componentResized = {
                    liveWindowBounds = window.currentBounds()
                }
            )

            window.addComponentListener(adapter)
            // Seed the current bounds immediately so a no-op session still has a valid baseline.
            liveWindowBounds = window.currentBounds()

            onDispose {
                window.removeComponentListener(adapter)
            }
        }
        LaunchedEffect(viewModel) { viewModel.initialize() }
        LaunchedEffect(appState.settingsSyncVersion) {
            val current = settingsService.current
            val targetSize = DpSize(current.mainWindowWidth.dp, current.mainWindowHeight.dp)
            if (windowState.size != targetSize) windowState.size = targetSize

            // Position is reapplied from settings sync as well so a reset from Preferences can
            // immediately re-center the existing window instead of only affecting the next launch.
            windowState.position = current.savedMainWindowPosition()
        }
        LaunchedEffect(appState.settingsSyncVersion, appState.fileListRefreshVersion) {
            val refreshFileList = appState.fileListRefreshVersion != handledFileListRefreshVersion
            if (refreshFileList) handledFileListRefreshVersion = appState.fileListRefreshVersion
            viewModel.onExternalSettingsChanged(refreshFileList = refreshFileList)
        }
        LaunchedEffect(Unit) {
            snapshotFlow { liveWindowBounds }
                .filterNotNull()
                .filter { bounds -> bounds.width > 0 && bounds.height > 0 }
                .distinctUntilChanged()
                .debounce(250)
                .collect { bounds ->
                    // Persist real AWT bounds instead of relying on Compose state snapshots only.
                    // The native window can move/resize outside Compose's immediate awareness.
                    settingsService.current.mainWindowWidth = bounds.width.toDouble()
                    settingsService.current.mainWindowHeight = bounds.height.toDouble()
                    settingsService.current.mainWindowPosX = bounds.x.toDouble()
                    settingsService.current.mainWindowPosY = bounds.y.toDouble()

                    withContext(Dispatchers.IO) {
                        settingsService.save()
                    }
                }
        }
        DisposableEffect(viewModel) {
            onDispose { viewModel.dispose() }
        }

        AdbrowserTheme(darkTheme = appState.isDarkTheme) {
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

/**
 * Converts persisted settings into an initial Compose window position.
 *
 * A null persisted position means "center on screen", which is also how the reset action is
 * represented in settings.
 */
private fun AppSettings.savedMainWindowPosition(): WindowPosition {
    val mainWindowPosX = mainWindowPosX
    val mainWindowPosY = mainWindowPosY

    return if (mainWindowPosX != null && mainWindowPosY != null)
        WindowPosition(mainWindowPosX.dp, mainWindowPosY.dp)
    else WindowPosition.Aligned(Alignment.Center)
}

private val MinWindowSize = Dimension(900, 400)