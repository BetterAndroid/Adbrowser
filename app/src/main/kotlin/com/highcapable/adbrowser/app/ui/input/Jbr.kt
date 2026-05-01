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
 * This file is created by fankes on 2026/5/1.
 */
package com.highcapable.adbrowser.app.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window

/**
 * Remembers a JBR custom title bar for the given host window.
 *
 * The handle is window-scoped because JBR associates native drag and inset state with the host
 * frame itself, so recreating it on every recomposition would only create redundant bridge
 * objects with no benefit.
 */
@Composable
fun rememberJbrCustomTitleBar(window: Window) = remember(window) { createJbrCustomTitleBarOrNull() }

/**
 * Applies the requested custom title-bar height to the current frame window.
 *
 * This mirrors the contract Jewel uses on macOS and Windows: Compose owns the visual layer while
 * JBR keeps the host window's native draggable/title-bar region aligned with that layout height.
 */
fun applyJbrCustomTitleBar(window: Window, customTitleBar: WindowDecorations.CustomTitleBar?, height: Dp) {
    customTitleBar ?: return

    customTitleBar.height = height.value
    when (window) {
        is Frame -> JBR.getWindowDecorations().setCustomTitleBar(window, customTitleBar)
        is Dialog -> JBR.getWindowDecorations().setCustomTitleBar(window, customTitleBar)
    }
}

/**
 * Bridges Compose pointer dispatch back into JBR's native title-bar hit testing.
 *
 * Once a custom title bar is installed, JBR needs explicit guidance about whether the current
 * pointer event belongs to app controls or to the host window chrome. Unconsumed events are
 * treated as draggable title-bar space, while consumed events temporarily disable native hit
 * testing so controls keep normal interaction behavior.
 */
fun Modifier.customTitleBarMouseEventHandler(titleBar: WindowDecorations.CustomTitleBar?) =
    if (titleBar == null) this else pointerInput(titleBar) {
        val currentContext = currentCoroutineContext()
        awaitPointerEventScope {
            var inUserControl = false
            while (currentContext.isActive) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach {
                    if (!it.isConsumed && !inUserControl) {
                        titleBar.forceHitTest(false)
                    } else {
                        if (event.type == PointerEventType.Press) inUserControl = true
                        if (event.type == PointerEventType.Release) inUserControl = false
                        titleBar.forceHitTest(true)
                    }
                }
            }
        }
    }

private fun createJbrCustomTitleBarOrNull(): WindowDecorations.CustomTitleBar? =
    if (JBR.isWindowDecorationsSupported()) JBR.getWindowDecorations().createCustomTitleBar() else null