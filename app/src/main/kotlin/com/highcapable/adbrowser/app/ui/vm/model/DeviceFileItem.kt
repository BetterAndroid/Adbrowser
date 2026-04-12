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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.app.ui.vm.model

import com.highcapable.adbrowser.core.adb.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.core.common.utils.extension.toFriendlyFileSize
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DeviceFileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedAt: Instant,
    val permission: String,
    val isDirectory: Boolean,
    val isSymbolicLink: Boolean = false
) {

    /**
     * File entries are replaced wholesale on refresh, so a per-instance lazy cache avoids repeated
     * formatting work without risking stale values after backend updates.
     */
    val friendlySizeText by lazy { if (isDirectory) "-" else sizeBytes.toFriendlyFileSize() }

    /**
     * File entries are replaced wholesale on refresh, so a per-instance lazy cache avoids repeated
     * formatting work without risking stale values after backend updates.
     */
    val modifiedText: String by lazy {
        DateFormatter.format(modifiedAt.atZone(ZoneId.systemDefault()))
    }

    companion object {

        private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(entry: DeviceFileEntry) = DeviceFileItem(
            name = entry.name,
            path = entry.path,
            sizeBytes = entry.size,
            modifiedAt = entry.modifiedAt,
            permission = entry.permission,
            isDirectory = entry.isDirectory,
            isSymbolicLink = entry.isSymlink
        )
    }
}