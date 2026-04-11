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
package com.highcapable.adbrowser.app.ui.foundation

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.withFrameNanos

fun LazyListState.isIndexVisible(index: Int): Boolean {
    if (index < 0) return false
    return layoutInfo.visibleItemsInfo.any { it.index == index }
}

fun LazyGridState.isIndexVisible(index: Int): Boolean {
    if (index < 0) return false
    return layoutInfo.visibleItemsInfo.any { it.index == index }
}

fun LazyListState.isIndexFullyVisible(index: Int): Boolean {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val itemStart = item.offset
    val itemEnd = item.offset + item.size

    // We require full visibility here instead of "visible at all".
    // Without this, keyboard navigation can stop with the target item only partially revealed.
    return itemStart >= viewportStart && itemEnd <= viewportEnd
}

fun LazyGridState.isIndexFullyVisible(index: Int): Boolean {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val itemStart = item.offset.y
    val itemEnd = item.offset.y + item.size.height

    // For the grid case we only care about full vertical visibility.
    // Horizontal placement is already handled by LazyVerticalGrid itself.
    return itemStart >= viewportStart && itemEnd <= viewportEnd
}

suspend fun LazyListState.revealIndexBySingleStep(index: Int) {
    // This intentionally uses a bounded retry loop instead of while(true):
    // 1. layoutInfo is only reliable after the next frame, so we may need multiple
    //    "scroll -> await frame -> recompute" passes.
    // 2. In practice 1-2 passes usually settle it; 4 gives enough room for edge items,
    //    spacing, and partially visible rows without becoming an unbounded loop.
    // 3. If layout updates behave unexpectedly, we still have scrollToItem as a final fallback.
    repeat(4) {
        if (isIndexFullyVisible(index)) return

        val visibleItems = layoutInfo.visibleItemsInfo
        val targetItem = visibleItems.firstOrNull { it.index == index }
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset

        val delta = when {
            targetItem != null -> {
                val itemStart = targetItem.offset
                val itemEnd = targetItem.offset + targetItem.size

                // The target item is already inside the viewport, but may still be clipped.
                // In that case we correct by the exact missing pixels instead of another coarse step.
                when {
                    itemStart < viewportStart -> (itemStart - viewportStart).toFloat()
                    itemEnd > viewportEnd -> (itemEnd - viewportEnd).toFloat()
                    else -> 0f
                }
            }
            visibleItems.isEmpty() -> 0f
            index < visibleItems.first().index -> {
                // When the target is above the current viewport, only reveal one item step.
                // This keeps arrow navigation feeling incremental rather than page-like.
                -((visibleItems.first().size + layoutInfo.mainAxisItemSpacing).toFloat())
            }
            else -> {
                // Same idea when the target is below the current viewport.
                (visibleItems.last().size + layoutInfo.mainAxisItemSpacing).toFloat()
            }
        }

        if (delta == 0f) {
            // Reaching this usually means layoutInfo has not caught up yet,
            // or the currently visible data is not enough to keep deriving a safe minimal delta.
            scrollToItem(index)
            return
        }

        scrollBy(delta)
        // scrollBy starts the movement immediately, but layoutInfo only becomes trustworthy
        // after the next frame. Skipping this await can make the next pass work from stale data.
        withFrameNanos { }
    }

    // If bounded correction still could not fully reveal the item, fall back to a hard jump.
    // This covers extreme edge cases such as trailing items or unexpected layout measurements.
    if (!isIndexFullyVisible(index)) scrollToItem(index)
}

suspend fun LazyGridState.revealIndexBySingleStep(index: Int) {
    // The grid version uses the same bounded convergence approach.
    // We still keep the loop at 4 because adaptive columns and uneven card heights
    // can require one extra correction pass, while an infinite loop would be risky here.
    repeat(4) {
        if (isIndexFullyVisible(index)) return

        val visibleItems = layoutInfo.visibleItemsInfo
        val targetItem = visibleItems.firstOrNull { it.index == index }
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset

        val delta = when {
            targetItem != null -> {
                val itemStart = targetItem.offset.y
                val itemEnd = targetItem.offset.y + targetItem.size.height

                // If the target is already in view but still clipped, correct by the exact
                // missing pixels instead of another whole-row jump.
                when {
                    itemStart < viewportStart -> (itemStart - viewportStart).toFloat()
                    itemEnd > viewportEnd -> (itemEnd - viewportEnd).toFloat()
                    else -> 0f
                }
            }
            visibleItems.isEmpty() -> 0f
            index < visibleItems.first().index ->
                // When the target row is above the viewport, reveal exactly one visible row.
                // We must use the tallest item in that row because wrapped labels can make
                // cards in the same row end up with different heights.
                -((visibleItems.visibleGridRowHeight(visibleItems.first().offset.y) + layoutInfo.mainAxisItemSpacing).toFloat())
            else -> (visibleItems.visibleGridRowHeight(visibleItems.last().offset.y) + layoutInfo.mainAxisItemSpacing).toFloat()
        }

        if (delta == 0f) {
            // Same fallback as the list version: if we can no longer derive a reliable delta,
            // jump to the item rather than leaving it partially visible.
            scrollToItem(index)
            return
        }

        scrollBy(delta)

        // Wait for the next layout pass before reading layoutInfo again.
        withFrameNanos {}
    }

    if (!isIndexFullyVisible(index)) scrollToItem(index)
}

private fun List<LazyGridItemInfo>.visibleGridRowHeight(rowOffsetY: Int) = asSequence()
    // Items in the same LazyVerticalGrid row can still have different heights because
    // wrapped file names change the card height. We use the tallest one so a single-row
    // reveal never leaves the row half visible.
    .filter { it.offset.y == rowOffsetY }
    .maxOfOrNull { it.size.height } ?: 0