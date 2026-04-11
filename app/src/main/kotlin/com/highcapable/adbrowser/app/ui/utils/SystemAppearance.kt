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
 * This file is created by fankes on 2026/4/6.
 */
package com.highcapable.adbrowser.app.ui.utils

import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.core.common.utils.CommandRunner
import com.highcapable.adbrowser.core.common.utils.OsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Detects the system appearance (light/dark mode) on different platforms.
 */
object SystemAppearance {

    val isDarkMode get() = when {
        OsType.isMacOS -> checkMacOSDarkMode()
        OsType.isWindows -> checkWindowsDarkMode()
        OsType.isLinux -> checkLinuxDarkMode()
        else -> false
    }

    /** Initializes platform-specific properties to enable native light/dark mode support where possible. */
    fun initialize() {
        if (!OsType.isMacOS) return

        // Let macOS native window appearance follow system light/dark mode dynamically.
        System.setProperty("apple.awt.application.appearance", "system")
    }

    /** Continuously checks the system appearance and updates the app theme accordingly. */
    suspend fun startListening(appState: AppState) {
        while (true) {
            val isDarkMode = withContext(Dispatchers.IO) { isDarkMode }
            appState.syncTheme(isDarkMode)

            // Check for appearance changes every 50 milliseconds.
            delay(50L)
        }
    }

    private fun checkMacOSDarkMode(): Boolean {
        val output = CommandRunner.exec("defaults", "read", "-g", "AppleInterfaceStyle") ?: return false
        return output.contains("Dark", ignoreCase = true)
    }

    private fun checkWindowsDarkMode(): Boolean {
        val output = CommandRunner.exec(
            "reg",
            "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v",
            "AppsUseLightTheme"
        ) ?: return false
        val value = output.lineSequence()
            .firstOrNull { it.contains("AppsUseLightTheme", ignoreCase = true) }
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.lastOrNull()
            ?.lowercase()
            ?: return false

        return value == "0x0" || value == "0"
    }

    private fun checkLinuxDarkMode(): Boolean {
        val colorScheme = CommandRunner.exec("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
        if (colorScheme?.contains("prefer-dark", ignoreCase = true) == true) return true

        val gtkTheme = CommandRunner.exec("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
        return gtkTheme?.contains("dark", ignoreCase = true) == true
    }
}