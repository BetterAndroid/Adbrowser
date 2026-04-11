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
package com.highcapable.adbrowser.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AdbrowserColorScheme(
    val mainBackground: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val primaryAccent: Color,
    val primaryAccentPressed: Color,
    val hintBackground: Color,
    val hintBorder: Color,
    val subtleControlBackground: Color,
    val pathBreadcrumbForeground: Color,
)

val LightColors = AdbrowserColorScheme(
    mainBackground = Color(0xFFF3F4F6),
    panelBackground = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFD6D9DE),
    primaryAccent = Color(0xFF3574F0),
    primaryAccentPressed = Color(0xFF315FBD),
    hintBackground = Color(0xFFF8FAFC),
    hintBorder = Color(0xFFE3E3E3),
    subtleControlBackground = Color(0xFFF7F8FB),
    pathBreadcrumbForeground = Color(0xFF6B7280)
)

val DarkColors = AdbrowserColorScheme(
    mainBackground = Color(0xFF161A20),
    panelBackground = Color(0xFF242B35),
    panelBorder = Color(0xFF3A4453),
    primaryAccent = Color(0xFF3574F0),
    primaryAccentPressed = Color(0xFF375FAD),
    hintBackground = Color(0xFF1E293B),
    hintBorder = Color(0xFF334155),
    subtleControlBackground = Color(0xFF2A3240),
    pathBreadcrumbForeground = Color(0xFF9CA3AF)
)

val LocalColors = staticCompositionLocalOf<AdbrowserColorScheme> {
    error("No AdbrowserColorScheme provided.")
}