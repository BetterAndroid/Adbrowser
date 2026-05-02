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
 * This file is created by fankes on 2026/5/3.
 */
package com.highcapable.adbrowser.app.ui.platform.jbr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.jetbrains.WindowDecorations
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window
import com.jetbrains.JBR as JetBrainsRuntime

/**
 * Remembers a JBR custom title bar for the given host window.
 *
 * The returned handle is window-scoped because native title-bar state is owned by the backing AWT
 * window rather than by the Compose tree that happens to render inside it.
 */
@Composable
fun rememberJbrCustomTitleBar(window: Window) = remember(window) { createJbrCustomTitleBarOrNull() }

/**
 * Applies the requested custom title-bar height to the current host window through JBR.
 *
 * Both frame windows and dialog windows can participate in the same custom title-bar bridge, so
 * the overload dispatch stays here instead of being repeated by every caller.
 */
fun applyJbrCustomTitleBar(window: Window, customTitleBar: WindowDecorations.CustomTitleBar?, height: Dp) {
    customTitleBar ?: return

    customTitleBar.height = height.value
    when (window) {
        is Frame -> JetBrainsRuntime.getWindowDecorations().setCustomTitleBar(window, customTitleBar)
        is Dialog -> JetBrainsRuntime.getWindowDecorations().setCustomTitleBar(window, customTitleBar)
    }
}

private fun createJbrCustomTitleBarOrNull(): WindowDecorations.CustomTitleBar? {
    if (!JBR.isAvailable) return null
    if (!JetBrainsRuntime.isWindowDecorationsSupported()) return null
    return JetBrainsRuntime.getWindowDecorations().createCustomTitleBar()
}