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
package com.highcapable.adbrowser.app.ui.window.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.highcapable.adbrowser.app.ui.component.WindowButtonsSpacing
import com.highcapable.adbrowser.app.ui.component.WindowTitleBar
import com.highcapable.adbrowser.app.ui.input.rememberWindowDecorationsVisible
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.utils.LookAndFeel

@Composable
fun WindowScaffold(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String,
    icon: Painter? = null,
    titleIsVisible: Boolean = true,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    titleBarBackgroundColor: Color = Color.Transparent,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default,
    titleLeftContent: @Composable (RowScope.() -> Unit)? = null,
    titleRightContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable FrameWindowScope.(decorationsVisible: Boolean) -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        undecorated = undecorated,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent
    ) {
        val colors = AdbrowserTheme.colors
        val isWindowDecorationsVisible = rememberWindowDecorationsVisible(window)

        LaunchedEffect(Unit) {
            LookAndFeel.applyMacOSImmersiveTitleBarStyle(window)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.mainBackground)
        ) {
            if (isWindowDecorationsVisible)
                WindowTitleBar(
                    title = title,
                    titleIsVisible = titleIsVisible,
                    leftContent = titleLeftContent,
                    rightContent = titleRightContent,
                    backgroundColor = titleBarBackgroundColor,
                    buttonsSpacing = buttonsSpacing
                )
            content(isWindowDecorationsVisible)
        }
    }
}