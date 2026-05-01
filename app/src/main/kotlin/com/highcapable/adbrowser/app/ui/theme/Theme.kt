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
package com.highcapable.adbrowser.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.styling.Default
import org.jetbrains.jewel.intui.standalone.styling.Editor
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.dark
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.light
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.LocalTypography
import org.jetbrains.jewel.ui.component.styling.ButtonColors
import org.jetbrains.jewel.ui.component.styling.ButtonMetrics
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.component.styling.DividerMetrics
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import org.jetbrains.jewel.ui.component.styling.LocalDefaultButtonStyle
import org.jetbrains.jewel.ui.component.styling.LocalOutlinedButtonStyle
import org.jetbrains.jewel.ui.component.styling.TabColors
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.component.styling.TextAreaMetrics
import org.jetbrains.jewel.ui.component.styling.TextAreaStyle
import org.jetbrains.jewel.ui.theme.defaultButtonStyle
import org.jetbrains.jewel.ui.theme.defaultTabStyle
import org.jetbrains.jewel.ui.theme.dividerStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle
import org.jetbrains.jewel.ui.theme.textAreaStyle

object AdbrowserTheme {

    val colors @Composable get() = LocalColors.current

    @Composable
    fun defaultButtonStyle(padding: ComponentPadding = DefaultButtonPadding) = ButtonStyle(
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
    fun outlineButtonStyle(padding: ComponentPadding = DefaultButtonPadding) = ButtonStyle(
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

    val undecoratedTextAreaStyle @Composable get() = TextAreaStyle(
        colors = JewelTheme.textAreaStyle.colors,
        metrics = TextAreaMetrics(
            borderWidth = JewelTheme.textAreaStyle.metrics.borderWidth,
            contentPadding = ComponentPadding.None,
            cornerSize = JewelTheme.textAreaStyle.metrics.cornerSize,
            minSize = DpSize.Zero
        )
    )

    val DefaultButtonPadding = ComponentPadding(horizontal = 10.dp, vertical = 5.dp)
    val DefaultTextFieldHeight = 30.dp

    val DefaultItemFontSize = 14.sp
}

@Composable
fun AdbrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val fontFamily = remember { FontFamily() }
    val defaultTextStyle = remember(fontFamily) {
        if (fontFamily != null) 
            JewelTheme.createDefaultTextStyle(
                fontFamily = fontFamily,
                fontSynthesis = FontSynthesis.All
            )
        else 
            JewelTheme.createDefaultTextStyle(
                fontSynthesis = FontSynthesis.All
            )
    }
    val typography = remember(fontFamily) {
        AdbrowserTypography()
    }

    val themeDefinition = remember(darkTheme, defaultTextStyle) {
        if (darkTheme)
            JewelTheme.darkThemeDefinition(defaultTextStyle = defaultTextStyle)
        else JewelTheme.lightThemeDefinition(defaultTextStyle = defaultTextStyle)
    }
    val styling = remember(
        darkTheme,
        colors.primaryAccent,
        colors.primaryAccentPressed
    ) {
        if (darkTheme) 
            ComponentStyling.dark(
                defaultButtonStyle = ButtonStyle.Default.dark(
                    colors = ButtonColors.Default.dark(
                        background = SolidColor(colors.primaryAccent),
                        backgroundFocused = SolidColor(colors.primaryAccent),
                        backgroundPressed = SolidColor(colors.primaryAccentPressed),
                        border = SolidColor(colors.primaryAccent),
                        borderFocused = SolidColor(colors.primaryAccent),
                        borderPressed = SolidColor(colors.primaryAccent),
                        borderHovered = SolidColor(colors.primaryAccent)
                    )
                ),
                defaultTabStyle = TabStyle.Default.dark(
                    colors = TabColors.Default.dark(
                        underlineSelected = colors.primaryAccent
                    )
                ),
                editorTabStyle = TabStyle.Editor.dark(
                    colors = TabColors.Editor.dark(
                        underlineSelected = colors.primaryAccent
                    )
                )
            )
        else 
            ComponentStyling.light(
                defaultButtonStyle = ButtonStyle.Default.light(
                    colors = ButtonColors.Default.light(
                        background = SolidColor(colors.primaryAccent),
                        backgroundFocused = SolidColor(colors.primaryAccent),
                        backgroundPressed = SolidColor(colors.primaryAccentPressed),
                        border = SolidColor(colors.primaryAccent),
                        borderFocused = SolidColor(colors.primaryAccent),
                        borderPressed = SolidColor(colors.primaryAccent),
                        borderHovered = SolidColor(colors.primaryAccent)
                    )
                ),
                defaultTabStyle = TabStyle.Default.light(
                    colors = TabColors.Default.light(
                        underlineSelected = colors.primaryAccent
                    )
                ),
                editorTabStyle = TabStyle.Editor.light(
                    colors = TabColors.Editor.light(
                        underlineSelected = colors.primaryAccent
                    )
                )
            )
    }

    IntUiTheme(
        theme = themeDefinition,
        styling = styling
    ) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalTypography provides typography,
            LocalDefaultButtonStyle provides AdbrowserTheme.defaultButtonStyle(),
            LocalOutlinedButtonStyle provides AdbrowserTheme.outlineButtonStyle()
        ) {
            content()
        }
    }
}