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
 * This file is created by fankes on 2026/4/2.
 */
package com.highcapable.adbrowser.core.domain.setting

import com.highcapable.adbrowser.core.domain.setting.model.MenuShortcut
import com.highcapable.adbrowser.core.domain.setting.model.type.FileSortMode
import com.highcapable.adbrowser.core.domain.setting.model.type.FileViewMode
import kotlinx.serialization.Serializable

/**
 * Stores user preferences persisted on local machine.
 */
@Serializable
data class AppSettings(
    var language: String = "",
    var adbExecPath: String = "",
    var menuShortcuts: MenuShortcut.Collection = MenuShortcut.Collection(),
    var connectedDeviceAddressHistory: MutableList<String> = mutableListOf(),
    var rememberLastDevice: Boolean = true,
    var rememberLastDevicePath: Boolean = true,
    var useSuperuser: Boolean = false,
    var showHiddenFiles: Boolean = true,
    var rememberLastFileViewMode: Boolean = true,
    var rememberLastFileSortMode: Boolean = true,
    var lastFileViewMode: FileViewMode = FileViewMode.List,
    var lastFileSortMode: FileSortMode = FileSortMode.Name,
    var foldersFirst: Boolean = true,
    var lastDeviceSerial: String = "",
    var deviceHomePaths: MutableMap<String, String> = mutableMapOf(),
    var deviceLastPaths: MutableMap<String, String> = mutableMapOf(),
    var devicePaneWidth: Double = DEFAULT_DEVICE_PANE_WIDTH,
    var fileColumnWidthName: Double = DEFAULT_FILE_COLUMN_WIDTH_NAME,
    var fileColumnWidthSize: Double = DEFAULT_FILE_COLUMN_WIDTH_SIZE,
    var fileColumnWidthModified: Double = DEFAULT_FILE_COLUMN_WIDTH_MODIFIED,
    var fileColumnWidthPermission: Double = DEFAULT_FILE_COLUMN_WIDTH_PERMISSION,
    var mainWindowWidth: Double = DEFAULT_MAIN_WINDOW_WIDTH,
    var mainWindowHeight: Double = DEFAULT_MAIN_WINDOW_HEIGHT,
    var mainWindowPosX: Double? = null,
    var mainWindowPosY: Double? = null
) {

    companion object {

        const val DEFAULT_MAIN_WINDOW_WIDTH = 1220.0
        const val DEFAULT_MAIN_WINDOW_HEIGHT = 820.0

        const val DEFAULT_DEVICE_PANE_WIDTH = 300.0
        const val DEFAULT_FILE_COLUMN_WIDTH_NAME = 260.0
        const val DEFAULT_FILE_COLUMN_WIDTH_SIZE = 140.0
        const val DEFAULT_FILE_COLUMN_WIDTH_MODIFIED = 240.0
        const val DEFAULT_FILE_COLUMN_WIDTH_PERMISSION = 150.0
    }
}