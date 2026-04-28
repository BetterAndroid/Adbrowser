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
 * This file is created by fankes on 2026/4/15.
 */
package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import com.highcapable.adbrowser.app.ui.geometry.intersects
import com.highcapable.adbrowser.app.ui.geometry.intersectsWithMinOverlap
import com.highcapable.adbrowser.app.ui.geometry.normalizedRect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared interaction state for list/grid areas that support blank-space hit testing and
 * marquee-style drag selection.
 *
 * The state intentionally stores rendered item bounds instead of domain data. This keeps the
 * selection logic reusable across file lists, icon grids, and log rows where the visual hit
 * regions are different even when the selection semantics are the same.
 */
class SelectionAreaState<T> {

    var selectionRect by mutableStateOf<Rect?>(null)
    private val visibleItemBounds = mutableStateMapOf<T, VisibleItemBounds>()
    var contentCoordinates by mutableStateOf<LayoutCoordinates?>(null)
    var cumulativeScrollY by mutableStateOf(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Pointer positions remain in viewport-local coordinates while content moves. Tracking
            // cumulative scroll lets the active gesture re-run hit testing during auto-scroll even
            // when the pointer itself is stationary.
            cumulativeScrollY -= consumed.y
            return Offset.Zero
        }
    }

    fun dismissSelectionRect() {
        selectionRect = null
    }

    fun updateVisibleItemBounds(item: T, bounds: List<Rect>) {
        visibleItemBounds[item] = VisibleItemBounds(
            regions = bounds,
            scrollY = cumulativeScrollY
        )
    }

    fun removeVisibleItemBounds(item: T) {
        visibleItemBounds.remove(item)
    }

    fun isBlankArea(position: Offset): Boolean {
        val rootPosition = contentCoordinates?.localToRoot(position) ?: position
        return visibleItemBounds.values.none { bounds ->
            bounds.regionsAt(cumulativeScrollY).any { it.contains(rootPosition) }
        }
    }

    /**
     * Returns the currently visible items whose rendered hit regions intersect the drag rectangle.
     *
     * The viewport filter is important because off-screen rows can leave stale bounds around for a
     * short time while lazy layouts are remeasuring during scroll.
     */
    fun itemsIntersecting(localRect: Rect): Set<T> {
        val coords = contentCoordinates ?: return emptySet()
        val rootRect = normalizedRect(
            coords.localToRoot(localRect.topLeft),
            coords.localToRoot(localRect.bottomRight)
        )
        val viewport = viewportRectInRoot() ?: return emptySet()

        return visibleItemBounds
            .filterValues { bounds ->
                val regions = bounds.regionsAt(cumulativeScrollY)
                regions.any { it.intersects(viewport) } &&
                    regions.any { it.intersectsWithMinOverlap(rootRect, MinDragSelectionOverlapPx) }
            }.keys
    }

    fun viewportItems(): Set<T> {
        val viewport = viewportRectInRoot() ?: return emptySet()
        return visibleItemBounds
            .filterValues { bounds ->
                bounds.regionsAt(cumulativeScrollY).any { it.intersects(viewport) }
            }
            .keys
    }

    /** Builds the current viewport rectangle in root coordinates for hit testing. */
    private fun viewportRectInRoot(): Rect? {
        val coords = contentCoordinates ?: return null
        return normalizedRect(
            coords.localToRoot(Offset.Zero),
            coords.localToRoot(Offset(coords.size.width.toFloat(), coords.size.height.toFloat()))
        )
    }
}

private const val MinDragSelectionOverlapPx = 2f

private data class VisibleItemBounds(
    val regions: List<Rect>,
    val scrollY: Float
) {
    fun regionsAt(currentScrollY: Float): List<Rect> {
        val scrollDelta = currentScrollY - scrollY
        if (scrollDelta == 0f) return regions
        return regions.map { region ->
            region.translate(Offset(x = 0f, y = -scrollDelta))
        }
    }
}

