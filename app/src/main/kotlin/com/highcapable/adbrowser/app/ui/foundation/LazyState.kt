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

    return itemStart >= viewportStart && itemEnd <= viewportEnd
}

fun LazyGridState.isIndexFullyVisible(index: Int): Boolean {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val itemStart = item.offset.y
    val itemEnd = item.offset.y + item.size.height

    return itemStart >= viewportStart && itemEnd <= viewportEnd
}

suspend fun LazyListState.revealIndexBySingleStep(index: Int) {
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
                when {
                    itemStart < viewportStart -> (itemStart - viewportStart).toFloat()
                    itemEnd > viewportEnd -> (itemEnd - viewportEnd).toFloat()
                    else -> 0f
                }
            }
            visibleItems.isEmpty() -> 0f
            index < visibleItems.first().index -> {
                -((visibleItems.first().size + layoutInfo.mainAxisItemSpacing).toFloat())
            }
            else -> {
                (visibleItems.last().size + layoutInfo.mainAxisItemSpacing).toFloat()
            }
        }

        if (delta == 0f) {
            scrollToItem(index)
            return
        }

        scrollBy(delta)
        withFrameNanos { }
    }

    if (!isIndexFullyVisible(index)) scrollToItem(index)
}

suspend fun LazyGridState.revealIndexBySingleStep(index: Int) {
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

                when {
                    itemStart < viewportStart -> (itemStart - viewportStart).toFloat()
                    itemEnd > viewportEnd -> (itemEnd - viewportEnd).toFloat()
                    else -> 0f
                }
            }
            visibleItems.isEmpty() -> 0f
            index < visibleItems.first().index ->
                -((visibleItems.visibleGridRowHeight(visibleItems.first().offset.y) + layoutInfo.mainAxisItemSpacing).toFloat())
            else -> (visibleItems.visibleGridRowHeight(visibleItems.last().offset.y) + layoutInfo.mainAxisItemSpacing).toFloat()
        }

        if (delta == 0f) {
            scrollToItem(index)
            return
        }

        scrollBy(delta)
        withFrameNanos {}
    }

    if (!isIndexFullyVisible(index)) scrollToItem(index)
}

private fun List<LazyGridItemInfo>.visibleGridRowHeight(rowOffsetY: Int) = asSequence()
    .filter { it.offset.y == rowOffsetY }
    .maxOfOrNull { it.size.height } ?: 0