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
package com.highcapable.adbrowser.frontend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.ui.component.styling.ButtonMetrics
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.component.styling.DividerMetrics
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import org.jetbrains.jewel.ui.component.styling.LocalDefaultButtonStyle
import org.jetbrains.jewel.ui.component.styling.LocalOutlinedButtonStyle
import org.jetbrains.jewel.ui.component.styling.TabColors
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.theme.defaultButtonStyle
import org.jetbrains.jewel.ui.theme.defaultTabStyle
import org.jetbrains.jewel.ui.theme.dividerStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle

@Immutable
data class AdbrowserColorScheme(
    val mainBackground: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val primaryAccent: Color,
    val primaryAccentHover: Color,
    val primaryAccentPressed: Color,
    val hintBackground: Color,
    val hintBorder: Color,
    val subtleControlBackground: Color,
    val pathBreadcrumbForeground: Color,
    val fileHintForeground: Color
)

private val LightColors = AdbrowserColorScheme(
    mainBackground = Color(0xFFF3F4F6),
    panelBackground = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFD6D9DE),
    primaryAccent = Color(0xFF2E6CD3),
    primaryAccentHover = Color(0xFF265BB2),
    primaryAccentPressed = Color(0xFF1F4D98),
    hintBackground = Color(0xFFF8FAFC),
    hintBorder = Color(0xFFE3E3E3),
    subtleControlBackground = Color(0xFFF7F8FB),
    pathBreadcrumbForeground = Color(0xFF6B7280),
    fileHintForeground = Color(0xFF97B6E9)
)

private val DarkColors = AdbrowserColorScheme(
    mainBackground = Color(0xFF161A20),
    panelBackground = Color(0xFF242B35),
    panelBorder = Color(0xFF3A4453),
    primaryAccent = Color(0xFF4D8FF6),
    primaryAccentHover = Color(0xFF3F7FDF),
    primaryAccentPressed = Color(0xFF336FC7),
    hintBackground = Color(0xFF1E293B),
    hintBorder = Color(0xFF334155),
    subtleControlBackground = Color(0xFF2A3240),
    pathBreadcrumbForeground = Color(0xFF9CA3AF),
    fileHintForeground = Color(0xFF27487B)
)

private val LocalColors = staticCompositionLocalOf { LightColors }

object AdbrowserTheme {

    val colors @Composable get() = LocalColors.current

    @Composable
    fun defaultButtonStyle(padding: PaddingValues = DefaultButtonPadding) = ButtonStyle(
        colors = JewelTheme.defaultButtonStyle.colors,
        metrics = ButtonMetrics(
            cornerSize = JewelTheme.defaultButtonStyle.metrics.cornerSize,
            padding = padding,
            minSize = JewelTheme.defaultButtonStyle.metrics.minSize,
            borderWidth = JewelTheme.defaultButtonStyle.metrics.borderWidth,
            focusOutlineExpand = JewelTheme.defaultButtonStyle.metrics.focusOutlineExpand
        ),
        focusOutlineAlignment = JewelTheme.defaultButtonStyle.focusOutlineAlignment
    )

    @Composable
    fun outlineButtonStyle(padding: PaddingValues = DefaultButtonPadding) = ButtonStyle(
        colors = JewelTheme.outlinedButtonStyle.colors,
        metrics = ButtonMetrics(
            cornerSize = JewelTheme.outlinedButtonStyle.metrics.cornerSize,
            padding = padding,
            minSize = JewelTheme.outlinedButtonStyle.metrics.minSize,
            borderWidth = JewelTheme.outlinedButtonStyle.metrics.borderWidth,
            focusOutlineExpand = JewelTheme.outlinedButtonStyle.metrics.focusOutlineExpand
        ),
        focusOutlineAlignment = JewelTheme.outlinedButtonStyle.focusOutlineAlignment
    )

    val defaultTabStyle @Composable get() = run {
        val baseTabStyle = JewelTheme.defaultTabStyle
        val baseColors = baseTabStyle.colors
        val baseMetrics = baseTabStyle.metrics

        TabStyle(
            colors = TabColors(
                background = Color.Transparent,
                backgroundDisabled = Color.Transparent,
                backgroundPressed = baseColors.backgroundPressed,
                backgroundHovered = baseColors.backgroundHovered,
                backgroundSelected = Color.Transparent,
                content = baseColors.content,
                contentDisabled = baseColors.contentDisabled,
                contentPressed = baseColors.contentPressed,
                contentHovered = baseColors.contentHovered,
                contentSelected = baseColors.contentSelected,
                underline = baseColors.underline,
                underlineDisabled = baseColors.underlineDisabled,
                underlinePressed = baseColors.underlinePressed,
                underlineHovered = baseColors.underlineHovered,
                underlineSelected = baseColors.underlineSelected
            ),
            metrics = baseMetrics,
            icons = baseTabStyle.icons,
            contentAlpha = baseTabStyle.contentAlpha,
            scrollbarStyle = baseTabStyle.scrollbarStyle
        )
    }

    @Composable
    fun dividerStyle(thickness: Dp, color: Color = Color.Transparent) = DividerStyle(
        color = color,
        metrics = DividerMetrics(
            thickness = thickness,
            startIndent = JewelTheme.dividerStyle.metrics.startIndent
        )
    )

    val DefaultButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
    val DefaultTextFieldHeight = 30.dp
}

@Composable
fun AdbrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    IntUiTheme(isDark = darkTheme) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalDefaultButtonStyle provides AdbrowserTheme.defaultButtonStyle(),
            LocalOutlinedButtonStyle provides AdbrowserTheme.outlineButtonStyle()
        ) {
            content()
        }
    }
}