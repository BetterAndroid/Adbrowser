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
 * This file is created by fankes on 2026/4/12.
 */
package com.highcapable.adbrowser.app.ui.input

import androidx.compose.runtime.Stable
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

/**
 * Get the current bounds of a Component as a WindowBounds object.
 */
fun Component.currentBounds() = WindowBounds(
    x = location.x,
    y = location.y,
    width = size.width,
    height = size.height
)

/**
 * Creates a ComponentAdapter with the provided lambda functions for each event type.
 */
fun ComponentAdapter(
    componentResized: (ComponentEvent) -> Unit = {},
    componentMoved: (ComponentEvent) -> Unit = {},
    componentShown: (ComponentEvent) -> Unit = {},
    componentHidden: (ComponentEvent) -> Unit = {}
) = object : ComponentAdapter() {

    override fun componentResized(e: ComponentEvent) {
        componentResized(e)
    }

    override fun componentMoved(e: ComponentEvent) {
        componentMoved(e)
    }

    override fun componentShown(e: ComponentEvent) {
        componentShown(e)
    }

    override fun componentHidden(e: ComponentEvent) {
        componentHidden(e)
    }
}

/**
 * Represents the bounds of a window, including its position and size.
 */
@Stable
data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)