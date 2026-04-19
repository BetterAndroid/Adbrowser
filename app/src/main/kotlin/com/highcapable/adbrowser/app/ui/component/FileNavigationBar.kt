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
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.type.FileSortMode
import com.highcapable.adbrowser.app.ui.vm.model.type.FileViewMode
import org.jetbrains.jewel.ui.component.TextField

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
            onClick = {
                val newMode = when (selectedSortMode) {
                    FileSortMode.Name -> FileSortMode.Size
                    FileSortMode.Size -> FileSortMode.ModifiedTime
                    FileSortMode.ModifiedTime -> FileSortMode.Name
                }
                onSortModeSelected(newMode)
            }
        )
    }
}

@Composable
private fun SortModeLabel(option: FileSortMode) = when (option) {
    FileSortMode.Name -> strings.mainFileListSortModeName
    FileSortMode.Size -> strings.mainFileListSortModeSize
    FileSortMode.ModifiedTime -> strings.mainFileListSortModeModifiedTime
}