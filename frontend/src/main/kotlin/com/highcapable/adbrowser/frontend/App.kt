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
package com.highcapable.adbrowser.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import com.highcapable.adbrowser.backend.AppServices
import com.highcapable.adbrowser.backend.utils.OsType
import com.highcapable.adbrowser.frontend.cl.AppState
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.locale.ProvidedLocales
import com.highcapable.adbrowser.frontend.ui.window.MainWindow
import com.highcapable.adbrowser.frontend.ui.window.manager.AppWindow
import com.highcapable.adbrowser.frontend.ui.window.manager.LocalWindowManager
import com.highcapable.adbrowser.frontend.ui.window.manager.WindowManager
import com.highcapable.adbrowser.frontend.ui.window.manager.rememberWindowManager
import com.highcapable.adbrowser.frontend.ui.window.manager.windowRegistries
import kotlinx.coroutines.runBlocking
import java.awt.Desktop

fun main() {
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
        RegisterMacOSAppMenu(windowManager)
        ProvidedLocales(
            settingsLanguageTag = appState.languageTag
        ) {
            RenderWindows()
        }
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

    // Keep the main window always open.
    MainWindow(appState.application::exitApplication)

    // Render all registered windows.
    windowRegistries.forEach {
        if (windowManager.isOpen(it.window)) it.content {
            windowManager.close(it.window)
        }
    }
}