fun <T, K> Modifier.blankAreaDragSelection(
    selectionState: SelectionAreaState<T>,
    showSelectionRect: Boolean,
    selectedKeysProvider: () -> Set<K>,
    onClearSelection: () -> Unit,
    onSelectionChanged: (candidateKeys: Set<K>, additive: Boolean, initialSelectionKeys: Set<K>) -> Unit,
    keyOfItem: (T) -> K,
    prepareBlankGesture: () -> Boolean = { true },
    autoScrollBy: (suspend (Float) -> Unit)? = null
) = pointerInput(selectionState, showSelectionRect) {
    // Keep the gesture implementation generic. Callers only provide how items map to selection
    // keys and how selection updates should be applied to their own view model state.
    val inputScope = this

    val drag = object {
        var startX = 0f
        var startY = 0f
        var currentX = 0f
        var currentY = 0f
        var startScrollY = 0f
        var dragging = false
        var active = false
        var additive = false
        var initialSelectionKeys: Set<K> = emptySet()
        var accumulatedKeys = linkedSetOf<K>()
    }

    /**
     * Recomputes the candidate selection set from the current drag rectangle.
     *
     * Instead of only adding newly intersecting items, we rebuild the contribution from the
     * current viewport each time. This prevents rows from remaining selected forever after they
     * scroll out of view during an active drag-selection gesture.
     */
    fun performSelectionUpdate() {
        val scrollDelta = selectionState.cumulativeScrollY - drag.startScrollY
        val adjustedStart = Offset(drag.startX, drag.startY - scrollDelta)
        val current = Offset(drag.currentX, drag.currentY)
        val dragRect = normalizedRect(adjustedStart, current)
        val currentIntersectingKeys = selectionState.itemsIntersecting(dragRect)
            .mapTo(linkedSetOf(), keyOfItem)
        val viewportKeys = selectionState.viewportItems()
            .mapTo(linkedSetOf(), keyOfItem)

        selectionState.selectionRect = if (showSelectionRect) dragRect else null
        drag.accumulatedKeys = ((drag.accumulatedKeys - viewportKeys) + currentIntersectingKeys)
            .toCollection(linkedSetOf())

        onSelectionChanged(drag.accumulatedKeys, drag.additive, drag.initialSelectionKeys)
    }

    coroutineScope {
        launch {
            var lastObservedScrollY = selectionState.cumulativeScrollY
            while (isActive) {
                delay(16)

                if (!drag.active || !drag.dragging) {
                    lastObservedScrollY = selectionState.cumulativeScrollY
                    continue
                }

                if (autoScrollBy != null) {
                    val y = drag.currentY
                    val height = inputScope.size.height.toFloat()
                    val maxScrollSpeed = 18f
                    val maxOvershoot = 150f
                    val scrollAmount = when {
                        y < 0f -> -maxScrollSpeed * (minOf(-y, maxOvershoot) / maxOvershoot)
                        y > height -> maxScrollSpeed * (minOf(y - height, maxOvershoot) / maxOvershoot)
                        else -> 0f
                    }

                    if (scrollAmount != 0f) {
                        autoScrollBy(scrollAmount)
                        // Lazy item bounds are reported in root coordinates and update on the
                        // next layout pass. Hit testing in the same tick can combine a scrolled
                        // marquee rect with stale item bounds, causing transient false selections.
                        withFrameNanos {}
                    }
                }

                // Re-run hit testing when scrolling changes, even if the pointer itself did not
                // move. Without this, auto-scroll would visually move the list while the selected
                // range lags one frame behind.
                val currentScrollY = selectionState.cumulativeScrollY
                if (currentScrollY != lastObservedScrollY) {
                    lastObservedScrollY = currentScrollY
                    performSelectionUpdate()
                }
            }
        }

        inputScope.awaitEachGesture {
            var downPosition: Offset? = null
            drag.additive = false

            while (downPosition == null) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (!event.buttons.isPrimaryPressed) continue

                // We only start drag selection from a fresh down event. Reusing a later move event
                // here would make a regular click occasionally look like a marquee gesture.
                val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
                downPosition = change.position
                drag.additive = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
            }

            val start = downPosition
            if (!selectionState.isBlankArea(start)) {
                selectionState.dismissSelectionRect()
                return@awaitEachGesture
            }

            // Callers can veto the gesture to resolve higher-priority interactions first, such as
            // dismissing a context menu without also clearing the current selection.
            if (!prepareBlankGesture()) {
                selectionState.dismissSelectionRect()
                return@awaitEachGesture
            }

            if (!drag.additive) onClearSelection()

            drag.initialSelectionKeys = selectedKeysProvider()
            drag.startX = start.x
            drag.startY = start.y
            drag.currentX = start.x
            drag.currentY = start.y
            drag.startScrollY = selectionState.cumulativeScrollY
            drag.accumulatedKeys = linkedSetOf()
            drag.dragging = false
            drag.active = true

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val primaryChange = event.changes.firstOrNull() ?: continue
                val current = primaryChange.position

                if (primaryChange.pressed) {
                    val scrollDelta = selectionState.cumulativeScrollY - drag.startScrollY
                    val adjustedStart = Offset(start.x, start.y - scrollDelta)

                    drag.currentX = current.x
                    drag.currentY = current.y

                    val dragRect = normalizedRect(adjustedStart, current)
                    // Ignore tiny pointer jitter so a normal click on blank space does not start
                    // selecting nearby entries by accident.
                    if (!drag.dragging && maxOf(dragRect.width, dragRect.height) >= 4f) drag.dragging = true
                    if (drag.dragging) performSelectionUpdate()
                    continue
                }

                drag.active = false
                selectionState.dismissSelectionRect()
                break
            }
        }
    }
}
