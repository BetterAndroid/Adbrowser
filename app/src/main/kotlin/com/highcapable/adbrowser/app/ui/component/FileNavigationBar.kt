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
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.type.FileSortMode
import com.highcapable.adbrowser.app.ui.vm.model.type.FileViewMode
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import kotlin.math.roundToInt

@Composable
fun FileNavigationBar(
    pathInput: TextFieldState,
    canNavigateBack: Boolean,
    canNavigateForward: Boolean,
    canNavigateUp: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenPathInput: () -> Unit,
    selectedViewMode: FileViewMode,
    onViewModeSelected: (FileViewMode) -> Unit,
    selectedSortMode: FileSortMode,
    onSortModeSelected: (FileSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sortMenuVerticalOffsetPx = with(density) { 4.dp.roundToPx() }
    var sortMenuAnchor by remember { mutableStateOf<SortMenuAnchor?>(null) }
    var sortButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContentIconButton(
            key = AppIcons.ArrowLeft,
            contentDescription = "Navigate Back",
            enabled = canNavigateBack,
            outlined = true,
            onClick = onNavigateBack
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowRight,
            contentDescription = "Navigate Forward",
            enabled = canNavigateForward,
            outlined = true,
            onClick = onNavigateForward
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowUp,
            contentDescription = "Navigate Up",
            enabled = canNavigateUp,
            outlined = true,
            onClick = onNavigateUp
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.Home,
            contentDescription = "Navigate Home",
            outlined = true,
            onClick = onNavigateHome
        )
        Spacer(Modifier.width(6.dp))
        TextField(
            state = pathInput,
            modifier = Modifier
                .weight(1f)
                .height(AdbrowserTheme.DefaultTextFieldHeight)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        onOpenPathInput()
                        true
                    } else false
                }
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = when (selectedViewMode) {
                FileViewMode.List -> AppIcons.FileViewGrid
                FileViewMode.Grid -> AppIcons.FileViewList
            },
            contentDescription = when (selectedViewMode) {
                FileViewMode.List -> "Switch to Grid View"
                FileViewMode.Grid -> "Switch to List View"
            },
            outlined = true,
            onClick = {
                val newMode = when (selectedViewMode) {
                    FileViewMode.List -> FileViewMode.Grid
                    FileViewMode.Grid -> FileViewMode.List
                }
                onViewModeSelected(newMode)
            }
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.FileSort,
            contentDescription = "Change Sort Mode",
            outlined = true,
            modifier = Modifier.onGloballyPositioned { sortButtonCoordinates = it },
            onClick = {
                sortMenuAnchor = sortButtonCoordinates?.boundsInRoot()?.let {
                    SortMenuAnchor(
                        right = it.right.roundToInt(),
                        bottom = it.bottom.roundToInt()
                    )
                }
            }
        )
    }

    sortMenuAnchor?.let { anchor ->
        PopupMenu(
            onDismissRequest = { sortMenuAnchor = null; true },
            popupPositionProvider = remember(anchor, sortMenuVerticalOffsetPx) {
                SortMenuPositionProvider(
                    anchor = anchor,
                    verticalOffsetPx = sortMenuVerticalOffsetPx
                )
            },
            popupProperties = PopupProperties(focusable = false)
        ) {
            FileSortMode.entries.forEach { option ->
                val selected = option == selectedSortMode
                selectableItem(
                    iconKey = if (selected) AllIconsKeys.Actions.Checked else null,
                    selected = selected,
                    onClick = {
                        sortMenuAnchor = null
                        onSortModeSelected(option)
                    }
                ) { Text(FileSortMode.Label(option)) }
            }
        }
    }
}

private data class SortMenuAnchor(
    val right: Int,
    val bottom: Int
)

private class SortMenuPositionProvider(
    private val anchor: SortMenuAnchor,
    private val verticalOffsetPx: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)

        return IntOffset(
            x = (anchor.right - popupContentSize.width).coerceIn(0, maxX),
            y = (anchor.bottom + verticalOffsetPx).coerceIn(0, maxY)
        )
    }
}