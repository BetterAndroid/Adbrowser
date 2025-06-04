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
package com.highcapable.adbrowser.frontend.ui.window.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class WindowManager {

    private val _openWindows = mutableStateMapOf<AppWindow, Boolean>()

    fun isOpen(window: AppWindow): Boolean = _openWindows[window] == true

    fun open(window: AppWindow) {
        _openWindows[window] = true
    }

    fun close(window: AppWindow) {
        _openWindows[window] = false
    }

    val openWindows get() = _openWindows
}

@Composable
fun rememberWindowManager() = remember { WindowManager() }

val LocalWindowManager = staticCompositionLocalOf<WindowManager> {
    error("No WindowManager provided")
}