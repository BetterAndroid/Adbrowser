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
 * This file is created by fankes on 2026/4/20.
 */
package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.awt.KeyEventPostProcessor
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

@Composable
fun ProvidePrimaryAction(window: Window, content: @Composable () -> Unit) {
    val primaryActionController = remember { PrimaryActionController() }

    DisposableEffect(window, primaryActionController) {
        val keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val postProcessor = KeyEventPostProcessor { event ->
            if (event.id != KeyEvent.KEY_PRESSED || event.isConsumed) return@KeyEventPostProcessor false
            if (event.keyCode != KeyEvent.VK_ENTER) return@KeyEventPostProcessor false

            val eventWindow = event.component?.let(SwingUtilities::getWindowAncestor)
            if (eventWindow != window && keyboardFocusManager.focusedWindow != window) return@KeyEventPostProcessor false

            val action = primaryActionController.onPrimary
            if (!primaryActionController.primaryEnabled || action == null) return@KeyEventPostProcessor false

            event.consume()
            action()

            true
        }

        keyboardFocusManager.addKeyEventPostProcessor(postProcessor)
        onDispose { keyboardFocusManager.removeKeyEventPostProcessor(postProcessor) }
    }

    CompositionLocalProvider(LocalPrimaryActionController provides primaryActionController) {
        content()
    }
}

/**
 * Provides a way to control the primary action of a window or dialog.
 */
val LocalPrimaryActionController = compositionLocalOf<PrimaryActionController?> { null }

@Stable
class PrimaryActionController {

    var onPrimary by mutableStateOf<(() -> Unit)?>(null)
        private set

    var primaryEnabled by mutableStateOf(false)
        private set

    fun update(action: () -> Unit, enabled: Boolean) {
        onPrimary = action
        primaryEnabled = enabled
    }

    fun clear() {
        onPrimary = null
        primaryEnabled = false
    }
}