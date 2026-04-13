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
package com.highcapable.adbrowser.core.adb.di

import com.highcapable.adbrowser.core.adb.AdbClient
import com.highcapable.adbrowser.core.adb.AdbClientImpl
import com.highcapable.adbrowser.core.adb.fs.FileSystemService
import com.highcapable.adbrowser.core.adb.fs.FileSystemServiceImpl
import com.highcapable.adbrowser.core.adb.permission.PermissionService
import com.highcapable.adbrowser.core.adb.permission.PermissionServiceImpl
import com.highcapable.adbrowser.core.adb.shell.AdbShellExecutor
import com.highcapable.adbrowser.core.adb.shell.AdbShellExecutorImpl
import com.highcapable.adbrowser.core.logging.di.LoggingComponent
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/**
 * Component for providing adb-level services.
 */
@AdbScope
@Component
abstract class AdbComponent(
    @Component val loggingComponent: LoggingComponent
) {

    abstract fun provideAdbClient(): AdbClient
    abstract fun provideAdbShellExecutor(): AdbShellExecutor
    abstract fun provideFileSystemService(): FileSystemService
    abstract fun providePermissionService(): PermissionService

    @Provides
    fun provideAdbClient(impl: AdbClientImpl): AdbClient = impl

    @Provides
    fun provideAdbShellExecutor(impl: AdbShellExecutorImpl): AdbShellExecutor = impl

    @Provides
    fun provideFileSystemService(impl: FileSystemServiceImpl): FileSystemService = impl

    @Provides
    fun providePermissionService(impl: PermissionServiceImpl): PermissionService = impl
}