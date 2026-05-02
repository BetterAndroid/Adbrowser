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
 * This file is created by fankes on 2026/4/30.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.FrameWindowScope
import com.highcapable.adbrowser.app.ui.platform.jbr.JBR
import com.highcapable.adbrowser.app.ui.platform.jbr.applyJbrCustomTitleBar
import com.highcapable.adbrowser.app.ui.platform.jbr.customTitleBarMouseEventHandler
import com.highcapable.adbrowser.app.ui.platform.jbr.rememberJbrCustomTitleBar
import com.highcapable.adbrowser.core.common.utils.OsType
import org.jetbrains.jewel.ui.component.Text
import java.awt.Window
import javax.swing.RootPaneContainer

/**
 * Reserves a macOS custom title-bar area inside the Compose content tree.
 *
 * The visual height is fixed to match the selected traffic-light spacing preset, while JBR keeps
 * the native title-bar contract in sync so drag/zoom behavior still comes from the system.
 */
@Composable
fun FrameWindowScope.WindowTitleBar(
    modifier: Modifier = Modifier,
    title: String = "",
    backgroundColor: Color = Color.Transparent,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default
) {
    WindowTitleBarContent(
        hostWindow = window,
        rootPaneContainer = window,
        modifier = modifier,
        title = title,
        backgroundColor = backgroundColor,
        buttonsSpacing = buttonsSpacing
    )
}

/**
 * Reserves a macOS custom title-bar area inside the Compose content tree.
 *
 * The visual height is fixed to match the selected traffic-light spacing preset, while JBR keeps
 * the native title-bar contract in sync so drag/zoom behavior still comes from the system.
 */
@Composable
fun DialogWindowScope.WindowTitleBar(
    modifier: Modifier = Modifier,
    title: String = "",
    backgroundColor: Color = Color.Transparent,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default
) {
    WindowTitleBarContent(
        hostWindow = window,
        rootPaneContainer = window,
        modifier = modifier,
        title = title,
        backgroundColor = backgroundColor,
        buttonsSpacing = buttonsSpacing
    )
}

@Composable
private fun WindowTitleBarContent(
    hostWindow: Window,
    rootPaneContainer: RootPaneContainer,
    modifier: Modifier = Modifier,
    title: String = "",
    backgroundColor: Color = Color.Transparent,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default
) {
    if (!WindowTitleBar.isAvailable) return

    val titleBarHeight = buttonsSpacing.height
    val customTitleBar = rememberJbrCustomTitleBar(hostWindow)

    DisposableEffect(hostWindow, buttonsSpacing, titleBarHeight, customTitleBar) {
        rootPaneContainer.rootPane.putClientProperty(MacOSWindowButtonsSpacingKey, buttonsSpacing.value)
        applyJbrCustomTitleBar(hostWindow, customTitleBar, titleBarHeight)
        onDispose {}
    }

    Box(
        modifier = modifier
            .customTitleBarMouseEventHandler(customTitleBar)
            .fillMaxWidth()
            .background(backgroundColor)
            .height(titleBarHeight)
    ) {
        if (title.isNotBlank())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(titleBarHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
            }
    }
}

/**
 * A marker for the WindowTitleBar.
 */
object WindowTitleBar {

    /**
     * Whether the WindowTitleBar component is available on the current platform.
     */
    val isAvailable get() = OsType.isMacOS && JBR.isAvailable
}

/**
 * Controls the spacing between the macOS window buttons and the content area.
 */
enum class WindowButtonsSpacing(val value: String, val height: Dp) {
    Default("default", MacOSDefaultTitleBarHeight),
    Medium("medium", MacOSMediumTitleBarHeight),
    Large("large", MacOSLargeTitleBarHeight)
}

private const val MacOSWindowButtonsSpacingKey = "FlatLaf.macOS.windowButtonsSpacing"

private val MacOSDefaultTitleBarHeight = 28.dp
private val MacOSMediumTitleBarHeight = 40.dp
private val MacOSLargeTitleBarHeight = 50.dp