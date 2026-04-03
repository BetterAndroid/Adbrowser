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

import com.highcapable.adbrowser.backend.di.AppServicesComponent
import com.highcapable.adbrowser.backend.di.create

/**
 * Stores singleton-like service instances for application bootstrap.
 *
 * - Note: This instance must be initialized before any service is accessed
 *   and should be shared across the entire application lifecycle.
*/
class AppServices {

    private val component = AppServicesComponent::class.create()

    val logService get() = component.provideLogService()

    val adbClient get() = component.provideAppSettingsService()

    val fileSystemService get() = component.provideFileSystemService()

    val shellCommandExecutor get() = component.provideAdbShellCommandExecutor()

    val permissionService get() = component.providePermissionService()

    val settingsService get() = component.provideAppSettingsService()

    /**
     * Initializes backend service graph.
     */
    suspend fun initialize() {
        settingsService.load()
    }
}