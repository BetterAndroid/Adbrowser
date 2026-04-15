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
 * This file is created by fankes on 2026/4/15.
 */
package com.highcapable.adbrowser.app.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.awt.KeyboardFocusManager
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.beans.PropertyChangeListener

/**
 * Creates a [WindowFocusListener] backed by lambdas, mirroring the small adapter helpers used for
 * component events in this package.
 */
fun WindowFocusListener(
    windowGainedFocus: (WindowEvent?) -> Unit = {},
    windowLostFocus: (WindowEvent?) -> Unit = {}
) = object : WindowFocusListener {

    override fun windowGainedFocus(event: WindowEvent?) {
        windowGainedFocus(event)
    }

    override fun windowLostFocus(event: WindowEvent?) {
        windowLostFocus(event)
    }
}

/**
 * Returns whether any window belonging to this app is currently active.
 *
 * Compose Desktop does not provide an "always on top within this app only" flag, so windows that
 * want that behavior can bridge to AWT and treat "some app window is active" as the signal for
 * enabling `alwaysOnTop`.
 */
@Composable
fun rememberAppHasActiveWindow(): Boolean {
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

    return appHasActiveWindow
}