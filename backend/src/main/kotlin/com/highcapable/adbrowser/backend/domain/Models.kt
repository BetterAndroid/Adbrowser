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
 */
package com.highcapable.adbrowser.backend.domain

/** Device connection state from ADB. */
enum class DeviceConnectionState {
    Online,
    Offline
}

/** Device information exposed to frontend. */
data class AdbDevice(
    val id: String,
    val name: String,
    val model: String,
    val state: DeviceConnectionState
)

/** File system entry type. */
enum class FsEntryType {
    Directory,
    File,
    Symlink,
    Unknown
}

/** Unix-like rwx permission model. */
data class FsPermission(
    val read: Boolean,
    val write: Boolean,
    val execute: Boolean
) {
    companion object {
        val Full = FsPermission(read = true, write = true, execute = true)
        val ReadOnly = FsPermission(read = true, write = false, execute = false)
    }
}

/** File system entry that can be rendered in UI. */
data class FsEntry(
    val path: String,
    val name: String,
    val type: FsEntryType,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
    val permission: FsPermission
)