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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Aggregates multiple independently measured regions into one ordered bounds list.
 *
 * Components like icon cards often expose several hit regions to marquee-selection logic. This
 * tracker keeps the region ordering centralized and only re-emits when the effective list changes.
 */
@Stable
class OrderedBoundsTracker<K> internal constructor(private val order: List<K>) {

    private val boundsByKey = mutableMapOf<K, Rect>()

    var aggregatedBounds by mutableStateOf<List<Rect>>(emptyList())
        private set

    fun update(key: K, bounds: Rect) {
        if (boundsByKey[key] == bounds) return

        boundsByKey[key] = bounds
        aggregatedBounds = order.mapNotNull(boundsByKey::get)
    }

    fun update(key: K, coordinates: LayoutCoordinates, bounds: Rect) {
        if (!coordinates.isAttached) {
            clear(key)
            return
        }
        update(key, bounds)
    }

    fun clear(key: K) {
        if (boundsByKey.remove(key) == null) return
        aggregatedBounds = order.mapNotNull(boundsByKey::get)
    }

    fun boundsOf(key: K) = boundsByKey[key]
}

@Composable
fun <K> rememberOrderedBoundsTracker(
    key: Any? = Unit,
    order: List<K>,
    onBoundsChanged: (List<Rect>) -> Unit = {}
): OrderedBoundsTracker<K> {
    val tracker = remember(key, order) { OrderedBoundsTracker(order) }
    val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)

    LaunchedEffect(tracker.aggregatedBounds) {
        currentOnBoundsChanged(tracker.aggregatedBounds)
    }
    return tracker
}