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
package com.highcapable.adbrowser.backend

import com.highcapable.adbrowser.backend.adb.AdbClient
import com.highcapable.adbrowser.backend.adb.AdbClientImpl
import com.highcapable.adbrowser.backend.fs.FileSystemService
import com.highcapable.adbrowser.backend.fs.FileSystemServiceImpl
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.logging.LogServiceImpl
import com.highcapable.adbrowser.backend.permission.PermissionService
import com.highcapable.adbrowser.backend.permission.PermissionServiceImpl
import com.highcapable.adbrowser.backend.setting.AppSettingsService
import com.highcapable.adbrowser.backend.setting.AppSettingsServiceImpl
import com.highcapable.adbrowser.backend.shell.AdbShellCommandExecutor
import com.highcapable.adbrowser.backend.shell.AdbShellCommandExecutorImpl

/**
 * Stores singleton-like service instances for application bootstrap.
 *
 * - Note: This instance must be initialized before any service is accessed
 *   and should be shared across the entire application lifecycle.
*/
class AppServices {

    lateinit var logService: LogService
        private set

    lateinit var adbClient: AdbClient
        private set

    lateinit var fileSystemService: FileSystemService
        private set

    lateinit var shellCommandExecutor: AdbShellCommandExecutor
        private set

    lateinit var permissionService: PermissionService
        private set

    lateinit var settingsService: AppSettingsService
        private set

    /**
     * Initializes backend service graph.
     */
    suspend fun initialize() {
        logService = LogServiceImpl()
        settingsService = AppSettingsServiceImpl(logService)
        settingsService.load()

        adbClient = AdbClientImpl(logService, settingsService)
        shellCommandExecutor = AdbShellCommandExecutorImpl(adbClient, settingsService, logService)
        fileSystemService = FileSystemServiceImpl(shellCommandExecutor, logService)
        permissionService = PermissionServiceImpl(shellCommandExecutor, logService)
    }
}