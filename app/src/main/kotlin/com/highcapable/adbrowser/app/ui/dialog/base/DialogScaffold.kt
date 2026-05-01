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
 * This file is created by fankes on 2026/4/5.
 */
package com.highcapable.adbrowser.app.ui.dialog.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.rememberDialogState
import com.highcapable.adbrowser.app.ui.component.WindowButtonsSpacing
import com.highcapable.adbrowser.app.ui.component.WindowTitleBar
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.utils.LookAndFeel
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import java.awt.Window

@Composable
fun DialogScaffold(
    title: String,
    onCloseRequest: () -> Unit,
    ownerWindow: Window? = null,
    width: Dp = 460.dp,
    height: Dp = Dp.Unspecified,
    verticalSpacing: Dp = 12.dp,
    contentPadding: ComponentPadding = ComponentPadding(20.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    titleBarBackgroundColor: Color = Color.Transparent,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default,
    content: @Composable DialogWindowScope.() -> Unit
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        title = title,
        resizable = false,
        state = rememberDialogState(width = width, height = height)
    ) {
        val colors = AdbrowserTheme.colors

        LaunchedEffect(Unit) {
            LookAndFeel.applyMacOSImmersiveTitleBarStyle(window)
        }
        LaunchedEffect(window, ownerWindow) {
            if (ownerWindow == null) return@LaunchedEffect

            // DialogWindow centers by screen by default. Waiting until the first frame is laid out
            // lets AWT know the dialog's final measured size before we center it over the owner window.
            withFrameNanos {}
            window.setLocationRelativeTo(ownerWindow)
        }

        Column(
            modifier = Modifier
                .background(colors.mainBackground)
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            horizontalAlignment = horizontalAlignment
        ) {
            // If the window title bar is available,
            // we need to remove the top padding to avoid extra space between the title bar and the content.
            val compatiblePadding = contentPadding.copy(
                top = if (WindowTitleBar.isAvailable) 0.dp else contentPadding.top
            )

            WindowTitleBar(
                title = title,
                backgroundColor = titleBarBackgroundColor,
                buttonsSpacing = buttonsSpacing
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(compatiblePadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                horizontalAlignment = horizontalAlignment
            ) {
                content()
            }
        }
    }
}