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
 * This file is created by fankes on 2026/4/8.
 */
package com.highcapable.adbrowser.frontend.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserColorScheme

fun Modifier.edgeBorder(
    color: Color,
    top: Boolean = true,
    bottom: Boolean = true,
    start: Boolean = true,
    end: Boolean = true
): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    val halfStroke = strokeWidth / 2f

    if (top) {
        drawLine(
            color = color,
            start = Offset(halfStroke, halfStroke),
            end = Offset(size.width - halfStroke, halfStroke),
            strokeWidth = strokeWidth
        )
    }
    if (bottom) {
        drawLine(
            color = color,
            start = Offset(halfStroke, size.height - halfStroke),
            end = Offset(size.width - halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
    }
    if (start) {
        drawLine(
            color = color,
            start = Offset(halfStroke, halfStroke),
            end = Offset(halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
    }
    if (end) {
        drawLine(
            color = color,
            start = Offset(size.width - halfStroke, halfStroke),
            end = Offset(size.width - halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
    }
}

fun resolveListItemBackground(
    colors: AdbrowserColorScheme,
    selected: Boolean,
    hovered: Boolean,
    pressed: Boolean
) = when {
    selected && pressed -> colors.primaryAccentPressed
    selected -> colors.primaryAccent
    pressed -> colors.panelBorder
    hovered -> colors.subtleControlBackground
    else -> Color.Transparent
}