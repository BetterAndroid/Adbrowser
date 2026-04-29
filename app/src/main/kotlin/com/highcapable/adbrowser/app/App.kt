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
package com.highcapable.adbrowser.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.cl.LocalAppState
import com.highcapable.adbrowser.app.locale.ProvidedLocales
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.utils.LookAndFeel
import com.highcapable.adbrowser.app.ui.utils.SystemAppearance
import com.highcapable.adbrowser.app.ui.window.manager.AppWindow
import com.highcapable.adbrowser.app.ui.window.manager.LocalWindowManager
import com.highcapable.adbrowser.app.ui.window.manager.WindowManager
import com.highcapable.adbrowser.app.ui.window.manager.rememberWindowManager
import com.highcapable.adbrowser.app.ui.window.manager.windowRegistries
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.domain.AppServices
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.menuStyle
import java.awt.Desktop

fun main() {
    SystemAppearance.initialize()
    LookAndFeel.initialize(SystemAppearance.isDarkMode)

    // Initialize all backend services before the UI event loop starts.
    // initialize() only does disk IO (load settings) and object construction,
    // so blocking the main thread here is safe and avoids any Window lifecycle issues.
    val services = AppServices()
    runBlocking { services.initialize() }
    runApp(services)
}

private fun runApp(services: AppServices) = application {
    val appState = remember { AppState(application = this, services) }
    val windowManager = rememberWindowManager()

    CompositionLocalProvider(
        LocalAppState provides appState,
        LocalWindowManager provides windowManager
    ) {
        AdbrowserTheme(darkTheme = appState.isDarkTheme) {
            SyncSystemAppearance(appState)
            SyncLookAndFeel(appState)
            RegisterMacOSAppMenu(windowManager)

            ProvidedLocales(
                settingsLanguageTag = appState.languageTag
            ) {
                RenderWindows()
            }
        }
    }
}

@Composable
private fun SyncSystemAppearance(appState: AppState) {
    LaunchedEffect(appState) {
        SystemAppearance.startListening(appState)
    }
}

@Composable
private fun SyncLookAndFeel(appState: AppState) {
    val uiStyle = LookAndFeel.UiStyle(
        colors = AdbrowserTheme.colors,
        contentColor = JewelTheme.contentColor,
        menuStyle = JewelTheme.menuStyle
    )

    LaunchedEffect(appState.isDarkTheme) {
        LookAndFeel.sync(appState.isDarkTheme, uiStyle)
    }
}

@Composable
private fun RegisterMacOSAppMenu(windowManager: WindowManager) {
    if (!OsType.isMacOS || !Desktop.isDesktopSupported()) return

    DisposableEffect(windowManager) {
        runCatching {
            Desktop.getDesktop().takeIf {
                it.isSupported(Desktop.Action.APP_PREFERENCES)
            }?.setPreferencesHandler {
                windowManager.open(AppWindow.Preferences)
            }
        }

        // No cleanup needed, the handler will be automatically garbage collected when the app exits.
        onDispose {}
    }
}

@Composable
private fun RenderWindows() {
    val appState = LocalAppState.current
    val windowManager = LocalWindowManager.current

    windowManager.open(
        if (appState.appServices.settingsService.current.adbExecPath.isBlank())
            AppWindow.InitialSetup
        else AppWindow.Main
    )

    // Render all registered windows.
    windowRegistries.forEach {
        if (windowManager.isOpen(it.window)) it.content {
            // If the current window is Main, exit the app on close request.
            // Otherwise, just close the window.
            if (it.window != AppWindow.Main)
                windowManager.close(it.window)
            else appState.application.exitApplication()
        }
    }
}