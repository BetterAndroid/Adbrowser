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
package com.highcapable.adbrowser.backend.di

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
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/**
 * Internal component for providing application services.
 * This component is used for dependency injection.
 */
@Component
internal abstract class AppServicesComponent {

    abstract fun provideLogService(): LogService
    abstract fun provideAppSettingsService(): AppSettingsService
    abstract fun provideAdbClient(): AdbClient
    abstract fun provideAdbShellCommandExecutor(): AdbShellCommandExecutor
    abstract fun provideFileSystemService(): FileSystemService
    abstract fun providePermissionService(): PermissionService

    @Provides
    fun provideLogService(impl: LogServiceImpl): LogService = impl

    @Provides
    fun provideAppSettingsService(impl: AppSettingsServiceImpl): AppSettingsService = impl

    @Provides
    fun provideAdbClient(impl: AdbClientImpl): AdbClient = impl

    @Provides
    fun provideAdbShellCommandExecutor(impl: AdbShellCommandExecutorImpl): AdbShellCommandExecutor = impl

    @Provides
    fun provideFileSystemService(impl: FileSystemServiceImpl): FileSystemService = impl

    @Provides
    fun providePermissionService(impl: PermissionServiceImpl): PermissionService = impl
}