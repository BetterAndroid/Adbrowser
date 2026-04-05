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
 * This file is created by fankes on 2026/4/6.
 */
package com.highcapable.adbrowser.frontend.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.shared.utils.OsType
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Typography
import java.awt.GraphicsEnvironment

class AdbrowserTypography : Typography {

    @get:Composable
    override val labelTextStyle: TextStyle
        get() = JewelTheme.defaultTextStyle

    @get:Composable
    override val labelTextSize: TextUnit
        get() = labelTextStyle.fontSize.takeUnless { it.isUnspecified } ?: 13.sp

    @get:Composable
    override val h0TextStyle: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + 12, fontWeight = FontWeight.Bold)

    @get:Composable
    override val h1TextStyle: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + 9, fontWeight = FontWeight.Bold)

    @get:Composable
    override val h2TextStyle: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + 5)

    @get:Composable
    override val h3TextStyle: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + 3)

    @get:Composable
    override val h4TextStyle: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + 1, fontWeight = FontWeight.Bold)

    @get:Composable
    override val regular: TextStyle
        get() = labelTextStyle

    @get:Composable
    override val medium: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + (-1))

    @get:Composable
    override val small: TextStyle
        get() = labelTextStyle.copy(fontSize = labelTextSize + (-2))

    @get:Composable
    override val editorTextStyle: TextStyle
        get() = JewelTheme.editorTextStyle

    @get:Composable
    override val consoleTextStyle: TextStyle
        get() = JewelTheme.consoleTextStyle
}

private val availableSystemFontFamilies by lazy {
    runCatching {
        GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .map { it.lowercase() }
            .toSet()
    }.getOrDefault(emptySet())
}

private val PreferredFontFamilies by lazy {
    when {
        OsType.isMacOS -> listOf("PingFang SC", "Hiragino Sans GB", "STHeiti")
        OsType.isWindows -> listOf("Microsoft YaHei UI", "Microsoft YaHei", "DengXian", "SimHei")
        OsType.isLinux -> listOf("Noto Sans CJK SC", "Noto Sans SC", "Source Han Sans SC", "WenQuanYi Micro Hei")
        else -> emptyList()
    }
}

private val SelectedFontFamilyName by lazy {
    PreferredFontFamilies.firstOrNull { it.lowercase() in availableSystemFontFamilies }
}

@OptIn(ExperimentalTextApi::class)
fun createFontFamilyOrNull(): FontFamily? {
    val familyName = SelectedFontFamilyName ?: return null

    return FontFamily(
        SystemFont(identity = familyName, weight = FontWeight.Normal),
        SystemFont(identity = familyName, weight = FontWeight.Medium),
        SystemFont(identity = familyName, weight = FontWeight.SemiBold),
        SystemFont(identity = familyName, weight = FontWeight.Bold)
    )
}

private operator fun TextUnit.plus(delta: Int) = if (isUnspecified) this else (value + delta).sp