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
 * This file is created by fankes on 2026/4/22.
 */
package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Tracks one parent coordinate space plus one child bounds region that should be ignored.
 *
 * This keeps hit-testing math out of high-level composable. Callers only need to tell the guard
 * which container owns the pointer coordinates and which child bounds should be avoided.
 */
@Stable
class PointerBoundsGuard {

    private var containerCoordinates: LayoutCoordinates? by mutableStateOf(null)
    private var avoidedBoundsInRoot: Rect? by mutableStateOf(null)

    fun updateContainer(coordinates: LayoutCoordinates) {
        containerCoordinates = coordinates
    }

    fun updateAvoidedBounds(bounds: Rect) {
        avoidedBoundsInRoot = bounds
    }

    fun shouldHandle(localPosition: Offset): Boolean {
        val container = containerCoordinates ?: return true
        return shouldHandleRoot(container.localToRoot(localPosition))
    }

    fun shouldHandleRoot(rootPosition: Offset): Boolean {
        val avoidedBounds = avoidedBoundsInRoot ?: return true
        return !avoidedBounds.contains(rootPosition)
    }
}

@Composable
fun rememberPointerBoundsGuard(key: Any? = Unit) = remember(key) { PointerBoundsGuard() }

fun Modifier.trackPointerContainer(guard: PointerBoundsGuard): Modifier =
    onGloballyPositioned(guard::updateContainer)

fun Modifier.trackBoundsInRoot(guard: PointerBoundsGuard): Modifier =
    onGloballyPositioned { coordinates ->
        guard.updateAvoidedBounds(coordinates.boundsInRoot())
    }

/**
 * Exposes a readable "ignore this child bounds" wrapper around pointer handlers.
 */
inline fun Modifier.avoidingBounds(
    guard: PointerBoundsGuard,
    block: Modifier.((Offset) -> Boolean) -> Modifier
): Modifier = block(guard::shouldHandle)