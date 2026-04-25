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
 * This file is created by fankes on 2026/4/5.
 */
package com.highcapable.adbrowser.app.ui.vm.model

import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.common.utils.extension.toFriendlyFileSize
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FileEntrySnapshot(
    val device: AndroidDevice,
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val isSymlink: Boolean,
    val sizeBytes: Long,
    val modifiedAt: Instant,
    val symbolicPermission: String
) {

    private companion object {

        val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    /**
     * Dialog snapshots are immutable and short-lived, so lazy formatting keeps the display string
     * cheap while still ensuring repeated recompositions reuse the same computed value.
     */
    val friendlySizeText by lazy { if (isDirectory) "" else sizeBytes.toFriendlyFileSize() }

    /**
     * Dialog snapshots are immutable and short-lived, so lazy formatting keeps the display string
     * cheap while still ensuring repeated recompositions reuse the same computed value.
     */
    val friendlyModifiedAtText by lazy { DateFormatter.format(modifiedAt.atZone(ZoneId.systemDefault())) ?: "" }
}