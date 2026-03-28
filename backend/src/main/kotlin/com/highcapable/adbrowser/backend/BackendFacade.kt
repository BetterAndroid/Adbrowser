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
package com.highcapable.adbrowser.backend

import com.highcapable.adbrowser.backend.adb.AdbService
import com.highcapable.adbrowser.backend.adb.InMemoryAdbService
import com.highcapable.adbrowser.backend.fs.FileSystemService
import com.highcapable.adbrowser.backend.fs.InMemoryFileSystemService
import com.highcapable.adbrowser.backend.permission.InMemoryPermissionService
import com.highcapable.adbrowser.backend.permission.PermissionService

/**
 * Unified backend entry point for frontend use.
 */
interface BackendFacade {
    val adb: AdbService
    val fs: FileSystemService
    val permission: PermissionService
}

/**
 * Default in-memory backend used during scaffold stage.
 */
class DefaultBackendFacade(adbPath: String? = null) : BackendFacade {
    override val adb: AdbService = InMemoryAdbService(adbPath)
    override val fs: FileSystemService = InMemoryFileSystemService()
    override val permission: PermissionService = InMemoryPermissionService(fs)
}