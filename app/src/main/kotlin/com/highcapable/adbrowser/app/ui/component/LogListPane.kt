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
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.vm.model.LogEntryItem

@Composable
fun LogListPane(
    entries: List<LogEntryItem>,
    selectedEntryIds: Set<String>,
    listState: LazyListState,
    horizontalScrollState: ScrollState,
    timeWidth: Dp,
    levelWidth: Dp,
    categoryWidth: Dp,
    messageWidth: Dp,
    timeLabel: String,
    levelLabel: String,
    categoryLabel: String,
    messageLabel: String,
    onResizeTimeAndLevel: (Float) -> Float,
    onResizeLevelAndCategory: (Float) -> Float,
    onResizeCategoryAndMessage: (Float) -> Float,
    onResizeFinished: () -> Unit,
    onEntryPrimaryClick: (entry: LogEntryItem, appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onEntrySecondaryClick: (entry: LogEntryItem, position: Offset) -> Unit,
    onEntryBoundsChanged: (entry: LogEntryItem, bounds: List<Rect>?) -> Unit,
    modifier: Modifier = Modifier,
    contentAreaModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.(entry: LogEntryItem) -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val messageColumnWidth = maxOf(
            messageWidth,
            maxWidth - timeWidth - levelWidth - categoryWidth - 30.dp
        )
        val contentWidth = timeWidth + levelWidth + categoryWidth + messageColumnWidth + 30.dp

        Column(modifier = Modifier.fillMaxSize()) {
            LogListHeader(
                horizontalScrollState = horizontalScrollState,
                timeWidth = timeWidth,
                levelWidth = levelWidth,
                categoryWidth = categoryWidth,
                messageWidth = messageColumnWidth,
                timeLabel = timeLabel,
                levelLabel = levelLabel,
                categoryLabel = categoryLabel,
                messageLabel = messageLabel,
                onResizeTimeAndLevel = onResizeTimeAndLevel,
                onResizeLevelAndCategory = onResizeLevelAndCategory,
                onResizeCategoryAndMessage = onResizeCategoryAndMessage,
                onResizeFinished = onResizeFinished
            )
            Box(
                modifier = contentAreaModifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.width(contentWidth),
                        contentPadding = PaddingValues(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = entries,
                            key = { it.id }
                        ) { entry ->
                            DisposableEffect(entry) {
                                onDispose { onEntryBoundsChanged(entry, null) }
                            }
                            LogEntryRow(
                                entry = entry,
                                selected = entry.id in selectedEntryIds,
                                timeWidth = timeWidth,
                                levelWidth = levelWidth,
                                categoryWidth = categoryWidth,
                                messageWidth = messageColumnWidth,
                                onPrimaryClick = { appendSelection, rangeSelection ->
                                    onEntryPrimaryClick(entry, appendSelection, rangeSelection)
                                },
                                onSecondaryClick = { position ->
                                    onEntrySecondaryClick(entry, position)
                                },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    onEntryBoundsChanged(entry, listOf(coordinates.boundsInRoot()))
                                },
                                overlay = { overlay(entry) }
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = 2.dp, bottom = 2.dp, end = 2.dp)
                )
            }

            if (horizontalScrollState.maxValue > 0)
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(horizontalScrollState),
                    modifier = Modifier.fillMaxWidth()
                )
        }
    }
}