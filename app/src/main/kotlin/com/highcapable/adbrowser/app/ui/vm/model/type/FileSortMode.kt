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
 * This file is created by fankes on 2026/4/19.
 */
package com.highcapable.adbrowser.app.ui.vm.model.type

import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.core.domain.setting.model.type.FileSortMode as SettingsFileSortMode

/**
 * File sort mode for file list sorting.
 */
enum class FileSortMode {
    Name,
    Size,
    ModifiedTime;

    companion object {

        @Composable
        fun Label(option: FileSortMode) = when (option) {
            Name -> strings.mainFileListSortModeName
            Size -> strings.mainFileListSortModeSize
            ModifiedTime -> strings.mainFileListSortModeModifiedTime
        }

        fun SettingsFileSortMode.toUiType() = when (this) {
            SettingsFileSortMode.Name -> Name
            SettingsFileSortMode.Size -> Size
            SettingsFileSortMode.ModifiedTime -> ModifiedTime
        }
    }

    fun toSettingsType() = when (this) {
        Name -> SettingsFileSortMode.Name
        Size -> SettingsFileSortMode.Size
        ModifiedTime -> SettingsFileSortMode.ModifiedTime
    }
}