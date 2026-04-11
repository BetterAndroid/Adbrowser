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
package com.highcapable.adbrowser.core.adb.fs

import com.highcapable.adbrowser.core.adb.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.model.OperationResult

/**
 * Defines Android filesystem operations.
 */
interface FileSystemService {

    /**
     * Lists file entries under the specified directory.
     */
    suspend fun list(device: AndroidDevice, path: String): OperationResult<List<DeviceFileEntry>>

    /**
     * Searches entries under the specified path.
     */
    suspend fun search(device: AndroidDevice, path: String, keyword: String): OperationResult<List<DeviceFileEntry>>

    /**
     * Creates a folder on device.
     */
    suspend fun createFolder(device: AndroidDevice, parentPath: String, folderName: String): OperationResult<Unit>

    /**
     * Deletes a file or directory.
     */
    suspend fun delete(device: AndroidDevice, path: String): OperationResult<Unit>

    /**
     * Renames a file or directory.
     */
    suspend fun rename(device: AndroidDevice, path: String, newName: String): OperationResult<Unit>

    /**
     * Copies a file or directory to target path.
     */
    suspend fun copy(device: AndroidDevice, sourcePath: String, targetPath: String): OperationResult<Unit>

    /**
     * Moves a file or directory to target path.
     */
    suspend fun move(device: AndroidDevice, sourcePath: String, targetPath: String): OperationResult<Unit>
}