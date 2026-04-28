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
 * This file is created by fankes on 2026/4/9.
 */
package com.highcapable.adbrowser.app.ui.geometry

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs

@Stable
fun Rect.intersects(other: Rect) =
    left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top

@Stable
fun Rect.intersectsWithMinOverlap(other: Rect, minOverlap: Float): Boolean {
    val overlapWidth = minOf(right, other.right) - maxOf(left, other.left)
    val overlapHeight = minOf(bottom, other.bottom) - maxOf(top, other.top)

    return overlapWidth >= minOverlap && overlapHeight >= minOverlap
}

@Stable
fun normalizedRect(start: Offset, end: Offset) = Rect(
    offset = Offset(
        x = minOf(start.x, end.x),
        y = minOf(start.y, end.y)
    ),
    size = Size(
        width = abs(end.x - start.x),
        height = abs(end.y - start.y)
    )
)