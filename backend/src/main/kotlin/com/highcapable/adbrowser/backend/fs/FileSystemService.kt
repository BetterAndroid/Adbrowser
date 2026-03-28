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
package com.highcapable.adbrowser.backend.fs

import com.highcapable.adbrowser.backend.domain.FsEntry
import com.highcapable.adbrowser.backend.domain.FsPermission

/**
 * Android file system access abstraction.
 */
interface FileSystemService {

    /** Lists children under target directory. */
    fun listEntries(deviceId: String, path: String): List<FsEntry>

    /** Retrieves a single path attributes. */
    fun getAttributes(deviceId: String, path: String): FsEntry?

    /** Searches entries by name. */
    fun search(deviceId: String, path: String, keyword: String): List<FsEntry>

    /** Creates directory under parent path. */
    fun createFolder(deviceId: String, parentPath: String, name: String): Boolean

    /** Deletes one entry by full path. */
    fun delete(deviceId: String, path: String): Boolean

    /** Renames one entry by full path. */
    fun rename(deviceId: String, path: String, newName: String): Boolean

    /** Updates rwx permission on a path. */
    fun updatePermission(deviceId: String, path: String, permission: FsPermission): Boolean
}
