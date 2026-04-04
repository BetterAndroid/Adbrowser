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
package com.highcapable.adbrowser.backend.setting

import kotlinx.serialization.Serializable

/**
 * Stores user preferences persisted on local machine.
 */
@Serializable
data class AppSettings(
    var language: String = "",
    var adbExecPath: String = "",
    var rememberLastDevice: Boolean = true,
    var rememberDevicePath: Boolean = true,
    var useSuperuser: Boolean = false,
    var showHiddenFiles: Boolean = true,
    var rememberLastDisplayStyle: Boolean = true,
    var lastFileViewMode: FileViewMode = FileViewMode.List,
    var foldersFirst: Boolean = true,
    var lastDeviceSerial: String = "",
    var deviceHomePaths: MutableMap<String, String> = mutableMapOf(),
    var deviceLastPaths: MutableMap<String, String> = mutableMapOf(),
    var devicePaneWidth: Double = 300.0,
    var fileColumnWidthName: Double = 360.0,
    var fileColumnWidthSize: Double = 140.0,
    var fileColumnWidthModified: Double = 240.0,
    var fileColumnWidthPermission: Double = 150.0
) {

    /**
     * File view mode for file list display.
     */
    @Serializable
    enum class FileViewMode {
        List,
        Grid
    }
}